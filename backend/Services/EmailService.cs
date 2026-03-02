using System.Net;
using System.Net.Mail;

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
            try
            {
                var smtpHost = _config["EmailSettings:SmtpHost"] ?? "smtp.gmail.com";
                var smtpPort = int.Parse(_config["EmailSettings:SmtpPort"] ?? "587");
                var senderEmail = _config["EmailSettings:SenderEmail"] ?? "";
                var senderPassword = _config["EmailSettings:SenderPassword"] ?? "";
                var senderName = _config["EmailSettings:SenderName"] ?? "POS Billing App";

                if (string.IsNullOrEmpty(senderEmail) || string.IsNullOrEmpty(senderPassword))
                {
                    _logger.LogWarning("Email settings not configured. OTP not sent via email.");
                    return false;
                }

                var message = new MailMessage();
                message.From = new MailAddress(senderEmail, senderName);
                message.To.Add(new MailAddress(toEmail));
                message.Subject = "Your OTP for Password Reset - POS Billing App";
                message.IsBodyHtml = true;
                message.Body = $@"
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

                _logger.LogInformation($"Attempting to send email via {smtpHost}:{smtpPort} for {toEmail}");

                using var client = new SmtpClient(smtpHost, smtpPort);
                client.EnableSsl = true;
                client.UseDefaultCredentials = false;
                client.Credentials = new NetworkCredential(senderEmail, senderPassword);
                client.DeliveryMethod = SmtpDeliveryMethod.Network;
                client.Timeout = 15000; // 15 seconds timeout for cloud network

                await client.SendMailAsync(message);
                _logger.LogInformation($"OTP email sent successfully to {toEmail}");
                return true;
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, $"Failed to send OTP email to {toEmail}");
                return false;
            }
        }

        public async Task<bool> SendEmailAsync(string toEmail, string subject, string htmlBody)
        {
            try
            {
                var smtpHost = _config["EmailSettings:SmtpHost"] ?? "smtp.gmail.com";
                var smtpPort = int.Parse(_config["EmailSettings:SmtpPort"] ?? "587");
                var senderEmail = _config["EmailSettings:SenderEmail"] ?? "";
                var senderPassword = _config["EmailSettings:SenderPassword"] ?? "";
                var senderName = _config["EmailSettings:SenderName"] ?? "POS Billing App";

                if (string.IsNullOrEmpty(senderEmail) || string.IsNullOrEmpty(senderPassword))
                {
                    _logger.LogWarning("Email settings not configured. Email not sent.");
                    return false;
                }

                var message = new MailMessage();
                message.From = new MailAddress(senderEmail, senderName);
                message.To.Add(new MailAddress(toEmail));
                message.Subject = subject;
                message.IsBodyHtml = true;
                message.Body = htmlBody;

                using var client = new SmtpClient(smtpHost, smtpPort);
                client.EnableSsl = true;
                client.Credentials = new NetworkCredential(senderEmail, senderPassword);
                client.Timeout = 15000;

                await client.SendMailAsync(message);
                _logger.LogInformation("Email '{Subject}' sent to {Email}", subject, toEmail);
                return true;
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Failed to send email to {Email}", toEmail);
                return false;
            }
        }
    }
}
