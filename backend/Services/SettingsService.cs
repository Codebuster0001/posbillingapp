using System.Data;
using posbillingapp.api.Data;
using posbillingapp.api.Models;

namespace posbillingapp.api.Services
{
    public interface ISettingsService
    {
        Task EnsureTablesCreated();
        Task<DataTable?> GetCompanyProfile(long companyId);
        Task<bool> UpdateCompanyProfile(long companyId, CompanyProfileUpdate model);
        Task<bool> UpdateLogo(long companyId, string logoUrl);
        Task<DataTable?> GetBankDetails(long companyId);
        Task<bool> AddBankDetails(long companyId, BankDetailsModel model);
        Task<bool> SetPrimaryBank(long companyId, long accountId);
        Task<bool> DeleteBankDetails(long accountId);
        Task<bool> UpdateBankStatus(long accountId, string status);
        Task<DataTable?> GetCardDetails(long companyId);
        Task<bool> AddCardDetails(long companyId, CardDetailsModel model);
        Task<bool> SetPrimaryCard(long companyId, long cardId);
        Task<bool> DeleteCardDetails(long cardId);
        Task<List<PaymentModeModel>> GetPaymentModes();

    }

    public class SettingsService : ISettingsService
    {
        private readonly DbHelper _dbHelper;
        private readonly ILogger<SettingsService> _logger;

        public SettingsService(DbHelper dbHelper, ILogger<SettingsService> logger)
        {
            _dbHelper = dbHelper;
            _logger = logger;
        }

        public async Task EnsureTablesCreated()
        {
            try
            {
                // 1. Company Bank Details Table
                const string sqlBank = @"
                    CREATE TABLE IF NOT EXISTS company_bank_details (
                        Id BIGINT NOT NULL AUTO_INCREMENT,
                        CompanyId BIGINT NOT NULL,
                        AccountHolderName VARCHAR(255),
                        AccountNumber VARCHAR(50),
                        IFSCCode VARCHAR(20),
                        BankName VARCHAR(255),
                        IsPrimary TINYINT(1) DEFAULT 0,
                        Status VARCHAR(20) DEFAULT 'Pending',
                        CreatedAt DATETIME DEFAULT CURRENT_TIMESTAMP,
                        UpdatedAt DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                        PRIMARY KEY (Id),
                        KEY fk_bank_details_company (CompanyId),
                        CONSTRAINT fk_bank_details_company FOREIGN KEY (CompanyId) REFERENCES companies (Id) ON DELETE CASCADE
                    ) ENGINE=InnoDB;";
                await _dbHelper.ExecuteQueryWithParams(sqlBank, new string[0]);

                // Ensure columns exist (for migration)
                try {
                    await _dbHelper.ExecuteQueryWithParams("ALTER TABLE company_bank_details ADD COLUMN IsPrimary TINYINT(1) NOT NULL DEFAULT 1 AFTER BankName;", new string [0]);
                } catch { 
                    try {
                        await _dbHelper.ExecuteQueryWithParams("ALTER TABLE company_bank_details MODIFY COLUMN IsPrimary TINYINT(1) NOT NULL DEFAULT 1;", new string[0]);
                    } catch { }
                }

                try {
                    await _dbHelper.ExecuteQueryWithParams("ALTER TABLE company_bank_details ADD COLUMN RegisteredName VARCHAR(255) AFTER Status;", new string[0]);
                } catch { }

                // 2. Payment Modes Table
                const string sqlPayModes = @"
                    CREATE TABLE IF NOT EXISTS paymentmodes (
                        Id INT NOT NULL AUTO_INCREMENT,
                        Name VARCHAR(50) NOT NULL,
                        Type ENUM('Cash', 'UPI', 'Card', 'Credit') NOT NULL,
                        Provider VARCHAR(50),
                        IsActive TINYINT(1) DEFAULT '1',
                        PRIMARY KEY (Id)
                    ) ENGINE=InnoDB;";
                await _dbHelper.ExecuteQueryWithParams(sqlPayModes, new string[0]);

                // Seed Payment Modes if empty
                var checkModes = await _dbHelper.GetDataTable("SELECT COUNT(*) FROM paymentmodes");
                if (checkModes != null && Convert.ToInt32(checkModes.Rows[0][0]) == 0)
                {
                    const string seedSql = @"
                        INSERT INTO paymentmodes (Name, Type, Provider) VALUES 
                        ('Cash', 'Cash', NULL),
                        ('UPI - Google Pay', 'UPI', 'GPay'),
                        ('UPI - PhonePe', 'UPI', 'PhonePe'),
                        ('UPI - Paytm', 'UPI', 'Paytm'),
                        ('Card - Debit', 'Card', NULL),
                        ('Card - Credit', 'Card', NULL);";
                    await _dbHelper.ExecuteQueryWithParams(seedSql, new string[0]);
                }

                // 3. Company Cards Table
                const string sqlCards = @"
                    CREATE TABLE IF NOT EXISTS company_cards (
                        Id BIGINT NOT NULL AUTO_INCREMENT,
                        CompanyId BIGINT NOT NULL,
                        CardHolderName VARCHAR(255),
                        CardNumber VARCHAR(30),
                        CardType VARCHAR(20),
                        ExpiryMonth INT,
                        ExpiryYear INT,
                        Status VARCHAR(20) DEFAULT 'Verified',
                        IsPrimary TINYINT(1) DEFAULT 0,
                        CreatedAt DATETIME DEFAULT CURRENT_TIMESTAMP,
                        PRIMARY KEY (Id),
                        KEY fk_cards_company (CompanyId),
                        CONSTRAINT fk_cards_company FOREIGN KEY (CompanyId) REFERENCES companies (Id) ON DELETE CASCADE
                    ) ENGINE=InnoDB;";
                await _dbHelper.ExecuteQueryWithParams(sqlCards, new string[0]);

                // Ensure missing columns in bank details
                try { await _dbHelper.ExecuteQueryWithParams("ALTER TABLE company_bank_details ADD COLUMN MatchScore INT DEFAULT 0 AFTER RegisteredName;", new string[0]); } catch { }
                try { await _dbHelper.ExecuteQueryWithParams("ALTER TABLE company_bank_details ADD COLUMN Branch VARCHAR(255) AFTER MatchScore;", new string[0]); } catch { }
                try { await _dbHelper.ExecuteQueryWithParams("ALTER TABLE company_bank_details ADD COLUMN BankType VARCHAR(50) AFTER Branch;", new string[0]); } catch { }

                // 4. Company Payment Settings (for Gateway Keys)
                const string sqlPaySettings = @"
                    CREATE TABLE IF NOT EXISTS company_payment_settings (
                        Id BIGINT NOT NULL AUTO_INCREMENT,
                        CompanyId BIGINT NOT NULL,
                        UPI_ID VARCHAR(100),
                        Gateway_Name VARCHAR(50),
                        API_Key VARCHAR(255),
                        WebHook_Secret VARCHAR(255),
                        CreatedAt DATETIME DEFAULT CURRENT_TIMESTAMP,
                        UpdatedAt DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                        PRIMARY KEY (Id),
                        KEY fk_pay_settings_company (CompanyId),
                        CONSTRAINT fk_pay_settings_company FOREIGN KEY (CompanyId) REFERENCES companies (Id) ON DELETE CASCADE
                    ) ENGINE=InnoDB;";
                await _dbHelper.ExecuteQueryWithParams(sqlPaySettings, new string[0]);



                _logger.LogInformation("Settings and Payment tables checked/created successfully.");
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Error creating settings tables");
                throw;
            }
        }

        public async Task<DataTable?> GetCompanyProfile(long companyId)
        {
            const string sql = @"
                SELECT c.CompanyName, c.Email, c.PhoneNumber, c.LogoUrl, 
                       c.CountryId, cn.CountryName,
                       c.StateId, st.StateName,
                       c.CityId, ct.CityName
                FROM companies c
                LEFT JOIN countries cn ON c.CountryId = cn.Id
                LEFT JOIN states st ON c.StateId = st.Id
                LEFT JOIN cities ct ON c.CityId = ct.Id
                WHERE c.Id = @1";
            return await _dbHelper.GetDataTableWithParams(sql, new[] { companyId.ToString() });
        }

        public async Task<bool> UpdateCompanyProfile(long companyId, CompanyProfileUpdate model)
        {
            const string sql = @"
                UPDATE companies 
                SET CompanyName = @1, 
                    PhoneNumber = @2, 
                    CountryId = @3, 
                    StateId = @4, 
                    CityId = @5
                WHERE Id = @6";

            var rows = await _dbHelper.ExecuteQueryWithParams(sql, new[] {
                model.CompanyName ?? "",
                model.PhoneNumber ?? "",
                model.CountryId?.ToString() ?? "1",
                model.StateId?.ToString(),
                model.CityId?.ToString(),
                companyId.ToString()
            });
            return rows > 0;
        }

        public async Task<bool> UpdateLogo(long companyId, string logoUrl)
        {
            const string sql = "UPDATE companies SET LogoUrl = @1 WHERE Id = @2";
            var rows = await _dbHelper.ExecuteQueryWithParams(sql, new[] { logoUrl, companyId.ToString() });
            return rows > 0;
        }

        public async Task<DataTable?> GetBankDetails(long companyId)
        {
            const string sql = "SELECT * FROM company_bank_details WHERE CompanyId = @1 ORDER BY IsPrimary DESC, CreatedAt DESC";
            return await _dbHelper.GetDataTableWithParams(sql, new[] { companyId.ToString() });
        }

        public async Task<bool> AddBankDetails(long companyId, BankDetailsModel model)
        {
            // If first account, make it primary
            var checkSql = "SELECT COUNT(*) FROM company_bank_details WHERE CompanyId = @1";
            var dt = await _dbHelper.GetDataTableWithParams(checkSql, new[] { companyId.ToString() });
            bool isFirst = dt != null && Convert.ToInt32(dt.Rows[0][0]) == 0;

            const string insertSql = @"
                INSERT INTO company_bank_details (CompanyId, AccountHolderName, AccountNumber, IFSCCode, BankName, IsPrimary, Status, RegisteredName, MatchScore, Branch, BankType) 
                VALUES (@1, @2, @3, @4, @5, @6, @7, @8, @9, @10, @11)";
            
            var rows = await _dbHelper.ExecuteQueryWithParams(insertSql, new[] { 
                companyId.ToString(), 
                model.AccountHolderName!, 
                model.AccountNumber!, // Should be encrypted before calling this
                model.IFSCCode!, 
                model.BankName!,
                (isFirst || model.IsPrimary) ? "1" : "0",
                model.Status,
                model.RegisteredName,
                model.MatchScore.ToString(),
                model.Branch ?? "",
                model.BankType ?? ""
            });

            if (rows > 0 && (isFirst || model.IsPrimary))
            {
                // If we set this as primary, unset others (though it's the first if isFirst is true)
                // We'll call SetPrimary to be sure if it wasn't the very first but user wants it primary
                if (!isFirst && model.IsPrimary)
                {
                    // Need to get the last ID or use SetPrimaryBank
                    // For simplicity, we can just call SetPrimaryBank after finding the ID, 
                    // but since we want atomicity or close to it:
                    const string getLastId = "SELECT LAST_INSERT_ID()";
                    var lastIdDt = await _dbHelper.GetDataTable(getLastId);
                    if (lastIdDt != null && lastIdDt.Rows.Count > 0)
                    {
                        await SetPrimaryBank(companyId, Convert.ToInt64(lastIdDt.Rows[0][0]));
                    }
                }
                return true;
            }
            return rows > 0;
        }

        public async Task<bool> SetPrimaryBank(long companyId, long accountId)
        {
            // 1. Unset all as primary for this company
            const string unsetSql = "UPDATE company_bank_details SET IsPrimary = 0 WHERE CompanyId = @1";
            await _dbHelper.ExecuteQueryWithParams(unsetSql, new[] { companyId.ToString() });

            // 2. Set this one as primary
            const string setSql = "UPDATE company_bank_details SET IsPrimary = 1 WHERE Id = @1 AND CompanyId = @2";
            var rows = await _dbHelper.ExecuteQueryWithParams(setSql, new[] { accountId.ToString(), companyId.ToString() });
            
            return rows > 0;
        }

        public async Task<bool> DeleteBankDetails(long accountId)
        {
            // Check if it's primary before deleting
            const string checkSql = "SELECT IsPrimary, CompanyId FROM company_bank_details WHERE Id = @1";
            var dt = await _dbHelper.GetDataTableWithParams(checkSql, new[] { accountId.ToString() });
            
            if (dt == null || dt.Rows.Count == 0) return false;
            
            bool wasPrimary = Convert.ToBoolean(dt.Rows[0]["IsPrimary"]);
            long companyId = Convert.ToInt64(dt.Rows[0]["CompanyId"]);

            const string deleteSql = "DELETE FROM company_bank_details WHERE Id = @1";
            var rows = await _dbHelper.ExecuteQueryWithParams(deleteSql, new[] { accountId.ToString() });

            if (rows > 0 && wasPrimary)
            {
                // Pick another account as primary if exists
                const string pickNewSql = "UPDATE company_bank_details SET IsPrimary = 1 WHERE CompanyId = @1 LIMIT 1";
                await _dbHelper.ExecuteQueryWithParams(pickNewSql, new[] { companyId.ToString() });
            }

            return rows > 0;
        }

        public async Task<bool> UpdateBankStatus(long accountId, string status)
        {
            const string sql = "UPDATE company_bank_details SET Status = @1 WHERE Id = @2";
            var rows = await _dbHelper.ExecuteQueryWithParams(sql, new[] { status, accountId.ToString() });
            return rows > 0;
        }

        public async Task<DataTable?> GetCardDetails(long companyId)
        {
            const string sql = "SELECT * FROM company_cards WHERE CompanyId = @1 ORDER BY IsPrimary DESC, CreatedAt DESC";
            return await _dbHelper.GetDataTableWithParams(sql, new[] { companyId.ToString() });
        }

        public async Task<bool> AddCardDetails(long companyId, CardDetailsModel model)
        {
            var checkSql = "SELECT COUNT(*) FROM company_cards WHERE CompanyId = @1";
            var dt = await _dbHelper.GetDataTableWithParams(checkSql, new[] { companyId.ToString() });
            bool isFirst = dt != null && Convert.ToInt32(dt.Rows[0][0]) == 0;

            const string insertSql = @"
                INSERT INTO company_cards (CompanyId, CardHolderName, CardNumber, CardType, ExpiryMonth, ExpiryYear, Status, IsPrimary) 
                VALUES (@1, @2, @3, @4, @5, @6, @7, @8)";
            
            var rows = await _dbHelper.ExecuteQueryWithParams(insertSql, new[] { 
                companyId.ToString(), 
                model.CardHolderName!, 
                model.CardNumber!, // Should be masked
                model.CardType!,
                model.ExpiryMonth.ToString(),
                model.ExpiryYear.ToString(),
                model.Status,
                (isFirst || model.IsPrimary) ? "1" : "0"
            });

            if (rows > 0 && (isFirst || model.IsPrimary))
            {
                const string getLastId = "SELECT LAST_INSERT_ID()";
                var lastIdDt = await _dbHelper.GetDataTable(getLastId);
                if (lastIdDt != null && lastIdDt.Rows.Count > 0)
                {
                    await SetPrimaryCard(companyId, Convert.ToInt64(lastIdDt.Rows[0][0]));
                }
                return true;
            }
            return rows > 0;
        }

        public async Task<bool> SetPrimaryCard(long companyId, long cardId)
        {
            const string unsetSql = "UPDATE company_cards SET IsPrimary = 0 WHERE CompanyId = @1";
            await _dbHelper.ExecuteQueryWithParams(unsetSql, new[] { companyId.ToString() });

            const string setSql = "UPDATE company_cards SET IsPrimary = 1 WHERE Id = @1 AND CompanyId = @2";
            var rows = await _dbHelper.ExecuteQueryWithParams(setSql, new[] { cardId.ToString(), companyId.ToString() });
            return rows > 0;
        }

        public async Task<bool> DeleteCardDetails(long cardId)
        {
            const string checkSql = "SELECT IsPrimary, CompanyId FROM company_cards WHERE Id = @1";
            var dt = await _dbHelper.GetDataTableWithParams(checkSql, new[] { cardId.ToString() });
            if (dt == null || dt.Rows.Count == 0) return false;
            
            bool wasPrimary = Convert.ToBoolean(dt.Rows[0]["IsPrimary"]);
            long companyId = Convert.ToInt64(dt.Rows[0]["CompanyId"]);

            const string deleteSql = "DELETE FROM company_cards WHERE Id = @1";
            var rows = await _dbHelper.ExecuteQueryWithParams(deleteSql, new[] { cardId.ToString() });

            if (rows > 0 && wasPrimary)
            {
                const string pickNewSql = "UPDATE company_cards SET IsPrimary = 1 WHERE CompanyId = @1 LIMIT 1";
                await _dbHelper.ExecuteQueryWithParams(pickNewSql, new[] { companyId.ToString() });
            }
            return rows > 0;
        }

        public async Task<List<PaymentModeModel>> GetPaymentModes()
        {
            var dt = await _dbHelper.GetDataTable("SELECT * FROM paymentmodes WHERE IsActive = 1");
            var list = new List<PaymentModeModel>();
            if (dt != null)
            {
                foreach (DataRow row in dt.Rows)
                {
                    list.Add(new PaymentModeModel {
                        Id = Convert.ToInt32(row["Id"]),
                        Name = row["Name"].ToString()!,
                        Type = row["Type"].ToString()!,
                        Provider = row["Provider"]?.ToString(),
                        IsActive = true
                    });
                }
            }
            return list;
        }

    }
}
