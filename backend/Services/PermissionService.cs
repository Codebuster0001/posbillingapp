using System.Data;
using posbillingapp.api.Data;

namespace posbillingapp.api.Services
{
    public interface IPermissionService
    {
        Task EnsureTablesCreated();
        Task SeedPermissions();
        Task<List<string>> GetUserPermissions(string roleId, string companyId);
        Task<bool> UpdateRolePermissions(string roleId, string companyId, List<string> permissionKeys);
        Task<DataTable?> GetAllPermissions();
        Task<DataTable?> GetRolePermissions(string roleId, string companyId);
    }

    public class PermissionService : IPermissionService
    {
        private readonly DbHelper _dbHelper;
        private readonly ILogger<PermissionService> _logger;

        public PermissionService(DbHelper dbHelper, ILogger<PermissionService> logger)
        {
            _dbHelper = dbHelper;
            _logger = logger;
        }

        public async Task EnsureTablesCreated()
        {
            try {
                const string sqlPermissions = @"
                    CREATE TABLE IF NOT EXISTS permissions (
                        Id INT NOT NULL AUTO_INCREMENT,
                        PermissionKey VARCHAR(100) NOT NULL,
                        Description VARCHAR(255),
                        PRIMARY KEY (Id),
                        UNIQUE KEY (PermissionKey)
                    ) ENGINE=InnoDB;";
                await _dbHelper.ExecuteQueryWithParams(sqlPermissions, new string[0]);

                const string sqlRolePermissions = @"
                    CREATE TABLE IF NOT EXISTS role_permissions (
                        RoleId INT NOT NULL,
                        PermissionId INT NOT NULL,
                        CompanyId BIGINT NOT NULL,
                        PRIMARY KEY (RoleId, PermissionId, CompanyId),
                        KEY fk_perm_role (RoleId),
                        KEY fk_perm_permission (PermissionId),
                        KEY fk_perm_company (CompanyId)
                    ) ENGINE=InnoDB;";
                await _dbHelper.ExecuteQueryWithParams(sqlRolePermissions, new string[0]);
                _logger.LogInformation("PBAC tables checked/created successfully.");
            } catch (Exception ex) {
                _logger.LogError(ex, "Error creating PBAC tables");
                throw;
            }
        }

        public async Task SeedPermissions()
        {
            var permissions = new List<(string Key, string Description)>
            {
                ("VIEW_BILLING", "Access the billing section"),
                ("VIEW_EMPLOYEES", "View employee list"),
                ("VIEW_PRODUCTS", "View product management"),
                ("VIEW_REPORTS", "View sales and inventory reports"),
                ("POS_CALCULATOR", "Access POS Calculator Screen"),
                ("MANAGE_EMPLOYEES", "Add, edit, or delete employees"),
                ("MANAGE_PRODUCTS", "Add, edit, or delete products")
            };

            foreach (var p in permissions)
            {
                const string insertSql = @"
                    INSERT INTO permissions (PermissionKey, Description) 
                    VALUES (@1, @2)
                    ON DUPLICATE KEY UPDATE Description = @2;";
                await _dbHelper.ExecuteQueryWithParams(insertSql, new[] { p.Key, p.Description });
            }

            // Default mappings for the two roles (Full Access = 1, Limited Access = 2)
            // Note: Admin (Id 0) is handled separately in logic and gets all permissions.
            
            // Full Access (Role ID 1) - Default all
            foreach (var p in permissions)
            {
                const string mapSql = @"
                    INSERT IGNORE INTO role_permissions (RoleId, PermissionId, CompanyId)
                    SELECT 1, p.Id, c.Id 
                    FROM permissions p, companies c
                    WHERE p.PermissionKey = @1;";
                await _dbHelper.ExecuteQueryWithParams(mapSql, new[] { p.Key });
            }

            // Limited Access (Role ID 2) - Default only billing
            const string limitedSql = @"
                INSERT IGNORE INTO role_permissions (RoleId, PermissionId, CompanyId)
                SELECT 2, p.Id, c.Id 
                FROM permissions p, companies c
                WHERE p.PermissionKey IN ('VIEW_BILLING', 'POS_CALCULATOR');";
            await _dbHelper.ExecuteQueryWithParams(limitedSql, new string[0]);
            _logger.LogInformation("Permissions seeded successfully.");
        }

        public async Task<List<string>> GetUserPermissions(string roleId, string companyId)
        {
            if (roleId == "0") // Special case for Admin
            {
                 var dt = await _dbHelper.GetDataTable("SELECT PermissionKey FROM permissions");
                 var perms = new List<string>();
                 if (dt != null)
                 {
                     foreach (DataRow row in dt.Rows) perms.Add(row["PermissionKey"].ToString()!);
                 }
                 return perms;
            }

            const string sql = @"
                SELECT p.PermissionKey 
                FROM permissions p
                JOIN role_permissions rp ON p.Id = rp.PermissionId
                WHERE rp.RoleId = @1 AND rp.CompanyId = @2;";
            
            var resultDt = await _dbHelper.GetDataTableWithParams(sql, new[] { roleId, companyId });
            var list = new List<string>();
            if (resultDt != null)
            {
                foreach (DataRow row in resultDt.Rows)
                {
                    list.Add(row["PermissionKey"].ToString()!);
                }
            }
            return list;
        }

        public async Task<bool> UpdateRolePermissions(string roleId, string companyId, List<string> permissionKeys)
        {
            try
            {
                // Delete existing for this role/company
                const string deleteSql = "DELETE FROM role_permissions WHERE RoleId = @1 AND CompanyId = @2";
                await _dbHelper.ExecuteQueryWithParams(deleteSql, new[] { roleId, companyId });

                if (permissionKeys == null || permissionKeys.Count == 0) return true;

                // Insert new ones
                foreach (var key in permissionKeys)
                {
                    const string insertSql = @"
                        INSERT INTO role_permissions (RoleId, PermissionId, CompanyId)
                        SELECT @1, Id, @2 FROM permissions WHERE PermissionKey = @3";
                    await _dbHelper.ExecuteQueryWithParams(insertSql, new[] { roleId, companyId, key });
                }
                return true;
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Error updating role permissions");
                return false;
            }
        }

        public async Task<DataTable?> GetAllPermissions()
        {
            return await _dbHelper.GetDataTable("SELECT * FROM permissions");
        }

        public async Task<DataTable?> GetRolePermissions(string roleId, string companyId)
        {
            const string sql = @"
                SELECT p.* FROM permissions p
                JOIN role_permissions rp ON p.Id = rp.PermissionId
                WHERE rp.RoleId = @1 AND rp.CompanyId = @2";
            return await _dbHelper.GetDataTableWithParams(sql, new[] { roleId, companyId });
        }
    }
}
