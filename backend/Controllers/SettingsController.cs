using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using posbillingapp.api.Models;
using posbillingapp.api.Services;
using System.Security.Claims;

namespace posbillingapp.api.Controllers
{
    [Authorize]
    [ApiController]
    [Route("api/[controller]")]
    public class SettingsController : ControllerBase
    {
        private readonly ISettingsService _settingsService;
        private readonly IBankValidationService _bankValidationService;
        private readonly ICardValidationService _cardValidationService;
        private readonly ILogger<SettingsController> _logger;

        public SettingsController(
            ISettingsService settingsService, 
            IBankValidationService bankValidationService,
            ICardValidationService cardValidationService,
            ILogger<SettingsController> logger)
        {
            _settingsService = settingsService;
            _bankValidationService = bankValidationService;
            _cardValidationService = cardValidationService;
            _logger = logger;
        }

        [HttpGet("profile/{companyId}")]
        public async Task<IActionResult> GetProfile(long companyId)
        {
            var userId = User.FindFirst(ClaimTypes.NameIdentifier)?.Value;
            var jwtCompanyId = User.FindFirst("CompanyId")?.Value;
            
            _logger.LogInformation($"GetProfile request for companyId: {companyId}, JWT CompanyId: {jwtCompanyId}, User: {userId}");

            // Trust the JWT claim more than the URL parameter for security
            if (!string.IsNullOrEmpty(jwtCompanyId)) 
            {
                companyId = long.Parse(jwtCompanyId);
            }

            var dt = await _settingsService.GetCompanyProfile(companyId);
            if (dt == null || dt.Rows.Count == 0) 
            {
                _logger.LogWarning($"Company profile not found for ID: {companyId}");
                return NotFound(new { message = $"Profile not found for company {companyId}" });
            }

            var row = dt.Rows[0];
            return Ok(new
            {
                CompanyName = row["CompanyName"],
                Email = row["Email"],
                PhoneNumber = row["PhoneNumber"],
                LogoUrl = row["LogoUrl"],
                CountryId = row["CountryId"],
                CountryName = row["CountryName"],
                StateId = row["StateId"],
                StateName = row["StateName"],
                CityId = row["CityId"],
                CityName = row["CityName"]
            });
        }

        [HttpPut("profile/{companyId}")]
        public async Task<IActionResult> UpdateProfile(long companyId, [FromBody] CompanyProfileUpdate model)
        {
            var jwtCompanyId = User.FindFirst("CompanyId")?.Value;
            if (!string.IsNullOrEmpty(jwtCompanyId)) companyId = long.Parse(jwtCompanyId);

            var success = await _settingsService.UpdateCompanyProfile(companyId, model);
            return success ? Ok(new { message = "Profile updated successfully" }) : BadRequest("Failed to update profile");
        }

        [HttpPost("logo/{companyId}")]
        public async Task<IActionResult> UpdateLogo(long companyId, [FromForm] IFormFile file)
        {
            var jwtCompanyId = User.FindFirst("CompanyId")?.Value;
            if (!string.IsNullOrEmpty(jwtCompanyId)) companyId = long.Parse(jwtCompanyId);

            if (file == null || file.Length == 0) return BadRequest("No file uploaded");

            var extension = Path.GetExtension(file.FileName);
            var fileName = $"{Guid.NewGuid()}{extension}";
            var path = Path.Combine(Directory.GetCurrentDirectory(), "wwwroot", "images", "logos");

            if (!Directory.Exists(path)) Directory.CreateDirectory(path);

            var filePath = Path.Combine(path, fileName);
            using (var stream = new FileStream(filePath, FileMode.Create))
            {
                await file.CopyToAsync(stream);
            }

            var logoUrl = $"/images/logos/{fileName}";
            var success = await _settingsService.UpdateLogo(companyId, logoUrl);

            return success ? Ok(new { logoUrl, message = "Logo updated successfully" }) : BadRequest("Failed to update logo in database");
        }

        [HttpGet("bank/{companyId}")]
        public async Task<IActionResult> GetBankDetails(long companyId)
        {
            var jwtCompanyId = User.FindFirst("CompanyId")?.Value;
            long effectiveCompanyId = companyId;
            if (!string.IsNullOrEmpty(jwtCompanyId)) effectiveCompanyId = long.Parse(jwtCompanyId);

            _logger.LogInformation($"GetBankDetails request. URL ID: {companyId}, JWT ID: {jwtCompanyId}, Effective ID: {effectiveCompanyId}");

            var dt = await _settingsService.GetBankDetails(effectiveCompanyId);
            var list = new List<BankDetailsModel>();
            if (dt != null)
            {
                _logger.LogInformation($"Found {dt.Rows.Count} bank rows for company {effectiveCompanyId}");
                foreach (System.Data.DataRow row in dt.Rows)
                {
                    try
                    {
                        list.Add(new BankDetailsModel
                        {
                            Id = dt.Columns.Contains("Id") ? Convert.ToInt64(row["Id"]) : 0,
                            AccountHolderName = dt.Columns.Contains("AccountHolderName") ? row["AccountHolderName"]?.ToString() : "",
                            AccountNumber = dt.Columns.Contains("AccountNumber") ? row["AccountNumber"]?.ToString() : "",
                            IFSCCode = dt.Columns.Contains("IFSCCode") ? row["IFSCCode"]?.ToString() : "",
                            BankName = dt.Columns.Contains("BankName") ? row["BankName"]?.ToString() : "",
                            RegisteredName = dt.Columns.Contains("RegisteredName") ? row["RegisteredName"]?.ToString() : "",
                            MatchScore = dt.Columns.Contains("MatchScore") ? Convert.ToInt32(row["MatchScore"]) : 0,
                            Branch = dt.Columns.Contains("Branch") ? row["Branch"]?.ToString() : "",
                            BankType = dt.Columns.Contains("BankType") ? row["BankType"]?.ToString() : "",
                            IsPrimary = dt.Columns.Contains("IsPrimary") && row["IsPrimary"] != DBNull.Value && (
                                row["IsPrimary"] is bool b ? b : 
                                row["IsPrimary"] is int i ? i != 0 :
                                row["IsPrimary"] is long l ? l != 0 :
                                row["IsPrimary"] is sbyte s ? s != 0 :
                                Convert.ToBoolean(row["IsPrimary"])
                            ),
                            Status = dt.Columns.Contains("Status") ? row["Status"]?.ToString() ?? "Pending" : "Pending"
                        });
                    }
                    catch (Exception rowEx)
                    {
                        _logger.LogError(rowEx, $"Error parsing bank row for company {effectiveCompanyId}");
                    }
                }
            }
            else
            {
                _logger.LogWarning($"GetBankDetails returned null DataTable for company {effectiveCompanyId}");
            }

            _logger.LogInformation($"Returning {list.Count} bank accounts for company {effectiveCompanyId}");
            return Ok(list);
        }

        [HttpPost("bank/{companyId}")]
        public async Task<IActionResult> AddBankDetails(long companyId, [FromBody] BankDetailsModel model)
        {
            var jwtCompanyId = User.FindFirst("CompanyId")?.Value;
            if (!string.IsNullOrEmpty(jwtCompanyId)) companyId = long.Parse(jwtCompanyId);

            _logger.LogInformation("AddBankDetails via Razorpay verification for company: {CompanyId}", companyId);

            // 1. Razorpay / Local validation
            var validation = await _bankValidationService.ValidateBankAsync(model, companyId);

            // 2. Set enriched fields
            model.Status = validation.Verified ? "Verified" : "Mismatch";
            model.RegisteredName = validation.RegisteredName;
            model.MatchScore = validation.MatchScore;
            model.Branch = validation.Branch;
            model.BankType = validation.BankType;
            model.BankName = validation.BankName ?? model.BankName;

            // 3. Encrypt Account Number before saving (never store plain text)
            string originalAccountNumber = model.AccountNumber!;
            model.AccountNumber = _bankValidationService.EncryptAccountNumber(originalAccountNumber);

            var success = await _settingsService.AddBankDetails(companyId, model);

            if (success)
            {
                return Ok(new {
                    message = validation.Verified
                        ? "Bank account verified and matched successfully"
                        : $"Identity Mismatch: {validation.Message}",
                    verified = validation.Verified,
                    registeredName = validation.RegisteredName,
                    matchScore = validation.MatchScore,
                    bankName = validation.BankName,
                    branch = validation.Branch,
                    bankType = validation.BankType,
                    verificationMessage = validation.Message
                });
            }

            return BadRequest("Failed to add bank account to database");
        }

        [HttpPost("bank/primary/{accountId}")]
        public async Task<IActionResult> SetPrimaryBank(long accountId)
        {
            var jwtCompanyId = User.FindFirst("CompanyId")?.Value;
            if (string.IsNullOrEmpty(jwtCompanyId)) return Unauthorized();
            long companyId = long.Parse(jwtCompanyId);

            var success = await _settingsService.SetPrimaryBank(companyId, accountId);
            return success ? Ok(new { message = "Primary bank account updated" }) : BadRequest("Failed to update primary account");
        }

        [HttpDelete("bank/{accountId}")]
        public async Task<IActionResult> DeleteBank(long accountId)
        {
            var success = await _settingsService.DeleteBankDetails(accountId);
            return success ? Ok(new { message = "Bank account deleted" }) : BadRequest("Failed to delete bank account");
        }

        [HttpPost("bank/verify/{accountId}")]
        public async Task<IActionResult> VerifyBank(long accountId, [FromBody] StatusUpdateModel model)
        {
            var success = await _settingsService.UpdateBankStatus(accountId, model.Status);
            return success ? Ok(new { message = $"Bank account status updated to {model.Status}" }) : BadRequest("Failed to update status");
        }

        [HttpGet("card/{companyId}")]
        public async Task<IActionResult> GetCardDetails(long companyId)
        {
            var jwtCompanyId = User.FindFirst("CompanyId")?.Value;
            if (!string.IsNullOrEmpty(jwtCompanyId)) companyId = long.Parse(jwtCompanyId);

            var dt = await _settingsService.GetCardDetails(companyId);
            var list = new List<CardDetailsModel>();
            if (dt != null)
            {
                foreach (System.Data.DataRow row in dt.Rows)
                {
                    list.Add(new CardDetailsModel
                    {
                        Id = Convert.ToInt64(row["Id"]),
                        CardHolderName = row["CardHolderName"]?.ToString(),
                        CardNumber = row["CardNumber"]?.ToString(), // Masked
                        CardType = row["CardType"]?.ToString(),
                        ExpiryMonth = Convert.ToInt32(row["ExpiryMonth"]),
                        ExpiryYear = Convert.ToInt32(row["ExpiryYear"]),
                        IsPrimary = Convert.ToBoolean(row["IsPrimary"]),
                        Status = row["Status"]?.ToString() ?? "Verified"
                    });
                }
            }
            return Ok(list);
        }

        [HttpPost("card/{companyId}")]
        public async Task<IActionResult> AddCardDetails(long companyId, [FromBody] CardDetailsModel model)
        {
            var jwtCompanyId = User.FindFirst("CompanyId")?.Value;
            if (!string.IsNullOrEmpty(jwtCompanyId)) companyId = long.Parse(jwtCompanyId);

            // 1. Validate Card (Luhn etc)
            var validation = _cardValidationService.ValidateCard(model.CardNumber!, model.CardHolderName, model.ExpiryMonth, model.ExpiryYear);
            if (!validation.Verified) return BadRequest(new { message = validation.Message });

            // 2. Mask Card Number before saving
            model.CardNumber = _cardValidationService.MaskCardNumber(model.CardNumber!);
            model.CardType = validation.BankType; // Store detected type
            model.Status = "Verified";

            var success = await _settingsService.AddCardDetails(companyId, model);
            return success ? Ok(new { message = "Card verified and saved successfully", card = model }) : BadRequest("Failed to save card");
        }

        [HttpPost("card/primary/{cardId}")]
        public async Task<IActionResult> SetPrimaryCard(long cardId)
        {
            var jwtCompanyId = User.FindFirst("CompanyId")?.Value;
            if (string.IsNullOrEmpty(jwtCompanyId)) return Unauthorized();
            long companyId = long.Parse(jwtCompanyId);

            var success = await _settingsService.SetPrimaryCard(companyId, cardId);
            return success ? Ok(new { message = "Primary card updated" }) : BadRequest("Failed to update primary card");
        }

        [HttpDelete("card/{cardId}")]
        public async Task<IActionResult> DeleteCard(long cardId)
        {
            var success = await _settingsService.DeleteCardDetails(cardId);
            return success ? Ok(new { message = "Card deleted" }) : BadRequest("Failed to delete card");
        }

        [HttpGet("payment-modes")]
        public async Task<IActionResult> GetPaymentModes()
        {
            var modes = await _settingsService.GetPaymentModes();
            return Ok(modes);
        }
    }
}
