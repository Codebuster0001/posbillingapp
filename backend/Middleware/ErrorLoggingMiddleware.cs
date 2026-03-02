using System;
using System.Threading.Tasks;
using Microsoft.AspNetCore.Http;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.Logging;
using posbillingapp.api.Data;
using System.Collections.Generic;
using MySql.Data.MySqlClient;

namespace posbillingapp.api.Middleware
{
    public class ErrorLoggingMiddleware
    {
        private readonly RequestDelegate _next;
        private readonly ILogger<ErrorLoggingMiddleware> _logger;

        public ErrorLoggingMiddleware(RequestDelegate next, ILogger<ErrorLoggingMiddleware> logger)
        {
            _next = next;
            _logger = logger;
        }

        public async Task InvokeAsync(HttpContext context)
        {
            try
            {
                await _next(context);
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "An unhandled exception occurred.");
                await LogErrorToDatabase(context, ex);
                throw; // Re-throw to let the standard error handler handle the response
            }
        }

        private async Task LogErrorToDatabase(HttpContext context, Exception ex)
        {
            try
            {
                // Create a narrower scope to resolve scoped services like DbConnectionFactory
                using (var scope = context.RequestServices.CreateScope())
                {
                   var db = scope.ServiceProvider.GetRequiredService<DbHelper>();
                   
                   string userId = context.User?.FindFirst(System.Security.Claims.ClaimTypes.NameIdentifier)?.Value ?? "Anonymous";
                   string path = context.Request.Path;
                   string method = context.Request.Method;
                   string deviceInfo = context.Request.Headers["User-Agent"].ToString();

                   await db.InsertErrorLogAsync(
                       "UnhandledException",
                       $"{method} {path}",
                       ex.Message,
                       ex.StackTrace ?? "",
                       deviceInfo,
                       userId
                   );
                }
            }
            catch (Exception logEx)
            {
                _logger.LogError(logEx, "Failed to log error to database.");
            }
        }
    }
}
