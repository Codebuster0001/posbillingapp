using Resend;

namespace posbillingapp.api.Services
{
    public class ResendEmailService : IEmailService
    {
        private readonly IConfiguration _config;
        private readonly ILogger<ResendEmailService> _logger;

        public ResendEmailService(IConfiguration config, ILogger<ResendEmailService> logger)
        {
            _config = config;
            _logger = logger;
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
                if (string.IsNullOrEmpty(apiKey))
                {
                    _logger.LogWarning("Resend API Key not configured. Email not sent.");
                    return false;
                }

                var senderEmail = _config["EmailSettings:SenderEmail"] ?? "onboarding@resend.dev";
                
                // Resend Free Tier Requirement: 
                // You MUST use onboarding@resend.dev until you verify a custom domain.
                if (senderEmail.Contains("@gmail.com") || senderEmail.Contains("@yahoo.com") || senderEmail.Contains("@outlook.com"))
                {
                    _logger.LogInformation("Public sender detected. Swapping to 'onboarding@resend.dev' for Resend compatibility.");
                    senderEmail = "onboarding@resend.dev";
                }

                var senderName = _config["EmailSettings:SenderName"] ?? "POS Billing App";

                _logger.LogInformation("Attempting to send email via Resend Library to {Email}", toEmail);

                // Use the Resend Library as requested
                IResend resend = ResendClient.Create(apiKey);

                var message = new EmailMessage()
                {
                    From = $"{senderName} <{senderEmail}>",
                    To = toEmail,
                    Subject = subject,
                    HtmlBody = htmlBody,
                };

                await resend.EmailSendAsync(message);
                
                _logger.LogInformation("Email sent successfully via Resend Library to {Email}", toEmail);
                return true;
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Exception while sending email via Resend Library to {Email}", toEmail);
                return false;
            }
        }
    }
}
