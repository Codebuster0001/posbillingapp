using System.Text.Json;
using System.Text;
using System.Net.Http.Headers;

namespace posbillingapp.api.Services
{
    public class ResendEmailService : IEmailService
    {
        private readonly IConfiguration _config;
        private readonly ILogger<ResendEmailService> _logger;
        private readonly HttpClient _httpClient;

        public ResendEmailService(IConfiguration config, ILogger<ResendEmailService> logger)
        {
            _config = config;
            _logger = logger;
            _httpClient = new HttpClient();
        }

        public async Task<bool> SendOtpEmail(string toEmail, string otp)
        {
            var subject = "Your OTP for Password Reset - POS Billing App";
            var htmlBody = $@"
                <div style='font-family: Arial, sans-serif; max-width: 500px; margin: 0 auto; padding: 20px;'>
                    <h2 style='color: #333; text-align: center;'>Password Reset OTP</h2>
                    <p style='color: #666; font-size: 16px;'>You requested to reset your password. Use the OTP below:</p>
                    <div style='background: #f4f4f4; border-radius: 8px; padding: 20px; text-align: center; margin: 20px 0;'>
                        <span style='font-size: 32px; font-weight: bold; letter-spacing: 8px; color: #2196F3;'>{otp}</span>
                    </div>
                    <p style='color: #999; font-size: 14px;'>This OTP will expire in <strong>3 minutes</strong>.</p>
                    <p style='color: #999; font-size: 12px;'>If you did not request this, please ignore this email.</p>
                    <hr style='border: none; border-top: 1px solid #eee; margin: 20px 0;' />
                    <p style='color: #bbb; font-size: 11px; text-align: center;'>POS Billing App</p>
                </div>";

            return await SendEmailAsync(toEmail, subject, htmlBody);
        }

        public async Task<bool> SendEmailAsync(string toEmail, string subject, string htmlBody)
        {
            try
            {
                var apiKey = _config["Resend:ApiKey"];
                var senderEmail = _config["EmailSettings:SenderEmail"] ?? "onboarding@resend.dev";
                var senderName = _config["EmailSettings:SenderName"] ?? "POS Billing App";

                if (string.IsNullOrEmpty(apiKey))
                {
                    _logger.LogWarning("Resend API Key not configured. Email not sent.");
                    return false;
                }

                _logger.LogInformation("Attempting to send email via Resend API to {Email}", toEmail);

                var payload = new
                {
                    from = $"{senderName} <{senderEmail}>",
                    to = new[] { toEmail },
                    subject = subject,
                    html = htmlBody
                };

                var request = new HttpRequestMessage(HttpMethod.Post, "https://api.resend.com/emails");
                request.Headers.Authorization = new AuthenticationHeaderValue("Bearer", apiKey);
                request.Content = new StringContent(JsonSerializer.Serialize(payload), Encoding.UTF8, "application/json");

                var response = await _httpClient.SendAsync(request);

                if (response.IsSuccessStatusCode)
                {
                    _logger.LogInformation("Email sent successfully via Resend to {Email}", toEmail);
                    return true;
                }
                else
                {
                    var error = await response.Content.ReadAsStringAsync();
                    _logger.LogError("Failed to send email via Resend. Status: {Status}, Error: {Error}", response.StatusCode, error);
                    return false;
                }
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Exception while sending email via Resend to {Email}", toEmail);
                return false;
            }
        }
    }
}
