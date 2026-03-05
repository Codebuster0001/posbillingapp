using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Authorization;
using posbillingapp.api.Data;
using posbillingapp.api.Models;
using System.Data;

namespace posbillingapp.api.Controllers
{
    [Authorize]
    [ApiController]
    [Route("api/[controller]")]
    public class BillingController : ControllerBase
    {
        private readonly DbHelper _dbHelper;
        private readonly ILogger<BillingController> _logger;

        public BillingController(DbHelper dbHelper, ILogger<BillingController> logger)
        {
            _dbHelper = dbHelper;
            _logger = logger;
        }

        [Authorize(Roles = "Admin")]
        [HttpGet("setup")]
        public async Task<IActionResult> SetupDatabase()
        {
            try
            {
                // Fix for Admin orders: Make UserId nullable in orders table
                const string sql1 = "ALTER TABLE orders MODIFY COLUMN UserId bigint NULL;";
                await _dbHelper.ExecuteQueryWithParams(sql1, new string[0]);

                // Create employee_menu_access table if not exists
                const string sql2 = @"
                    CREATE TABLE IF NOT EXISTS employee_menu_access (
                        Id bigint NOT NULL AUTO_INCREMENT,
                        UserId bigint NOT NULL,
                        MenuItemId bigint NOT NULL,
                        CreatedAt datetime DEFAULT CURRENT_TIMESTAMP,
                        PRIMARY KEY (Id),
                        UNIQUE KEY unique_user_menu (UserId, MenuItemId),
                        CONSTRAINT fk_emp_menu_user FOREIGN KEY (UserId) REFERENCES users (Id) ON DELETE CASCADE,
                        CONSTRAINT fk_emp_menu_item FOREIGN KEY (MenuItemId) REFERENCES menuitems (Id) ON DELETE CASCADE
                    );";
                await _dbHelper.ExecuteQueryWithParams(sql2, new string[0]);

                return Ok(new { Success = true, Message = "Database setup for billing complete." });
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Error during setup");
                return StatusCode(500, new { Success = false, Message = "Setup failed: " + ex.Message });
            }
        }

        [HttpGet("next-number/{companyId}")]
        public async Task<IActionResult> GetNextBillNumber(long companyId)
        {
            try
            {
                // Count orders for this company to determine next number
                const string sql = "SELECT COUNT(*) FROM orders WHERE CompanyId = @1;";
                DataTable? dt = await _dbHelper.GetDataTableWithParams(sql, new[] { companyId.ToString() });
                
                int nextNumber = 1;
                if (dt != null && dt.Rows.Count > 0)
                {
                    nextNumber = Convert.ToInt32(dt.Rows[0][0]) + 1;
                }
                
                return Ok(new AuthResponse { 
                    Success = true, 
                    Message = nextNumber.ToString() 
                });
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Error getting next bill number");
                await _dbHelper.InsertErrorLogAsync("NextBillNumber", companyId.ToString(), ex.Message, ex.StackTrace, null, "User");
                return StatusCode(500, new AuthResponse { Success = false, Message = "1" });
            }
        }

        [HttpPost("order")]
        public async Task<IActionResult> CreateOrder([FromBody] OrderRequest request)
        {
            try
            {
                // Ensure the table supports NULL UserId (Run setup implicitly if needed or catch error)
                // For now, let's just attempt insert. If it fails due to FK, we might need to run setup.

                // BILL CREATION LOGIC:
                // Case 1: Company/Admin creates bill → UserId = NULL (no FK constraint)
                // Case 2: Employee creates bill → UserId = actual user ID (FK enforced)
                string? userIdParam = (request.UserId > 0) ? request.UserId.ToString() : null;

                // 1. Insert Order
                const string orderSql = @"
                    INSERT INTO orders (CompanyId, UserId, BillNumber, TotalAmount) 
                    VALUES (@1, @2, @3, @4);
                    SELECT LAST_INSERT_ID();";
                
                try 
                {
                    DataTable? dt = await _dbHelper.GetDataTableWithParams(orderSql, new[] {
                        request.CompanyId.ToString(),
                        userIdParam,
                        request.BillNumber,
                        request.TotalAmount.ToString()
                    });

                    if (dt == null || dt.Rows.Count == 0) return BadRequest(new { Message = "Failed to create order" });
                    
                    long orderId = Convert.ToInt64(dt.Rows[0][0]);

                    // 2. Insert Items
                    foreach (var item in request.Items)
                    {
                        const string itemSql = @"
                            INSERT INTO orderitems (OrderId, MenuItemId, ItemName, Price, Quantity) 
                            VALUES (@1, @2, @3, @4, @5);";
                        
                        await _dbHelper.ExecuteQueryWithParams(itemSql, new[] {
                            orderId.ToString(),
                            item.MenuItemId.ToString(),
                            item.ItemName,
                            item.Price.ToString(),
                            item.Quantity.ToString()
                        });
                    }

                    return Ok(new { Success = true, OrderId = orderId, Message = "Order created successfully" });
                }
                catch (Exception ex)
                {
                    if (ex.Message.Contains("constraint")) {
                         // Attempt auto-fix for the constraint issue
                         const string fixSql = "ALTER TABLE orders MODIFY COLUMN UserId bigint NULL;";
                         await _dbHelper.ExecuteQueryWithParams(fixSql, new string[0]);
                         
                         // Retry once
                         DataTable? dt = await _dbHelper.GetDataTableWithParams(orderSql, new[] {
                            request.CompanyId.ToString(),
                            userIdParam,
                            request.BillNumber,
                            request.TotalAmount.ToString()
                        });

                        if (dt != null && dt.Rows.Count > 0)
                        {
                             long orderId = Convert.ToInt64(dt.Rows[0][0]);
                             // Insert items (same code as above, simplified for retry)
                             foreach (var item in request.Items)
                             {
                                const string itemSql = @"INSERT INTO orderitems (OrderId, MenuItemId, ItemName, Price, Quantity) VALUES (@1, @2, @3, @4, @5);";
                                await _dbHelper.ExecuteQueryWithParams(itemSql, new[] { orderId.ToString(), item.MenuItemId.ToString(), item.ItemName, item.Price.ToString(), item.Quantity.ToString() });
                             }
                             return Ok(new { Success = true, OrderId = orderId, Message = "Order created successfully (after schema fix)" });
                        }
                    }
                    throw;
                }
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Error creating order");
                await _dbHelper.InsertErrorLogAsync("CreateOrder", request.CompanyId.ToString(), ex.Message, ex.StackTrace, null, "User");
                return StatusCode(500, new { Success = false, Message = "Internal error: " + ex.Message });
            }
        }

        [HttpGet("history/{companyId}")]
        public async Task<IActionResult> GetOrderHistory(long companyId, [FromQuery] string? from = null, [FromQuery] string? to = null)
        {
            try
            {
                // Explicitly select columns to avoid ambiguity and handle potential NULLs
                string sql = @"
                    SELECT 
                        o.Id, o.CompanyId, o.UserId, o.BillNumber, o.TotalAmount, o.CreatedAt,
                        u.Name as UserName, 
                        r.RoleName
                    FROM orders o 
                    LEFT JOIN users u ON o.UserId = u.Id 
                    LEFT JOIN role r ON u.RoleId = r.Id
                    WHERE o.CompanyId = @1";

                var parameters = new List<string?> { companyId.ToString() };

                // Apply User Filter if not Admin
                if (!User.IsInRole("Admin"))
                {
                    var userId = User.FindFirst(System.Security.Claims.ClaimTypes.NameIdentifier)?.Value;
                    if (!string.IsNullOrEmpty(userId))
                    {
                        parameters.Add(userId);
                        sql += " AND o.UserId = @" + parameters.Count;
                    }
                }

                if (!string.IsNullOrEmpty(from))
                {
                    parameters.Add(from);
                    sql += " AND o.CreatedAt >= @" + parameters.Count;
                }
                if (!string.IsNullOrEmpty(to))
                {
                    parameters.Add(to);
                    sql += " AND o.CreatedAt <= @" + parameters.Count;
                }

                sql += " ORDER BY o.CreatedAt DESC LIMIT 50;";
                    
                DataTable? dt = await _dbHelper.GetDataTableWithParams(sql, parameters.ToArray());
                
                var orders = new List<OrderResponse>();
                if (dt != null && dt.Rows.Count > 0)
                {
                    // Fetch all relevant order items in one go for efficiency
                    var orderIds = new List<long>();
                    foreach (DataRow row in dt.Rows)
                    {
                        orderIds.Add(Convert.ToInt64(row["Id"]));
                    }

                    string itemSql = $"SELECT * FROM orderitems WHERE OrderId IN ({string.Join(",", orderIds)});";
                    DataTable? itemsDt = await _dbHelper.GetDataTable(itemSql);

                    // Group items by OrderId
                    var itemsByOrderId = new Dictionary<long, List<OrderItemResponse>>();
                    if (itemsDt != null)
                    {
                        foreach (DataRow itemRow in itemsDt.Rows)
                        {
                            long orderId = Convert.ToInt64(itemRow["OrderId"]);
                            if (!itemsByOrderId.ContainsKey(orderId)) 
                            {
                                itemsByOrderId[orderId] = new List<OrderItemResponse>();
                            }
                            itemsByOrderId[orderId].Add(new OrderItemResponse
                            {
                                Id = Convert.ToInt64(itemRow["Id"]),
                                OrderId = orderId,
                                ItemName = itemRow["ItemName"]?.ToString() ?? "",
                                Price = Convert.ToDecimal(itemRow["Price"]),
                                Quantity = Convert.ToInt32(itemRow["Quantity"])
                            });
                        }
                    }

                    foreach (DataRow row in dt.Rows)
                    {
                        long orderId = Convert.ToInt64(row["Id"]);
                        var order = new OrderResponse
                        {
                            Id = orderId,
                            CompanyId = Convert.ToInt64(row["CompanyId"]),
                            UserId = row["UserId"] != DBNull.Value ? Convert.ToInt64(row["UserId"]) : null,
                            UserName = row["UserName"] != DBNull.Value ? row["UserName"].ToString()! : "Admin",
                            UserRole = row["RoleName"] != DBNull.Value ? row["RoleName"].ToString()! : "Administrator",
                            BillNumber = row["BillNumber"]?.ToString() ?? "",
                            TotalAmount = Convert.ToDecimal(row["TotalAmount"]),
                            CreatedAt = row["CreatedAt"] != DBNull.Value ? Convert.ToDateTime(row["CreatedAt"]) : DateTime.MinValue,
                            Items = itemsByOrderId.ContainsKey(orderId) ? itemsByOrderId[orderId] : new List<OrderItemResponse>()
                        };
                        orders.Add(order);
                    }
                }
                return Ok(orders);
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Error fetching order history");
                Console.WriteLine($"ORDER HISTORY ERROR: {ex.Message} \n {ex.StackTrace}");
                await _dbHelper.InsertErrorLogAsync("OrderHistory", companyId.ToString(), ex.Message, ex.StackTrace, null, "User");
                return StatusCode(500, new { Success = false, Message = "Internal error: " + ex.Message });
            }
        }
    }
}
