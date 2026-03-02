using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using posbillingapp.api.Data;
using System.Security.Claims;

namespace posbillingapp.api.Controllers
{
    [Route("api/[controller]")]
    [ApiController]
    public class ErrorLogsController : ControllerBase
    {
        private readonly DbHelper _dbHelper;

        public ErrorLogsController(DbHelper dbHelper)
        {
            _dbHelper = dbHelper;
        }

        [HttpPost]
        public async Task<IActionResult> LogError([FromBody] ErrorLogRequest request)
        {
            if (request == null)
                return BadRequest("Invalid request");

            string userId = User.FindFirst(ClaimTypes.NameIdentifier)?.Value ?? "Anonymous";
            if (string.IsNullOrEmpty(request.CreatedBy)) {
                request.CreatedBy = userId;
            }

            var success = await _dbHelper.InsertErrorLogAsync(
                request.LogType ?? "ClientError",
                request.ReferenceId ?? "App",
                request.ErrorMessage,
                request.StackTrace,
                request.DeviceInfo,
                request.CreatedBy
            );

            if (success)
                return Ok(new { success = true, message = "Error logged" });
            
            return StatusCode(500, new { success = false, message = "Failed to log error" });
        }
    }

    public class ErrorLogRequest
    {
        public string? LogType { get; set; }
        public string? ReferenceId { get; set; }
        public string? ErrorMessage { get; set; }
        public string? StackTrace { get; set; }
        public string? DeviceInfo { get; set; } // e.g., "Android 12 - Samsung S21"
        public string? CreatedBy { get; set; }
    }
}
