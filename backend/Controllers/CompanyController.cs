using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Authorization;
using posbillingapp.api.Data;
using posbillingapp.api.Models;
using posbillingapp.api.Services;
using System.Data;

namespace posbillingapp.api.Controllers
{
    [Authorize]
    [ApiController]
    [Route("api/[controller]")]
    public class CompanyController : ControllerBase
    {
        private readonly DbHelper _dbHelper;
        private readonly IPasswordService _passwordService;
        private readonly ILogger<CompanyController> _logger;

        public CompanyController(DbHelper dbHelper, IPasswordService passwordService, ILogger<CompanyController> logger)
        {
            _dbHelper = dbHelper;
            _passwordService = passwordService;
            _logger = logger;
        }

        #region Employee Management

        [Authorize(Roles = "Admin")]
        [HttpPost("employees")]
        public async Task<IActionResult> AddEmployee([FromBody] EmployeeAddRequest request)
        {
            var adminId = User.FindFirst(System.Security.Claims.ClaimTypes.NameIdentifier)?.Value;
            _logger.LogInformation($"AddEmployee called by Admin {adminId} for Company {request.CompanyId}");

            try
            {
                if (string.IsNullOrWhiteSpace(request.Email) || string.IsNullOrWhiteSpace(request.Name))
                {
                    return BadRequest(new { Success = false, Message = "Name and Email are required." });
                }

                string email = request.Email.Trim();
                
                // Enforce CompanyId from JWT
                string? jwtCompId = User.FindFirst("CompanyId")?.Value;
                if (string.IsNullOrEmpty(jwtCompId)) return Unauthorized();
                long companyId = long.Parse(jwtCompId);

                // 2. Duplicate Check
                const string checkSql = @"
                    SELECT 'Admin' as Source FROM companies WHERE Email = @1
                    UNION ALL
                    SELECT 'User' as Source FROM users WHERE Email = @1
                    LIMIT 1;";
                DataTable? dt = await _dbHelper.GetDataTableWithParams(checkSql, new[] { email });
                
                if (dt != null && dt.Rows.Count > 0)
                {
                    return BadRequest(new { Success = false, Message = $"Email '{email}' is already registered as {dt.Rows[0]["Source"]}." });
                }

                // 3. Role Retrieval
                string mappedRole = request.Role ?? "Limited Access";
                if (mappedRole.Contains("Full", StringComparison.OrdinalIgnoreCase) || mappedRole.Equals("Admin", StringComparison.OrdinalIgnoreCase)) 
                    mappedRole = "Full Access";
                else 
                    mappedRole = "Limited Access";

                DataTable? roleDt = await _dbHelper.GetDataTableWithParams("SELECT Id FROM role WHERE RoleName = @1 LIMIT 1;", new[] { mappedRole });
                if (roleDt == null || roleDt.Rows.Count == 0)
                {
                    return BadRequest(new { Success = false, Message = "Critical Error: Valid roles not found in database." });
                }
                long roleId = Convert.ToInt64(roleDt.Rows[0]["Id"]);

                // 4. Credentials
                string defaultPassword = "password123";
                string salt;
                string hash = _passwordService.HashPassword(defaultPassword, out salt);

                // 5. Database Insert
                const string sql = @"
                    INSERT INTO users (CompanyId, Name, Email, PasswordHash, Salt, PhoneNumber, RoleId, PhotoUrl, IsActive, IsDeleted) 
                    VALUES (@1, @2, @3, @4, @5, @6, @7, @8, 1, 0);";
                
                int result = await _dbHelper.ExecuteQueryWithParams(sql, new[] { 
                    companyId.ToString(), 
                    request.Name, 
                    email, 
                    hash,
                    salt,
                    request.PhoneNumber ?? "", 
                    roleId.ToString(), 
                    request.PhotoUrl ?? "" 
                });

                if (result > 0) 
                {
                    _logger.LogInformation($"Employee added: {email}");
                    return Ok(new { Success = true, Message = "Employee added successfully! Default pass: " + defaultPassword });
                }
                
                return BadRequest(new { Success = false, Message = "Database rejected the insert operation." });
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "AddEmployee Exception");
                await _dbHelper.InsertErrorLogAsync("AddEmployee", adminId, ex.Message, ex.StackTrace, null, "Admin");
                return StatusCode(500, new { Success = false, Message = "Backend error: " + ex.Message });
            }
        }

        [Authorize]
        [HttpGet("employees/{companyId}")]
        public async Task<IActionResult> GetEmployees(long companyId)
        {
            try
            {
                // Security check
                var jwtCompId = User.FindFirst("CompanyId")?.Value;
                if (jwtCompId != companyId.ToString()) return Forbid();

                const string sql = @"
                    SELECT u.Id, u.Name, u.Email, u.PhoneNumber, r.RoleName as Role, u.PhotoUrl, u.IsActive 
                    FROM users u 
                    LEFT JOIN role r ON u.RoleId = r.Id 
                    WHERE u.CompanyId = @1 AND u.IsDeleted = 0;";
                DataTable? dt = await _dbHelper.GetDataTableWithParams(sql, new[] { companyId.ToString() });
                
                var employees = new List<EmployeeResponse>();
                if (dt != null)
                {
                    foreach (DataRow row in dt.Rows)
                    {
                        employees.Add(new EmployeeResponse
                        {
                            Id = Convert.ToInt64(row["Id"]),
                            Name = row["Name"]?.ToString() ?? "",
                            Email = row["Email"]?.ToString() ?? "",
                            PhoneNumber = row["PhoneNumber"]?.ToString() ?? "",
                            Role = row["Role"]?.ToString() ?? "",
                            PhotoUrl = row["PhotoUrl"]?.ToString(),
                            IsActive = row["IsActive"] != DBNull.Value && (Convert.ToBoolean(row["IsActive"]) || row["IsActive"].ToString() == "1")
                        });
                    }
                }
                return Ok(employees);
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Error fetching employees");
                await _dbHelper.InsertErrorLogAsync("GetEmployees", companyId.ToString(), ex.Message, ex.StackTrace, null, "User");
                return StatusCode(500, new { Success = false, Message = "Internal error." });
            }
        }

        [Authorize(Roles = "Admin")]
        [HttpPut("employees/{id}")]
        public async Task<IActionResult> UpdateEmployee(long id, [FromBody] EmployeeUpdateRequest request)
        {
            try
            {
                // 1. Map Role
                string mappedRole = request.Role ?? "Limited Access";
                if (mappedRole.Contains("Full", StringComparison.OrdinalIgnoreCase) || mappedRole.Equals("Admin", StringComparison.OrdinalIgnoreCase)) 
                    mappedRole = "Full Access";
                else 
                    mappedRole = "Limited Access";

                DataTable? roleDt = await _dbHelper.GetDataTableWithParams("SELECT Id FROM role WHERE RoleName = @1 LIMIT 1;", new[] { mappedRole });
                if (roleDt == null || roleDt.Rows.Count == 0)
                {
                    return BadRequest(new { Success = false, Message = "Invalid role name provided." });
                }
                long roleId = Convert.ToInt64(roleDt.Rows[0]["Id"]);

                // 2. Update users table
                const string sql = @"
                    UPDATE users SET 
                        Name = @1, 
                        Email = @2,
                        PhoneNumber = @3, 
                        RoleId = @4, 
                        IsActive = @5, 
                        PhotoUrl = @6 
                    WHERE Id = @7;";
                
                int result = await _dbHelper.ExecuteQueryWithParams(sql, new[] { 
                    request.Name, 
                    request.Email,
                    request.PhoneNumber, 
                    roleId.ToString(), 
                    request.IsActive ? "1" : "0", 
                    request.PhotoUrl ?? "",
                    id.ToString() 
                });

                if (result > 0)
                {
                    _logger.LogInformation($"Employee updated: ID {id}, Email {request.Email}");
                    return Ok(new { Success = true, Message = "Employee updated successfully." });
                }
                
                return BadRequest(new { Success = false, Message = "Update failed. Employee not found or no changes made." });
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, $"UpdateEmployee Exception for ID {id}");
                await _dbHelper.InsertErrorLogAsync("UpdateEmployee", id.ToString(), ex.Message, ex.StackTrace, null, "Admin");
                return StatusCode(500, new { Success = false, Message = "Internal error: " + ex.Message });
            }
        }

        [Authorize(Roles = "Admin")]
        [HttpDelete("employees/{id}")]
        public async Task<IActionResult> DeleteEmployee(long id)
        {
            try
            {
                const string sql = "UPDATE users SET IsDeleted = 1, IsActive = 0 WHERE Id = @1;";
                int result = await _dbHelper.ExecuteQueryWithParams(sql, new[] { id.ToString() });
                return result > 0 ? Ok(new { Success = true, Message = "Employee deleted successfully." }) : BadRequest(new { Success = false, Message = "Employee not found." });
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Error deleting employee");
                await _dbHelper.InsertErrorLogAsync("DeleteEmployee", id.ToString(), ex.Message, ex.StackTrace, null, "Admin");
                return StatusCode(500, new { Success = false, Message = "Internal error." });
            }
        }

        #endregion

        #region Menu Management
        
        [HttpGet("categories")]
        public async Task<IActionResult> GetCategories()
        {
            try
            {
                const string sql = "SELECT CategoryName FROM category;";
                DataTable? dt = await _dbHelper.GetDataTable(sql);
                var categories = new List<string>();
                if (dt != null)
                {
                    foreach (DataRow row in dt.Rows)
                    {
                        string? categoryName = row["CategoryName"]?.ToString();
                        if (categoryName != null)
                        {
                            categories.Add(categoryName);
                        }
                    }
                }
                return Ok(categories);
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Error fetching categories");
                return StatusCode(500, new { Success = false, Message = "Internal error." });
            }
        }

        [Authorize(Roles = "Admin")]
        [HttpPost("menu")]
        public async Task<IActionResult> AddMenuItem([FromForm] MenuItemRequest request, IFormFile? imageFile)
        {
            try 
            {
                // Enforce CompanyId from JWT
                string? jwtCompId = User.FindFirst("CompanyId")?.Value;
                if (string.IsNullOrEmpty(jwtCompId)) return Unauthorized();
                long companyId = long.Parse(jwtCompId);

                // Handle Image Upload
                if (imageFile != null && imageFile.Length > 0)
                {
                    var uploadsFolder = Path.Combine(Directory.GetCurrentDirectory(), "wwwroot", "images", "menu_items");
                    if (!Directory.Exists(uploadsFolder)) Directory.CreateDirectory(uploadsFolder);

                    var uniqueFileName = Guid.NewGuid().ToString() + "_" + imageFile.FileName;
                    var filePath = Path.Combine(uploadsFolder, uniqueFileName);

                    using (var fileStream = new FileStream(filePath, FileMode.Create))
                    {
                        await imageFile.CopyToAsync(fileStream);
                    }
                    request.ImageUrl = $"/images/menu_items/{uniqueFileName}";
                }

                // 1. Get or Create Category
                string catName = (request.Category ?? "General").Trim();
                long categoryId;
                
                string catCheckSql = "SELECT Id FROM category WHERE CategoryName = @1";
                DataTable? catDt = await _dbHelper.GetDataTableWithParams(catCheckSql, new[] { catName });

                if (catDt != null && catDt.Rows.Count > 0)
                {
                    categoryId = Convert.ToInt64(catDt.Rows[0]["Id"]);
                }
                else
                {
                    string insertCatSql = "INSERT INTO category (CategoryName) VALUES (@1); SELECT LAST_INSERT_ID();";
                    categoryId = await _dbHelper.ExecuteQueryWithParamsTrn(insertCatSql, new[] { catName });
                }

                if (categoryId <= 0) return BadRequest(new { Success = false, Message = "Failed to process category." });

                // 2. Insert Menu Item
                const string sql = @"
                    INSERT INTO menuitems (CompanyId, Name, CategoryId, Price, Description, ImageUrl, IsAvailable) 
                    VALUES (@1, @2, @3, @4, @5, @6, @7);";
                
                int result = await _dbHelper.ExecuteQueryWithParams(sql, new[] { 
                    companyId.ToString(), 
                    request.Name, 
                    categoryId.ToString(),
                    request.Price.ToString(), 
                    request.Description ?? "", 
                    request.ImageUrl ?? "", 
                    request.IsAvailable ? "1" : "0" 
                });

                return result > 0 ? Ok(new { Success = true, Message = "Menu item added." }) : BadRequest(new { Success = false, Message = "Failed to add menu item." });
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Error adding menu item");
                await _dbHelper.InsertErrorLogAsync("AddMenuItem", null, ex.Message, ex.StackTrace, null, "Admin");
                return StatusCode(500, new { Success = false, Message = "Internal error." });
            }
        }

        [HttpGet("menu/{companyId}")]
        public async Task<IActionResult> GetMenu(long companyId)
        {
            var jwtCompanyId = User.FindFirst("CompanyId")?.Value;
            var role = User.FindFirst(System.Security.Claims.ClaimTypes.Role)?.Value;
            var userId = User.FindFirst(System.Security.Claims.ClaimTypes.NameIdentifier)?.Value;

            _logger.LogInformation($"GetMenu called. CompanyId Path: {companyId}, JWT: {jwtCompanyId}, Role: {role}");

            // Security: Ensure the user is only accessing their own company menu
            if (jwtCompanyId != companyId.ToString())
            {
                return Forbid();
            }

            try
            {
                string sql;
                string[] @params;

                if (role == "Limited Access")
                {
                    // Limited users only see assigned items
                    sql = @"
                        SELECT m.*, c.CategoryName 
                        FROM menuitems m 
                        LEFT JOIN category c ON m.CategoryId = c.Id 
                        INNER JOIN employee_menu_access ema ON m.Id = ema.MenuItemId
                        WHERE m.CompanyId = @1 AND ema.UserId = @2 AND m.IsDeleted = 0;";
                    @params = new[] { companyId.ToString(), userId ?? "0" };
                }
                else
                {
                    // Admin and Full Access see everything
                    sql = @"
                        SELECT m.*, c.CategoryName 
                        FROM menuitems m 
                        LEFT JOIN category c ON m.CategoryId = c.Id 
                        WHERE m.CompanyId = @1 AND m.IsDeleted = 0;";
                    @params = new[] { companyId.ToString() };
                }
                
                DataTable? dt = await _dbHelper.GetDataTableWithParams(sql, @params);
                
                var menu = new List<MenuItemResponse>();
                if (dt != null)
                {
                    foreach (DataRow row in dt.Rows)
                    {
                        menu.Add(new MenuItemResponse
                        {
                            Id = Convert.ToInt64(row["Id"]),
                            CompanyId = Convert.ToInt64(row["CompanyId"]),
                            Name = row["Name"]?.ToString() ?? "",
                            Category = row["CategoryName"]?.ToString() ?? "",
                            Price = Convert.ToDecimal(row["Price"]),
                            Description = row["Description"]?.ToString(),
                            ImageUrl = row["ImageUrl"]?.ToString(),
                            IsAvailable = row["IsAvailable"] != DBNull.Value && (Convert.ToBoolean(row["IsAvailable"]) || row["IsAvailable"].ToString() == "1")
                        });
                    }
                }
                return Ok(menu);
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Error fetching menu");
                await _dbHelper.InsertErrorLogAsync("GetMenu", companyId.ToString(), ex.Message, ex.StackTrace, null, "User");
                return StatusCode(500, new { Success = false, Message = "Internal error." });
            }
        }

        [Authorize(Roles = "Admin")]
        [HttpPut("menu/{id}")]
        public async Task<IActionResult> UpdateMenuItem(long id, [FromForm] MenuItemRequest request, IFormFile? imageFile)
        {
            try
            {
                // Enforce CompanyId from JWT
                string? jwtCompId = User.FindFirst("CompanyId")?.Value;
                if (string.IsNullOrEmpty(jwtCompId)) return Unauthorized();

                // 1. Verify ownership
                const string verifySql = "SELECT CompanyId FROM menuitems WHERE Id = @1 AND IsDeleted = 0 LIMIT 1;";
                DataTable? verifyDt = await _dbHelper.GetDataTableWithParams(verifySql, new[] { id.ToString() });
                if (verifyDt == null || verifyDt.Rows.Count == 0 || verifyDt.Rows[0]["CompanyId"].ToString() != jwtCompId)
                {
                    return Forbid();
                }

                // Handle Image Upload
                if (imageFile != null && imageFile.Length > 0)
                {
                    var uploadsFolder = Path.Combine(Directory.GetCurrentDirectory(), "wwwroot", "images", "menu_items");
                    if (!Directory.Exists(uploadsFolder)) Directory.CreateDirectory(uploadsFolder);

                    var uniqueFileName = Guid.NewGuid().ToString() + "_" + imageFile.FileName;
                    var filePath = Path.Combine(uploadsFolder, uniqueFileName);

                    using (var fileStream = new FileStream(filePath, FileMode.Create))
                    {
                        await imageFile.CopyToAsync(fileStream);
                    }
                    request.ImageUrl = $"/images/menu_items/{uniqueFileName}";
                }

                // 2. Get or Create Category
                string catName = (request.Category ?? "General").Trim();
                long categoryId;
                
                string catCheckSql = "SELECT Id FROM category WHERE CategoryName = @1";
                DataTable? catDt = await _dbHelper.GetDataTableWithParams(catCheckSql, new[] { catName });

                if (catDt != null && catDt.Rows.Count > 0)
                {
                    categoryId = Convert.ToInt64(catDt.Rows[0]["Id"]);
                }
                else
                {
                    string insertCatSql = "INSERT INTO category (CategoryName) VALUES (@1); SELECT LAST_INSERT_ID();";
                    categoryId = await _dbHelper.ExecuteQueryWithParamsTrn(insertCatSql, new[] { catName });
                }

                if (categoryId <= 0) return BadRequest(new { Success = false, Message = "Failed to process category." });

                // Construct Update SQL dynamically based on whether ImageUrl is new
                // Simplification: We update ImageUrl if request.ImageUrl is not null/empty (which we set above if file uploaded)
                // If no file uploaded, client SHOULD send the old ImageUrl if they want to keep it, but since we are using [FromForm], they might not.
                // However, the common pattern is to only update if changed.
                
                string sql;
                string[] queryParams;

                if (!string.IsNullOrEmpty(request.ImageUrl))
                {
                     sql = @"
                        UPDATE menuitems SET Name = @1, CategoryId = @2, Price = @3, Description = @4, ImageUrl = @5, IsAvailable = @6 
                        WHERE Id = @7;";
                     queryParams = new[] { 
                        request.Name, 
                        categoryId.ToString(),
                        request.Price.ToString(), 
                        request.Description ?? "", 
                        request.ImageUrl, 
                        request.IsAvailable ? "1" : "0", 
                        id.ToString() 
                    };
                }
                else
                {
                    // Don't update ImageUrl if not provided
                     sql = @"
                        UPDATE menuitems SET Name = @1, CategoryId = @2, Price = @3, Description = @4, IsAvailable = @5 
                        WHERE Id = @6;";
                     queryParams = new[] { 
                        request.Name, 
                        categoryId.ToString(),
                        request.Price.ToString(), 
                        request.Description ?? "", 
                        request.IsAvailable ? "1" : "0", 
                        id.ToString() 
                    };
                }
                
                int result = await _dbHelper.ExecuteQueryWithParams(sql, queryParams);

                return result > 0 ? Ok(new { Success = true, Message = "Menu item updated." }) : BadRequest(new { Success = false, Message = "Failed to update menu item." });
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Error updating menu item");
                await _dbHelper.InsertErrorLogAsync("UpdateMenuItem", id.ToString(), ex.Message, ex.StackTrace, null, "Admin");
                return StatusCode(500, new { Success = false, Message = "Internal error." });
            }
        }

        [Authorize(Roles = "Admin")]
        [HttpDelete("menu/{id}")]
        public async Task<IActionResult> DeleteMenuItem(long id)
        {
            var jwtCompId = User.FindFirst("CompanyId")?.Value;
            if (string.IsNullOrEmpty(jwtCompId)) return Unauthorized();

            // Verify ownership
            const string verifySql = "SELECT CompanyId FROM menuitems WHERE Id = @1 LIMIT 1;";
            DataTable? dt = await _dbHelper.GetDataTableWithParams(verifySql, new[] { id.ToString() });
            if (dt == null || dt.Rows.Count == 0 || dt.Rows[0]["CompanyId"].ToString() != jwtCompId)
            {
                return Forbid();
            }

            const string sql = "UPDATE menuitems SET IsDeleted = 1 WHERE Id = @1;";
            int result = await _dbHelper.ExecuteQueryWithParams(sql, new[] { id.ToString() });
            return result > 0 ? Ok(new { Success = true, Message = "Item deleted." }) : BadRequest(new { Success = false, Message = "Failed to delete item." });
        }

        #endregion

        #region Company Profile

        [Authorize(Roles = "Admin")]
        [HttpPut("profile/{companyId}")]
        public async Task<IActionResult> UpdateProfile(long companyId, [FromBody] CompanyProfileRequest request)
        {
            var jwtCompanyId = User.FindFirst("CompanyId")?.Value;
            if (jwtCompanyId != companyId.ToString()) return Forbid();

            const string sql = "UPDATE companies SET CompanyName = @1, Email = @2, LogoUrl = @3, CurrencySymbol = @4 WHERE Id = @5;";
            int result = await _dbHelper.ExecuteQueryWithParams(sql, new[] { 
                request.CompanyName, 
                request.Email,
                request.LogoUrl ?? "", 
                request.CurrencySymbol ?? "₹", 
                companyId.ToString() 
            });
            return result > 0 ? Ok(new { Success = true, Message = "Profile updated." }) : BadRequest(new { Success = false, Message = "Failed to update profile." });
        }

        #endregion

        #region Dashboard Stats

        [Authorize]
        [HttpGet("stats/{companyId}")]
        public async Task<IActionResult> GetDashboardStats(long companyId, [FromQuery] string? from = null, [FromQuery] string? to = null)
        {
            var jwtCompanyId = User.FindFirst("CompanyId")?.Value;
            if (jwtCompanyId != companyId.ToString()) return Forbid();

            try
            {
                // 1. Total Active Staff
                const string empSql = "SELECT COUNT(*) FROM users WHERE CompanyId = @1 AND IsDeleted = 0 AND IsActive = 1;";
                DataTable? empDt = await _dbHelper.GetDataTableWithParams(empSql, new[] { companyId.ToString() });
                int employeeCount = empDt != null && empDt.Rows.Count > 0 ? Convert.ToInt32(empDt.Rows[0][0]) : 0;

                // 2. Collection Stats
                string orderSql;
                List<string?> parameters;

                if (User.IsInRole("Admin"))
                {
                    // Admin sees all company orders
                    orderSql = "SELECT COUNT(*) as ReceiptCount, COALESCE(SUM(TotalAmount), 0) as TotalCollection FROM orders WHERE CompanyId = @1";
                    parameters = new List<string?> { companyId.ToString() };
                }
                else
                {
                    // Employees see only their own orders
                    var userId = User.FindFirst(System.Security.Claims.ClaimTypes.NameIdentifier)?.Value;
                    if (string.IsNullOrEmpty(userId)) return Unauthorized();

                    orderSql = "SELECT COUNT(*) as ReceiptCount, COALESCE(SUM(TotalAmount), 0) as TotalCollection FROM orders WHERE UserId = @1";
                    parameters = new List<string?> { userId };
                }

                // Use direct comparison for DATETIME/TIMESTAMP - MySQL handles YYYY-MM-DD HH:mm:ss strings perfectly
                if (!string.IsNullOrEmpty(from))
                {
                    parameters.Add(from);
                    orderSql += " AND CreatedAt >= @" + parameters.Count;
                }
                if (!string.IsNullOrEmpty(to))
                {
                    parameters.Add(to);
                    orderSql += " AND CreatedAt <= @" + parameters.Count;
                }
                orderSql += ";";

                DataTable? orderDt = await _dbHelper.GetDataTableWithParams(orderSql, parameters.ToArray());
                
                int receiptCount = 0;
                decimal totalCollection = 0;

                if (orderDt != null && orderDt.Rows.Count > 0)
                {
                    receiptCount = Convert.ToInt32(orderDt.Rows[0]["ReceiptCount"]);
                    totalCollection = Convert.ToDecimal(orderDt.Rows[0]["TotalCollection"]);
                }

                return Ok(new
                {
                    Success = true,
                    EmployeeCount = employeeCount,
                    TotalCollection = totalCollection,
                    ReceiptCount = receiptCount
                });
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "GetDashboardStats Exception");
                return StatusCode(500, new { Success = false, Message = "Server error." });
            }
        }


        [HttpGet("employees/{userId}/stats")]
        public async Task<IActionResult> GetEmployeeStats(long userId)
        {
            try
            {
                const string sql = @"
                    SELECT 
                        COUNT(*) as TotalReceipts,
                        COALESCE(SUM(TotalAmount), 0) as TotalAmount
                    FROM orders 
                    WHERE UserId = @1";

                DataTable? dt = await _dbHelper.GetDataTableWithParams(sql, new[] { userId.ToString() });

                if (dt == null || dt.Rows.Count == 0)
                {
                    return Ok(new { TotalReceipts = 0, TotalAmount = 0.0 });
                }

                DataRow row = dt.Rows[0];
                return Ok(new
                {
                    TotalReceipts = Convert.ToInt32(row["TotalReceipts"]),
                    TotalAmount = Convert.ToDouble(row["TotalAmount"])
                });
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Error fetching employee stats");
                return StatusCode(500, new { Success = false, Message = "Internal error." });
            }
        }

        [HttpGet("employees/{userId}/menu-access")]
        public async Task<IActionResult> GetAssignedMenuItems(long userId)
        {
            try
            {
                const string sql = "SELECT MenuItemId FROM employee_menu_access WHERE UserId = @1";
                DataTable? dt = await _dbHelper.GetDataTableWithParams(sql, new[] { userId.ToString() });

                List<long> menuItemIds = new List<long>();
                if (dt != null)
                {
                    foreach (DataRow row in dt.Rows)
                    {
                        menuItemIds.Add(Convert.ToInt64(row["MenuItemId"]));
                    }
                }

                return Ok(menuItemIds);
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Error fetching assigned menu items");
                return StatusCode(500, new { Success = false, Message = "Internal error." });
            }
        }

        [HttpGet("employees/{userId}/assigned-menu")]
        public async Task<IActionResult> GetAssignedMenuDetails(long userId)
        {
            try
            {
                const string sql = @"
                    SELECT m.*, c.CategoryName 
                    FROM menuitems m
                    LEFT JOIN category c ON m.CategoryId = c.Id
                    INNER JOIN employee_menu_access a ON m.Id = a.MenuItemId
                    WHERE a.UserId = @1";
                
                DataTable? dt = await _dbHelper.GetDataTableWithParams(sql, new[] { userId.ToString() });
                
                List<MenuItemResponse> items = new List<MenuItemResponse>();
                if (dt != null)
                {
                    foreach (DataRow row in dt.Rows)
                    {
                        items.Add(new MenuItemResponse
                        {
                            Id = Convert.ToInt64(row["Id"]),
                            CompanyId = Convert.ToInt64(row["CompanyId"]),
                            Name = row["Name"].ToString() ?? "",
                            Category = row["CategoryName"]?.ToString() ?? "",
                            Price = Convert.ToDecimal(row["Price"]),
                            Description = row["Description"] != DBNull.Value ? row["Description"].ToString() : "",
                            ImageUrl = row["ImageUrl"] != DBNull.Value ? row["ImageUrl"].ToString() : "",
                            IsAvailable = row["IsAvailable"] != DBNull.Value && Convert.ToBoolean(row["IsAvailable"])
                        });
                    }
                }
                return Ok(items);
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Error fetching assigned menu details");
                return StatusCode(500, new { Success = false, Message = "Internal error." });
            }
        }

        [Authorize(Roles = "Admin")]
        [HttpPost("employees/menu-access")]
        public async Task<IActionResult> UpdateMenuAssignments([FromBody] MenuAssignmentRequest request)
        {
            try
            {
                // Delete existing assignments
                const string deleteSql = "DELETE FROM employee_menu_access WHERE UserId = @1";
                await _dbHelper.ExecuteQueryWithParams(deleteSql, new[] { request.UserId.ToString() });

                // Insert new assignments
                if (request.MenuItemIds != null && request.MenuItemIds.Count > 0)
                {
                    foreach (long menuItemId in request.MenuItemIds)
                    {
                        const string insertSql = @"
                            INSERT INTO employee_menu_access (UserId, MenuItemId) 
                            VALUES (@1, @2)";
                        await _dbHelper.ExecuteQueryWithParams(insertSql, new[] { 
                            request.UserId.ToString(), 
                            menuItemId.ToString() 
                        });
                    }
                }

                return Ok(new { Success = true, Message = "Menu items assigned successfully" });
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Error updating menu assignments");
                return StatusCode(500, new { Success = false, Message = "Internal error." });
            }
        }

        #endregion
    }
}
