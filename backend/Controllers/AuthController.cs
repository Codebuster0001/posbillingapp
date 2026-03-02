using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Authorization;
using posbillingapp.api.Data;
using posbillingapp.api.Models;
using posbillingapp.api.Services;
using System.Data;

namespace posbillingapp.api.Controllers
{
    [ApiController]
    [Route("api/[controller]")]
    public class AuthController : ControllerBase
    {
        private readonly DbHelper _dbHelper;
        private readonly IPasswordService _passwordService;
        private readonly IEmailService _emailService;
        private readonly IJwtService _jwtService;
        private readonly ITokenBlacklistService _blacklistService;
        private readonly IPermissionService _permissionService;
        private readonly ILogger<AuthController> _logger;
        private readonly IServiceScopeFactory _scopeFactory;

        public AuthController(DbHelper dbHelper, IPasswordService passwordService, IEmailService emailService, IJwtService jwtService, ITokenBlacklistService blacklistService, IPermissionService permissionService, ILogger<AuthController> logger, IServiceScopeFactory scopeFactory)
        {
            _dbHelper = dbHelper;
            _passwordService = passwordService;
            _emailService = emailService;
            _jwtService = jwtService;
            _blacklistService = blacklistService;
            _permissionService = permissionService;
            _logger = logger;
            _scopeFactory = scopeFactory;
        }

        [Authorize]
        [HttpGet("whoami")]
        public IActionResult WhoAmI()
        {
            var claims = User.Claims.Select(c => new { c.Type, c.Value }).ToList();
            return Ok(new
            {
                IsAuthenticated = User.Identity?.IsAuthenticated,
                Name = User.Identity?.Name,
                Claims = claims
            });
        }

        [HttpPost("register")]
        public async Task<IActionResult> Register([FromBody] RegisterRequest request)
        {
            try
            {
                // Validate required fields
                if (string.IsNullOrWhiteSpace(request.CompanyName) || string.IsNullOrWhiteSpace(request.Email) || string.IsNullOrWhiteSpace(request.Password))
                {
                    return BadRequest(new AuthResponse { Success = false, Message = "Company name, email, and password are required." });
                }

                // Create Admin Credentials
                string salt;
                string passwordHash = _passwordService.HashPassword(request.Password, out salt);

                // Insert Company - Handle nullable StateId and CityId
                // When StateId or CityId is 0, pass NULL to avoid FK violation
                string? stateIdParam = request.StateId > 0 ? request.StateId.ToString() : null;
                string? cityIdParam = request.CityId > 0 ? request.CityId.ToString() : null;

                const string sql = @"
                    INSERT INTO companies (CompanyName, PhoneNumber, Email, PasswordHash, Salt, Status, CountryId, StateId, CityId) 
                    VALUES (@1, @2, @3, @4, @5, 'Active', @6, @7, @8);";
                
                long companyId = await _dbHelper.ExecuteQueryWithParamsTrn(sql, new[] { 
                    request.CompanyName, 
                    request.PhoneNumber ?? "", 
                    request.Email,
                    passwordHash,
                    salt,
                    request.CountryId > 0 ? request.CountryId.ToString() : "1",
                    stateIdParam,
                    cityIdParam
                });

                if (companyId <= 0)
                {
                    return BadRequest(new AuthResponse { Success = false, Message = "Failed to create company. Email or Phone might already exist." });
                }



                // Fetch currency info for the response
                const string currencySql = "SELECT CurrencyCode, CurrencySymbol FROM countries WHERE Id = @1;";
                DataTable? currencyDt = await _dbHelper.GetDataTableWithParams(currencySql, new[] { request.CountryId.ToString() });
                string currencySymbol = "$";
                string currencyCode = "USD";
                if (currencyDt != null && currencyDt.Rows.Count > 0)
                {
                    currencySymbol = currencyDt.Rows[0]["CurrencySymbol"].ToString() ?? "$";
                    currencyCode = currencyDt.Rows[0]["CurrencyCode"].ToString() ?? "USD";
                }

                return Ok(new AuthResponse { 
                    Success = true, 
                    Message = "Registration successful.", 
                    Role = "Admin",
                    CompanyId = companyId,
                    CurrencySymbol = currencySymbol,
                    CurrencyCode = currencyCode
                });
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Error during registration");
                await _dbHelper.InsertErrorLogAsync("Registration", request.Email, ex.Message, ex.StackTrace, null, "System");
                return StatusCode(500, new AuthResponse { Success = false, Message = "An internal error occurred." });
            }
        }

        [HttpPost("login")]
        public async Task<IActionResult> Login([FromBody] LoginRequest request)
        {
            try
            {
                string email = request.Email.Trim();
                _logger.LogInformation($"Login attempt for: {email}");

                // 1. Check Companies Table (Admin)
                const string adminSql = @"
                    SELECT c.Id, c.CompanyName, c.LogoUrl, c.PasswordHash, c.Salt, c.Status, co.CurrencySymbol, co.CurrencyCode 
                    FROM companies c
                    LEFT JOIN countries co ON c.CountryId = co.Id
                    WHERE c.Email = @1 LIMIT 1;";
                DataTable? adminDt = await _dbHelper.GetDataTableWithParams(adminSql, new[] { email });

                if (adminDt != null && adminDt.Rows.Count > 0)
                {
                    DataRow row = adminDt.Rows[0];
                    if (row["Status"].ToString() != "Active")
                    {
                        _logger.LogWarning($"Login failed: Company account {email} is inactive.");
                        return Unauthorized(new AuthResponse { Success = false, Message = "Company account is inactive." });
                    }

                    string hash = row["PasswordHash"].ToString() ?? "";
                    string salt = row["Salt"].ToString() ?? "";

                    if (_passwordService.VerifyPassword(request.Password, hash, salt))
                    {
                        _logger.LogInformation($"Admin login successful: {email}");
                        long companyId = Convert.ToInt64(row["Id"]);
                        
                        // For Admin, we use "0" as UserId in the JWT to indicate the main account,
                        // and store the CompanyId in the refresh token while leaving UserId null.
                        var permissions = await _permissionService.GetUserPermissions("0", companyId.ToString());
                        string token = _jwtService.GenerateToken("0", email, "Admin", companyId.ToString(), "0", permissions);
                        string refreshToken = _jwtService.GenerateRefreshToken();
                        string ipAddress = HttpContext.Connection.RemoteIpAddress?.ToString() ?? "unknown";

                        // Ensure table structure is correct (Nullable columns)
                        const string migrationSql = @"
                            CREATE TABLE IF NOT EXISTS refresh_tokens (
                                Id BIGINT NOT NULL AUTO_INCREMENT,
                                UserId BIGINT NULL,
                                CompanyId BIGINT NULL,
                                Token VARCHAR(500) NOT NULL,
                                ExpiresAt DATETIME NOT NULL,
                                CreatedAt DATETIME DEFAULT CURRENT_TIMESTAMP,
                                CreatedByIp VARCHAR(50),
                                RevokedAt DATETIME NULL,
                                RevokedByIp VARCHAR(50),
                                ReplacedByToken VARCHAR(500) NULL,
                                IsRevoked TINYINT(1) DEFAULT 0,
                                PRIMARY KEY (Id),
                                KEY fk_refresh_user (UserId),
                                KEY fk_refresh_company (CompanyId)
                            ) ENGINE=InnoDB;
                            ALTER TABLE refresh_tokens MODIFY COLUMN UserId BIGINT NULL;
                            ALTER TABLE refresh_tokens MODIFY COLUMN CompanyId BIGINT NULL;";
                        await _dbHelper.ExecuteQueryWithParams(migrationSql, new string[0]);

                        // Revoke old tokens for this Company Admin (where UserId is null)
                        const string revokeAdminSql = "UPDATE refresh_tokens SET IsRevoked = 1, RevokedAt = @1, RevokedByIp = @2 WHERE CompanyId = @3 AND UserId IS NULL AND IsRevoked = 0;";
                        await _dbHelper.ExecuteQueryWithParams(revokeAdminSql, new[] { DateTime.UtcNow.ToString("yyyy-MM-dd HH:mm:ss"), ipAddress, companyId.ToString() });

                        // Store new Refresh Token for Admin (UserId = null)
                        const string insertTokenAdminSql = @"
                            INSERT INTO refresh_tokens (CompanyId, Token, ExpiresAt, CreatedByIp) 
                            VALUES (@1, @2, @3, @4);";
                        await _dbHelper.ExecuteQueryWithParams(insertTokenAdminSql, new[] { 
                            companyId.ToString(),
                            refreshToken, 
                            DateTime.UtcNow.AddDays(7).ToString("yyyy-MM-dd HH:mm:ss"),
                            ipAddress
                        });

                        return Ok(new AuthResponse {
                            Success = true,
                            Message = "Login successful.",
                            Role = "Admin",
                            CompanyId = companyId,
                            UserId = 0,
                            CompanyName = row["CompanyName"].ToString(),
                            CompanyLogo = row["LogoUrl"].ToString(),
                            CurrencySymbol = row["CurrencySymbol"].ToString() ?? "$",
                            CurrencyCode = row["CurrencyCode"].ToString() ?? "USD",
                            Token = token,
                            RefreshToken = refreshToken,
                            RoleId = 0,
                            Permissions = permissions
                        });
                    }
                    else 
                    {
                        _logger.LogWarning($"Login failed: Password mismatch for Admin {email}");
                    }
                }

                // 2. Check Users Table (Employee)
                const string userSql = @"
                    SELECT u.Id, u.CompanyId, u.PasswordHash, u.Salt, r.RoleName as Role, u.RoleId, u.IsActive, c.CompanyName, c.LogoUrl
                    FROM users u 
                    JOIN role r ON u.RoleId = r.Id 
                    JOIN companies c ON u.CompanyId = c.Id
                    WHERE u.Email = @1 LIMIT 1;";
                DataTable? userDt = await _dbHelper.GetDataTableWithParams(userSql, new[] { email });

                if (userDt != null && userDt.Rows.Count > 0)
                {
                    DataRow row = userDt.Rows[0];
                    if (Convert.ToInt32(row["IsActive"]) == 0)
                    {
                        _logger.LogWarning($"Login failed: User account {email} is inactive.");
                        return Unauthorized(new AuthResponse { Success = false, Message = "User account is inactive." });
                    }

                    string hash = row["PasswordHash"].ToString() ?? "";
                    string salt = row["Salt"].ToString() ?? "";

                    if (_passwordService.VerifyPassword(request.Password, hash, salt))
                    {
                        _logger.LogInformation($"Employee login successful: {email} (Role: {row["Role"]})");
                        long companyId = Convert.ToInt64(row["CompanyId"]);
                        long userId = Convert.ToInt64(row["Id"]);
                        string role = row["Role"].ToString() ?? "User";
                        string roleId = row["RoleId"].ToString() ?? "0";
                        var permissions = await _permissionService.GetUserPermissions(roleId, companyId.ToString());
                        string token = _jwtService.GenerateToken(userId.ToString(), email, role, companyId.ToString(), roleId, permissions);
                        string refreshToken = _jwtService.GenerateRefreshToken();
                        string ipAddress = HttpContext.Connection.RemoteIpAddress?.ToString() ?? "unknown";

                        // Revoke old tokens for this user (Employee flow: CompanyId left null in refresh token)
                        const string revokeUserSql = "UPDATE refresh_tokens SET IsRevoked = 1, RevokedAt = @1, RevokedByIp = @2 WHERE UserId = @3 AND CompanyId IS NULL AND IsRevoked = 0;";
                        await _dbHelper.ExecuteQueryWithParams(revokeUserSql, new[] { DateTime.UtcNow.ToString("yyyy-MM-dd HH:mm:ss"), ipAddress, userId.ToString() });

                        // Store new Refresh Token for User (CompanyId = null)
                        const string insertTokenUserNewSql = @"
                            INSERT INTO refresh_tokens (UserId, Token, ExpiresAt, CreatedByIp) 
                            VALUES (@1, @2, @3, @4);";
                        await _dbHelper.ExecuteQueryWithParams(insertTokenUserNewSql, new[] { 
                            userId.ToString(),
                            refreshToken, 
                            DateTime.UtcNow.AddDays(7).ToString("yyyy-MM-dd HH:mm:ss"),
                            ipAddress
                        });

                        // Fetch Currency using user-specified query logic
                        string currencySymbol = "$";
                        string currencyCode = "USD";
                        // "select CurrencySymbol,CurrencyCode from countries co join companies c on co.Id = c.countryId where c.id=1 and status = "Active";"
                        // Parameterized for the specific companyId
                        string currencySql = "SELECT co.CurrencySymbol, co.CurrencyCode FROM countries co JOIN companies c ON co.Id = c.CountryId WHERE c.Id = @1 AND c.Status = 'Active';";
                        DataTable? currencyDt = await _dbHelper.GetDataTableWithParams(currencySql, new[] { companyId.ToString() });
                        
                        if (currencyDt != null && currencyDt.Rows.Count > 0)
                        {
                            currencySymbol = currencyDt.Rows[0]["CurrencySymbol"].ToString() ?? "$";
                            currencyCode = currencyDt.Rows[0]["CurrencyCode"].ToString() ?? "USD";
                        }

                        return Ok(new AuthResponse {
                            Success = true,
                            Message = "Login successful.",
                            Role = role,
                            CompanyId = companyId,
                            UserId = userId,
                            CompanyName = row["CompanyName"].ToString(),
                            CompanyLogo = row["LogoUrl"].ToString(),
                            CurrencySymbol = currencySymbol,
                            CurrencyCode = currencyCode,
                            Token = token,
                            RefreshToken = refreshToken,
                            RoleId = int.Parse(roleId),
                            Permissions = permissions
                        });
                    }
                    else 
                    {
                        _logger.LogWarning($"Login failed: Password mismatch for Employee {email}");
                    }
                }

                _logger.LogWarning($"Login failed: Invalid credentials for {email}");
                return Unauthorized(new AuthResponse { Success = false, Message = "Invalid email or password." });
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, $"Error during login for {request.Email}");
                await _dbHelper.InsertErrorLogAsync("Login", request.Email, ex.Message, ex.StackTrace, null, "System");
                return StatusCode(500, new AuthResponse { Success = false, Message = "An internal error occurred." });
            }
        }

        [HttpPost("forgot-password")]
        public async Task<IActionResult> ForgotPassword([FromBody] ForgotPasswordRequest request)
        {
            try
            {
                string email = request.Email.Trim();
                // Check if user exists in either companies or users
                const string checkSql = @"
                    SELECT Email FROM companies WHERE Email = @1 
                    UNION 
                    SELECT Email FROM users WHERE Email = @1 LIMIT 1;";
                DataTable? dt = await _dbHelper.GetDataTableWithParams(checkSql, new[] { email });

                if (dt == null || dt.Rows.Count == 0)
                {
                    return Ok(new AuthResponse { Success = true, Message = "If the email is registered, an OTP has been sent." });
                }

                // Delete any existing OTPs for this email before generating a new one
                const string deleteSql = "DELETE FROM otps WHERE Email = @1;";
                await _dbHelper.ExecuteQueryWithParams(deleteSql, new[] { email });

                // Generate new OTP
                string otp = new Random().Next(100000, 999999).ToString();
                
                // Use Database time for expiry (3 minutes from now)
                const string otpSql = "INSERT INTO otps (Email, OtpCode, ExpiryTime) VALUES (@1, @2, DATE_ADD(NOW(), INTERVAL 3 MINUTE));";
                await _dbHelper.ExecuteQueryWithParams(otpSql, new[] { email, otp });

                _logger.LogInformation($"New OTP for {email}: {otp} (Created at {DateTime.Now})");

                _logger.LogInformation($"New OTP for {email}: {otp}");

                // Send email in background using a new scope to avoid ObjectDisposedException
                _ = Task.Run(async () => {
                    try 
                    {
                        using var scope = _scopeFactory.CreateScope();
                        var emailService = scope.ServiceProvider.GetRequiredService<IEmailService>();
                        await emailService.SendOtpEmail(email, otp); 
                    }
                    catch (Exception ex) 
                    { 
                        _logger.LogError(ex, "Background email fail"); 
                    }
                });

                return Ok(new AuthResponse { 
                    Success = true, 
                    Message = "OTP sent! Please check your email inbox and spam folder." 
                });
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Error during forgot password");
                await _dbHelper.InsertErrorLogAsync("ForgotPassword", request.Email, ex.Message, ex.StackTrace, null, "System");
                return StatusCode(500, new AuthResponse { Success = false, Message = "An internal error occurred." });
            }
        }

        [HttpPost("reset-password")]
        public async Task<IActionResult> ResetPassword([FromBody] ResetPasswordRequest request)
        {
            try
            {
                string email = request.Email.Trim();
                string otpCode = request.OtpCode.Trim();

                // Validate OTP using Database time (NOW())
                const string validateSql = "SELECT Id FROM otps WHERE Email = @1 AND OtpCode = @2 AND ExpiryTime > NOW() LIMIT 1;";
                DataTable? dt = await _dbHelper.GetDataTableWithParams(validateSql, new[] { email, otpCode });

                if (dt == null || dt.Rows.Count == 0)
                {
                    _logger.LogWarning($"ResetPassword: OTP validation failed or expired for {email}. Code: {otpCode}");
                    return BadRequest(new AuthResponse { Success = false, Message = "Invalid or expired OTP. Please request a new one." });
                }

                // Hash new password
                string salt;
                string hash = _passwordService.HashPassword(request.NewPassword, out salt);

                // Update password in companies (Admin)
                const string updateAdminSql = "UPDATE companies SET PasswordHash = @1, Salt = @2, UpdatedAt = CURRENT_TIMESTAMP WHERE Email = @3;";
                int adminRows = await _dbHelper.ExecuteQueryWithParams(updateAdminSql, new[] { hash, salt, email });

                // Update password in users (Employee)
                const string updateUserSql = "UPDATE users SET PasswordHash = @1, Salt = @2, UpdatedAt = CURRENT_TIMESTAMP WHERE Email = @3;";
                int userRows = await _dbHelper.ExecuteQueryWithParams(updateUserSql, new[] { hash, salt, email });

                if (adminRows > 0 || userRows > 0)
                {
                    // Delete the used OTP
                    const string cleanOtpSql = "DELETE FROM otps WHERE Email = @1 AND OtpCode = @2;";
                    await _dbHelper.ExecuteQueryWithParams(cleanOtpSql, new[] { email, otpCode });

                    return Ok(new AuthResponse { Success = true, Message = "Password reset successful. You can now login with your new password." });
                }

                _logger.LogWarning($"ResetPassword: No user found to update for {email} (adminRows: {adminRows}, userRows: {userRows})");
                return BadRequest(new AuthResponse { Success = false, Message = "Failed to update password. User account not found." });
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Error during reset password");
                        _logger.LogError(ex, "Error during reset password");
                await _dbHelper.InsertErrorLogAsync("ResetPassword", request.Email, ex.Message, ex.StackTrace, null, "System");
                return StatusCode(500, new AuthResponse { Success = false, Message = "An internal error occurred." });
            }
        }

        [HttpPost("refresh")]
        public async Task<IActionResult> Refresh([FromBody] RefreshRequest request)
        {
            try
            {
                if (string.IsNullOrWhiteSpace(request.RefreshToken)) return BadRequest("Refresh token is required.");

                const string sql = @"
                    SELECT UserId, CompanyId, ExpiresAt, IsRevoked 
                    FROM refresh_tokens 
                    WHERE Token = @1 LIMIT 1;";
                DataTable? dt = await _dbHelper.GetDataTableWithParams(sql, new[] { request.RefreshToken });

                if (dt == null || dt.Rows.Count == 0) return Unauthorized("Invalid refresh token.");

                DataRow row = dt.Rows[0];
                if (Convert.ToInt32(row["IsRevoked"]) == 1) return Unauthorized("Token revoked.");
                if (Convert.ToDateTime(row["ExpiresAt"]) < DateTime.UtcNow) return Unauthorized("Token expired.");

                string newToken, email, role, companyIdStr, userIdStr;
                string roleIdStr = "0";

                // Case 1: Admin Token (CompanyId set, UserId null)
                if (row["UserId"] == DBNull.Value && row["CompanyId"] != DBNull.Value)
                {
                    long companyId = Convert.ToInt64(row["CompanyId"]);
                    const string adminSql = "SELECT Email FROM companies WHERE Id = @1 AND Status = 'Active' LIMIT 1;";
                    DataTable? adminDt = await _dbHelper.GetDataTableWithParams(adminSql, new[] { companyId.ToString() });
                    if (adminDt == null || adminDt.Rows.Count == 0) return Unauthorized("Company account deactivated.");

                    email = adminDt.Rows[0]["Email"].ToString()!;
                    role = "Admin";
                    companyIdStr = companyId.ToString();
                    userIdStr = "0"; // Main admin account identifier
                    roleIdStr = "0";
                }
                // Case 2: User/Employee Token (UserId set, CompanyId null)
                else if (row["UserId"] != DBNull.Value)
                {
                    long userId = Convert.ToInt64(row["UserId"]);
                    const string userCheckSql = @"
                        SELECT u.Email, r.RoleName, u.CompanyId, u.RoleId 
                        FROM users u 
                        JOIN role r ON u.RoleId = r.Id 
                        WHERE u.Id = @1 AND u.IsDeleted = 0 AND u.IsActive = 1 LIMIT 1;";
                    DataTable? userDt = await _dbHelper.GetDataTableWithParams(userCheckSql, new[] { userId.ToString() });

                    if (userDt == null || userDt.Rows.Count == 0) return Unauthorized("User account deactivated.");

                    email = userDt.Rows[0]["Email"].ToString()!;
                    role = userDt.Rows[0]["RoleName"].ToString()!;
                    companyIdStr = userDt.Rows[0]["CompanyId"].ToString()!;
                    userIdStr = userId.ToString();
                    roleIdStr = userDt.Rows[0]["RoleId"].ToString()!;
                }
                else
                {
                    return Unauthorized("Invalid token session mapping.");
                }

                var permissions = await _permissionService.GetUserPermissions(roleIdStr, companyIdStr);
                newToken = _jwtService.GenerateToken(userIdStr, email, role, companyIdStr, roleIdStr, permissions);
                return Ok(new { Success = true, Token = newToken, Permissions = permissions });
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Error during token refresh");
                return StatusCode(500, "Internal error during refresh");
            }
        }

        [Authorize]
        [HttpPost("logout")]
        public async Task<IActionResult> Logout()
        {
            try
            {
                var token = Request.Headers["Authorization"].ToString().Replace("Bearer ", "");
                if (!string.IsNullOrEmpty(token))
                {
                    // Blacklist the access token
                    _blacklistService.BlacklistToken(token, DateTime.UtcNow.AddMinutes(15));
                }

                // Revoke the refresh token from the database if provided in headers or body
                // For simplicity, we can revoke all active tokens for the current user ID
                var userId = User.FindFirst(System.Security.Claims.ClaimTypes.NameIdentifier)?.Value;
                if (!string.IsNullOrEmpty(userId))
                {
                    const string revokeAllSql = "UPDATE refresh_tokens SET IsRevoked = 1, RevokedAt = @1, RevokedByIp = @2 WHERE UserId = @3 AND IsRevoked = 0;";
                    string ipAddress = HttpContext.Connection.RemoteIpAddress?.ToString() ?? "unknown";
                    await _dbHelper.ExecuteQueryWithParams(revokeAllSql, new[] { DateTime.UtcNow.ToString("yyyy-MM-dd HH:mm:ss"), ipAddress, userId });
                }

                _logger.LogInformation($"User {userId} logged out and tokens invalidated.");
                return Ok(new { Success = true, Message = "Logged out successfully." });
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Error during logout");
                return StatusCode(500, new { Success = false, Message = "Logout failed." });
            }
        }
    }
}
