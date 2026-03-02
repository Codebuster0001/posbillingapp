using MailKit.Net.Smtp;
using MailKit.Security;
using MimeKit;
using System.Net;

namespace posbillingapp.api.Services
{
    public interface IEmailService
    {
        Task<bool> SendOtpEmail(string toEmail, string otp);
        Task<bool> SendEmailAsync(string toEmail, string subject, string htmlBody);
    }

    public class EmailService : IEmailService
    {
        private readonly IConfiguration _config;
        private readonly ILogger<EmailService> _logger;

        public EmailService(IConfiguration config, ILogger<EmailService> logger)
        {
            _config = config;
            _logger = logger;
        }

        public async Task<bool> SendOtpEmail(string toEmail, string otp)
        {
            var subject = "Your OTP for Password Reset - POS Billing App";
            var htmlBody = $@"
                <div style='font-family: Arial, sans-serif; max-width: 500px; margin: 0 auto; padding: 20px; border: 1px solid #eee; border-radius: 10px;'>
                    <div style='text-align: center; margin-bottom: 20px;'>
                        <h2 style='color: #2196F3;'>Password Reset OTP</h2>
                    </div>
                    <p style='color: #666; font-size: 16px;'>You requested to reset your password. Use the following OTP to proceed:</p>
                    <div style='background: #f4f4f4; border-radius: 8px; padding: 20px; text-align: center; margin: 20px 0;'>
                        <span style='font-size: 32px; font-weight: bold; letter-spacing: 5px; color: #333;'>{otp}</span>
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
            var email = new MimeMessage();
            
            var senderEmail = _config["EmailSettings:SenderEmail"] ?? "";
            var senderPassword = _config["EmailSettings:SenderPassword"] ?? "";
            var senderName = _config["EmailSettings:SenderName"] ?? "POS Billing App";
            var smtpHost = _config["EmailSettings:SmtpHost"] ?? "smtp.gmail.com";
            var smtpPortStr = _config["EmailSettings:SmtpPort"] ?? "465";
            int smtpPort = int.TryParse(smtpPortStr, out int port) ? port : 465;

            if (string.IsNullOrEmpty(senderEmail) || string.IsNullOrEmpty(senderPassword))
            {
                _logger.LogWarning("Email settings not configured. Email not sent.");
                return false;
            }

            try
            {
                email.From.Add(new MailboxAddress(senderName, senderEmail));
                email.To.Add(new MailboxAddress("", toEmail));
                email.Subject = subject;

                var bodyBuilder = new BodyBuilder { HtmlBody = htmlBody };
                email.Body = bodyBuilder.ToMessageBody();

                using var client = new SmtpClient();
                client.Timeout = 15000; // 15 seconds timeout
                
                // For Railway/Cloud:
                // Port 465 -> SslOnConnect (Implicit SSL)
                // Port 587 -> StartTls (Explicit SSL)
                var socketOptions = SecureSocketOptions.Auto;
                if (smtpPort == 465) socketOptions = SecureSocketOptions.SslOnConnect;
                else if (smtpPort == 587) socketOptions = SecureSocketOptions.StartTls;

                _logger.LogInformation($"Stepping 1: Connecting to {smtpHost}:{smtpPort}...");
                await client.ConnectAsync(smtpHost, smtpPort, socketOptions);
                
                _logger.LogInformation($"Stepping 2: Authenticating as {senderEmail}...");
                await client.AuthenticateAsync(senderEmail, senderPassword);
                
                _logger.LogInformation($"Stepping 3: Sending email contents...");
                await client.SendAsync(email);
                
                _logger.LogInformation($"Stepping 4: Disconnecting...");
                await client.DisconnectAsync(true);

                _logger.LogInformation($"SUCCESS: OTP email sent to {toEmail}!");
                return true;
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, $"CRITICAL: Gmail SMTP Failed for {toEmail}. Error: {ex.Message}");
                return false;
            }
        }
    }
}
