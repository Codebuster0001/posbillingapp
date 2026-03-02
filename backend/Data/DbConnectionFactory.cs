using MySql.Data.MySqlClient;
using System.Data;

namespace posbillingapp.api.Data
{
    public class DbConnectionFactory
    {
        private readonly string _connectionString;

        public DbConnectionFactory(IConfiguration configuration)
        {
            _connectionString = configuration.GetConnectionString("DefaultConnection") 
                              ?? throw new ArgumentNullException(nameof(configuration), "DefaultConnection string is missing");
        }

        public MySqlConnection CreateConnection()
        {
            return new MySqlConnection(_connectionString);
        }
    }
}
