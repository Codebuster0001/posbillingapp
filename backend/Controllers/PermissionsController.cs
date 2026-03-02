using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Authorization;
using posbillingapp.api.Services;
using System.Data;

namespace posbillingapp.api.Controllers
{
    [Authorize(Roles = "Admin")]
    [ApiController]
    [Route("api/[controller]")]
    public class PermissionsController : ControllerBase
    {
        private readonly IPermissionService _permissionService;
        private readonly ILogger<PermissionsController> _logger;

        public PermissionsController(IPermissionService permissionService, ILogger<PermissionsController> logger)
        {
            _permissionService = permissionService;
            _logger = logger;
        }

        [HttpGet("all")]
        public async Task<IActionResult> GetAllPermissions()
        {
            try
            {
                var dt = await _permissionService.GetAllPermissions();
                var permissions = new List<object>();
                if (dt != null)
                {
                    foreach (DataRow row in dt.Rows)
                    {
                        permissions.Add(new
                        {
                            Id = row["Id"],
                            Key = row["PermissionKey"],
                            Description = row["Description"]
                        });
                    }
                }
                return Ok(permissions);
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Error fetching all permissions");
                return StatusCode(500, "Internal server error");
            }
        }

        [HttpGet("role/{roleId}/{companyId}")]
        public async Task<IActionResult> GetRolePermissions(string roleId, string companyId)
        {
            try
            {
                // Security check: Ensure Admin is only managing their own company
                var jwtCompanyId = User.FindFirst("CompanyId")?.Value;
                if (jwtCompanyId != companyId) return Forbid();

                var dt = await _permissionService.GetRolePermissions(roleId, companyId);
                var permissions = new List<string>();
                if (dt != null)
                {
                    foreach (DataRow row in dt.Rows)
                    {
                        permissions.Add(row["PermissionKey"].ToString()!);
                    }
                }
                return Ok(permissions);
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, $"Error fetching permissions for role {roleId} and company {companyId}");
                return StatusCode(500, "Internal server error");
            }
        }

        [HttpPost("role")]
        public async Task<IActionResult> UpdateRolePermissions([FromBody] UpdateRolePermissionsRequest request)
        {
            try
            {
                // Security check
                var jwtCompanyId = User.FindFirst("CompanyId")?.Value;
                if (jwtCompanyId != request.CompanyId.ToString()) return Forbid();

                var result = await _permissionService.UpdateRolePermissions(
                    request.RoleId.ToString(), 
                    request.CompanyId.ToString(), 
                    request.PermissionKeys
                );

                if (result) return Ok(new { Success = true, Message = "Permissions updated successfully" });
                return BadRequest(new { Success = false, Message = "Failed to update permissions" });
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Error updating role permissions");
                return StatusCode(500, "Internal server error");
            }
        }
    }

    public class UpdateRolePermissionsRequest
    {
        public int RoleId { get; set; }
        public long CompanyId { get; set; }
        public List<string> PermissionKeys { get; set; } = new List<string>();
    }
}
