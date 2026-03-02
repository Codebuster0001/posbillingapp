using System;
using System.Data;
using System.IO;
using System.Threading.Tasks;
using Microsoft.Extensions.Logging;
using MySql.Data.MySqlClient;

namespace posbillingapp.api.Data
{
    public class DbHelper
    {
        private readonly DbConnectionFactory _connectionFactory;
        private readonly ILogger<DbHelper>? _logger;
        private static readonly string LogPath = Path.Combine(AppContext.BaseDirectory, "logs");

        public DbHelper(DbConnectionFactory connectionFactory, ILogger<DbHelper>? logger = null)
        {
            _connectionFactory = connectionFactory ?? throw new ArgumentNullException(nameof(connectionFactory));
            _logger = logger;
        }

        public async Task<DataTable?> GetDataTableWithParams(string query, string?[] parameters)
        {
            var dt = new DataTable();
            await using var con = _connectionFactory.CreateConnection();
            await con.OpenAsync();

            await using var cmd = new MySqlCommand(query, con);
            for (int i = 0; i < parameters.Length; i++)
            {
                cmd.Parameters.AddWithValue("@" + (i + 1), parameters[i] ?? (object)DBNull.Value);
            }

            await using var reader = await cmd.ExecuteReaderAsync();
            dt.Load(reader);
            return dt;
        }

        public async Task<DataTable?> GetDataTable(string query)
        {
            var dt = new DataTable();
            await using var con = _connectionFactory.CreateConnection();
            await con.OpenAsync();

            await using var cmd = new MySqlCommand(query, con);
            await using var reader = await cmd.ExecuteReaderAsync();
            dt.Load(reader);
            return dt;
        }

        public async Task<int> ExecuteQueryWithParams(string query, string?[] parameters)
        {
            if (string.IsNullOrWhiteSpace(query))
            {
                throw new ArgumentException("Query must not be null or empty.", nameof(query));
            }

            await using var con = _connectionFactory.CreateConnection();
            await con.OpenAsync();

            await using var cmd = new MySqlCommand(query, con);
            for (int i = 0; i < parameters.Length; i++)
            {
                cmd.Parameters.AddWithValue("@" + (i + 1), parameters[i] ?? (object)DBNull.Value);
            }

            var affected = await cmd.ExecuteNonQueryAsync();
            return affected;
        }

        public async Task<long> ExecuteQueryWithParamsTrn(string sql, string?[] parameters)
        {
            if (string.IsNullOrWhiteSpace(sql))
                throw new ArgumentException("SQL must not be null or empty.", nameof(sql));

            await using var con = _connectionFactory.CreateConnection();
            await con.OpenAsync();

            await using var cmd = new MySqlCommand(sql, con);
            for (int i = 0; i < parameters.Length; i++)
            {
                cmd.Parameters.AddWithValue("@" + (i + 1), parameters[i] ?? (object)DBNull.Value);
            }

            await cmd.ExecuteNonQueryAsync();
            return cmd.LastInsertedId;
        }

        public async Task<bool> InsertErrorLogAsync(
            string? logType,
            string? referenceId,
            string? errorMessage,
            string? stackTrace,
            string? deviceInfo,
            string? createdBy)
        {
            try
            {
                const string sql = @"
INSERT INTO error_logs
  (log_type, reference_id, error_message, stack_trace, device_info, created_by, DOC)
VALUES
  (@log_type, @reference_id, @error_message, @stack_trace, @device_info, @created_by, @doc);";

                await using var con = _connectionFactory.CreateConnection();
                await con.OpenAsync();

                await using var cmd = new MySqlCommand(sql, con);
                cmd.Parameters.AddWithValue("@log_type", (object?)logType ?? DBNull.Value);
                cmd.Parameters.AddWithValue("@reference_id", (object?)referenceId ?? DBNull.Value);
                cmd.Parameters.AddWithValue("@error_message", (object?)errorMessage ?? DBNull.Value);
                cmd.Parameters.AddWithValue("@stack_trace", (object?)stackTrace ?? DBNull.Value);
                cmd.Parameters.AddWithValue("@device_info", (object?)deviceInfo ?? DBNull.Value);
                cmd.Parameters.AddWithValue("@created_by", (object?)createdBy ?? DBNull.Value);
                cmd.Parameters.AddWithValue("@doc", DateTime.UtcNow);

                var rows = await cmd.ExecuteNonQueryAsync();
                return rows > 0;
            }
            catch (Exception ex)
            {
                await LogToFileAsync($"DB LOG FAILED: {ex.Message}. ORIGINAL ERROR: {errorMessage}. STACK: {stackTrace}");
                return false;
            }
        }

        private async Task LogToFileAsync(string message)
        {
            try
            {
                Directory.CreateDirectory(LogPath);
                var path = Path.Combine(LogPath, $"dbhelper_{DateTime.UtcNow:yyyyMMdd}.log");
                var line = $"{DateTime.UtcNow:O} {message}{Environment.NewLine}";
                await File.AppendAllTextAsync(path, line);
            }
            catch
            {
                // Avoid throwing from the logger.
            }
        }
    }
}
