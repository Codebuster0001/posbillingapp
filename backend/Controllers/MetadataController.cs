using Microsoft.AspNetCore.Mvc;
using posbillingapp.api.Data;
using System.Data;

namespace posbillingapp.api.Controllers
{
    [ApiController]
    [Route("api/[controller]")]
    public class MetadataController : ControllerBase
    {
        private readonly DbHelper _dbHelper;

        public MetadataController(DbHelper dbHelper)
        {
            _dbHelper = dbHelper;
        }

        [HttpGet("countries")]
        public async Task<IActionResult> GetCountries()
        {
            var dt = await _dbHelper.GetDataTable("SELECT Id, CountryName FROM countries;");
            if (dt == null) return Ok(new List<object>());

            var list = new List<object>();
            foreach (DataRow row in dt.Rows)
            {
                list.Add(new
                {
                    Id = Convert.ToInt32(row["Id"]),
                    CountryName = row["CountryName"].ToString()
                });
            }
            return Ok(list);
        }

        [HttpGet("states/{countryId}")]
        public async Task<IActionResult> GetStates(int countryId)
        {
            var dt = await _dbHelper.GetDataTableWithParams(
                "SELECT Id, StateName FROM states WHERE CountryId = @1;",
                new[] { countryId.ToString() });
            if (dt == null) return Ok(new List<object>());

            var list = new List<object>();
            foreach (DataRow row in dt.Rows)
            {
                list.Add(new
                {
                    Id = Convert.ToInt32(row["Id"]),
                    StateName = row["StateName"].ToString()
                });
            }
            return Ok(list);
        }

        [HttpGet("cities/{stateId}")]
        public async Task<IActionResult> GetCities(int stateId)
        {
            var dt = await _dbHelper.GetDataTableWithParams(
                "SELECT Id, CityName FROM cities WHERE StateId = @1;",
                new[] { stateId.ToString() });
            if (dt == null) return Ok(new List<object>());

            var list = new List<object>();
            foreach (DataRow row in dt.Rows)
            {
                list.Add(new
                {
                    Id = Convert.ToInt32(row["Id"]),
                    CityName = row["CityName"].ToString()
                });
            }
            return Ok(list);
        }
        [HttpGet("roles")]
        public async Task<IActionResult> GetRoles()
        {
            var dt = await _dbHelper.GetDataTable("SELECT Id, RoleName FROM role;");
            if (dt == null) return Ok(new List<object>());

            var list = new List<object>();
            foreach (DataRow row in dt.Rows)
            {
                list.Add(new
                {
                    Id = Convert.ToInt32(row["Id"]),
                    RoleName = row["RoleName"].ToString()
                });
            }
            return Ok(list);
        }
        [HttpGet("columns/{tableName}")]
        public async Task<IActionResult> GetColumns(string tableName)
        {
            var dt = await _dbHelper.GetDataTable($"SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = '{tableName}' AND TABLE_SCHEMA = 'pos_db2';");
            if (dt == null) return Ok(new List<string>());

            var list = new List<string>();
            foreach (DataRow row in dt.Rows)
            {
                list.Add(row["COLUMN_NAME"].ToString() ?? "");
            }
            return Ok(list);
        }
        [HttpGet("count/{tableName}")]
        public async Task<IActionResult> GetCount(string tableName)
        {
            var dt = await _dbHelper.GetDataTable($"SELECT COUNT(*) FROM {tableName};");
            if (dt == null || dt.Rows.Count == 0) return Ok(0);
            return Ok(dt.Rows[0][0]);
        }
        [HttpGet("emails")]
        public async Task<IActionResult> GetEmails()
        {
            var dt = await _dbHelper.GetDataTable("SELECT Email FROM companies;");
            if (dt == null) return Ok(new List<string>());
            var list = new List<string>();
            foreach (DataRow row in dt.Rows) list.Add(row["Email"].ToString() ?? "");
            return Ok(list);
        }
        [HttpGet("companies")]
        public async Task<IActionResult> GetCompanies()
        {
            var dt = await _dbHelper.GetDataTable("SELECT Id, CompanyName FROM companies;");
            if (dt == null) return Ok(new List<object>());
            var list = new List<object>();
            foreach (DataRow row in dt.Rows)
            {
                list.Add(new { Id = Convert.ToInt64(row["Id"]), Name = row["CompanyName"].ToString() });
            }
            return Ok(list);
        }
    }
}
