using Microsoft.AspNetCore.Authentication.JwtBearer;
using Microsoft.IdentityModel.Tokens;
using System.Net.Http.Headers;
using System.Text;

var builder = WebApplication.CreateBuilder(args);

// Add services to the container.
builder.Services.AddControllers();
builder.Services.AddCors(options => {
    options.AddPolicy("AllowAll", builder => builder.AllowAnyOrigin().AllowAnyMethod().AllowAnyHeader());
});

builder.Services.AddScoped<posbillingapp.api.Data.DbConnectionFactory>();
builder.Services.AddScoped<posbillingapp.api.Data.DbHelper>();
builder.Services.AddScoped<posbillingapp.api.Services.IPasswordService, posbillingapp.api.Services.PasswordService>();
builder.Services.AddScoped<posbillingapp.api.Services.IEmailService, posbillingapp.api.Services.EmailService>();
builder.Services.AddScoped<posbillingapp.api.Services.IJwtService, posbillingapp.api.Services.JwtService>();
builder.Services.AddScoped<posbillingapp.api.Services.IPermissionService, posbillingapp.api.Services.PermissionService>();
builder.Services.AddScoped<posbillingapp.api.Services.IBankValidationService, posbillingapp.api.Services.BankValidationService>();
builder.Services.AddScoped<posbillingapp.api.Services.ICardValidationService, posbillingapp.api.Services.CardValidationService>();
builder.Services.AddScoped<posbillingapp.api.Services.ISettingsService, posbillingapp.api.Services.SettingsService>();
builder.Services.AddSingleton<posbillingapp.api.Services.ITokenBlacklistService, posbillingapp.api.Services.TokenBlacklistService>();



// JWT Authentication Configuration
var jwtSettings = builder.Configuration.GetSection("Jwt");
var key = Encoding.ASCII.GetBytes(jwtSettings["Key"] ?? throw new InvalidOperationException("JWT Key is missing"));

builder.Services.AddAuthentication(options =>
{
    options.DefaultAuthenticateScheme = JwtBearerDefaults.AuthenticationScheme;
    options.DefaultChallengeScheme = JwtBearerDefaults.AuthenticationScheme;
})
.AddJwtBearer(options =>
{
    options.TokenValidationParameters = new TokenValidationParameters
    {
        ValidateIssuer = false, // Relaxed for debugging
        ValidateAudience = false, // Relaxed for debugging
        ValidateLifetime = true,
        ValidateIssuerSigningKey = true,
        ValidIssuer = jwtSettings["Issuer"],
        ValidAudience = jwtSettings["Audience"],
        IssuerSigningKey = new SymmetricSecurityKey(key),
        ClockSkew = TimeSpan.Zero,
        RoleClaimType = System.Security.Claims.ClaimTypes.Role,
        NameClaimType = System.Security.Claims.ClaimTypes.NameIdentifier
    };
    
    options.Events = new JwtBearerEvents
    {
        OnAuthenticationFailed = context =>
        {
            var logger = context.HttpContext.RequestServices.GetRequiredService<ILogger<Program>>();
            logger.LogError($"Authentication failed: {context.Exception.Message}");
            return Task.CompletedTask;
        },
        OnTokenValidated = context =>
        {
            var blacklistService = context.HttpContext.RequestServices.GetRequiredService<posbillingapp.api.Services.ITokenBlacklistService>();
            var authHeader = context.Request.Headers["Authorization"].ToString();
            if (authHeader.StartsWith("Bearer ", StringComparison.OrdinalIgnoreCase))
            {
                var token = authHeader.Substring(7);
                if (blacklistService.IsTokenBlacklisted(token))
                {
                    context.Fail("This token has been revoked.");
                }
            }
            return Task.CompletedTask;
        }
    };
});

var app = builder.Build();

app.UseCors("AllowAll");

// DB Initialization
using (var scope = app.Services.CreateScope())
{
    var permissionService = scope.ServiceProvider.GetRequiredService<posbillingapp.api.Services.IPermissionService>();
    var settingsService = scope.ServiceProvider.GetRequiredService<posbillingapp.api.Services.ISettingsService>();
    try {
        await permissionService.EnsureTablesCreated();
        await permissionService.SeedPermissions();
        await settingsService.EnsureTablesCreated();
    } catch (Exception ex) {
        var logger = scope.ServiceProvider.GetRequiredService<ILogger<Program>>();
        logger.LogError(ex, "Error during DB initialization");
    }
}

// Configure the HTTP request pipeline.
if (app.Environment.IsDevelopment())
{
    app.UseDeveloperExceptionPage();
}

// app.UseHttpsRedirection();
app.UseMiddleware<posbillingapp.api.Middleware.ErrorLoggingMiddleware>();
app.UseAuthentication();
app.UseAuthorization();

app.UseStaticFiles();
app.MapControllers();


app.MapGet("/check", () => "Backend Connected!");


app.Run();


