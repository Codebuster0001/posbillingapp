using System.Text.RegularExpressions;
using posbillingapp.api.Models;
using Microsoft.Extensions.Logging;
using System.Threading.Tasks;

namespace posbillingapp.api.Services
{
    public interface IBankVerificationService
    {
        Task<VerificationResult> VerifyAccountAsync(BankDetailsModel model);
    }

    public class VerificationResult
    {
        public bool Success { get; set; }
        public string? RegisteredName { get; set; }
        public string? BankName { get; set; }
        public string? Branch { get; set; }
        public string? BankType { get; set; } // Savings / Current
        public string Message { get; set; } = string.Empty;
    }

    public class BankVerificationService : IBankVerificationService
    {
        private readonly ILogger<BankVerificationService> _logger;

        public BankVerificationService(ILogger<BankVerificationService> logger)
        {
            _logger = logger;
        }

        public async Task<VerificationResult> VerifyAccountAsync(BankDetailsModel model)
        {
            _logger.LogInformation($"Starting simulated penny drop for account: {model.AccountNumber}, IFSC: {model.IFSCCode}");

            // 1. Artificial Delay to simulate API call (Razorpay style)
            await Task.Delay(2000);

            // 2. Basic Validation Rules
            if (string.IsNullOrWhiteSpace(model.AccountNumber) || model.AccountNumber.Length < 9)
            {
                return new VerificationResult { Success = false, Message = "Invalid Account Number format" };
            }

            if (string.IsNullOrWhiteSpace(model.IFSCCode) || !Regex.IsMatch(model.IFSCCode, @"^[A-Z]{4}0[A-Z0-9]{6}$"))
            {
                return new VerificationResult { Success = false, Message = "Invalid IFSC Code format" };
            }

            // 3. Simulated "Penny Drop" Success Logic
            // In a real system, you'd call Razorpay/Cashfree here.
            // For simulation, we'll succeed unless the account number is "000000000"
            if (model.AccountNumber == "000000000")
            {
                return new VerificationResult { Success = false, Message = "Account verification failed at bank" };
            }

            // Return a simulated registered name based on the account holder name
            var registeredName = model.AccountHolderName?.ToUpper() ?? "UNKNOWN OWNER";
            if (!registeredName.Contains("M/S") && registeredName.Length > 5)
            {
                registeredName = "M/S " + registeredName;
            }

            // Simulate Bank Discovery Details
            var bankName = model.BankName ?? GetSimulatedBankName(model.IFSCCode);
            var branch = "MAIN BRANCH, NEW DELHI";
            var bankType = model.AccountNumber?.Length > 12 ? "Current Account" : "Savings Account";

            _logger.LogInformation($"Verification successful. Registered Name: {registeredName}, Bank: {bankName}");

            return new VerificationResult
            {
                Success = true,
                RegisteredName = registeredName,
                BankName = bankName,
                Branch = branch,
                BankType = bankType,
                Message = "Account discovered and verified successfully"
            };
        }

        private string GetSimulatedBankName(string ifsc)
        {
            if (ifsc.StartsWith("SBIN")) return "State Bank of India";
            if (ifsc.StartsWith("HDFC")) return "HDFC Bank";
            if (ifsc.StartsWith("ICIC")) return "ICICI Bank";
            if (ifsc.StartsWith("KKBK")) return "Kotak Mahindra Bank";
            if (ifsc.StartsWith("BARB")) return "Bank of Baroda";
            return "Commercial Bank of India";
        }
    }
}
