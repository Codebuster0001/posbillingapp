using System.Text.RegularExpressions;
using posbillingapp.api.Models;

namespace posbillingapp.api.Services
{
    public class CardValidationService : ICardValidationService
    {
        private readonly ILogger<CardValidationService> _logger;

        public CardValidationService(ILogger<CardValidationService> logger)
        {
            _logger = logger;
        }

        public ValidationResult ValidateCard(string cardNumber, string? holderName, int expiryMonth, int expiryYear)
        {
            _logger.LogInformation("Validating Card Details");

            // 1. Basic cleaning
            string cleanNumber = Regex.Replace(cardNumber ?? "", @"\D", "");

            // 2. Luhn Check
            if (!LuhnCheck(cleanNumber))
            {
                return new ValidationResult { Verified = false, Message = "Invalid Card Number (Luhn Check Failed)." };
            }

            // 3. Expiry Validation
            if (expiryMonth < 1 || expiryMonth > 12)
            {
                return new ValidationResult { Verified = false, Message = "Invalid Expiry Month." };
            }

            var now = DateTime.UtcNow;
            var expiry = new DateTime(expiryYear, expiryMonth, 1).AddMonths(1).AddDays(-1);
            if (expiry < now)
            {
                return new ValidationResult { Verified = false, Message = "Card has expired." };
            }

            // 4. Card Type Detection
            string cardType = DetectCardType(cleanNumber);

            return new ValidationResult
            {
                Verified = true,
                BankType = cardType, // Using BankType field for Card Type
                Message = $"Verified {cardType} Card Successfully.",
                BankName = "Digital Card Network"
            };
        }

        public string MaskCardNumber(string cardNumber)
        {
            string clean = Regex.Replace(cardNumber ?? "", @"\D", "");
            if (clean.Length < 4) return clean;
            return "XXXX XXXX XXXX " + clean.Substring(clean.Length - 4);
        }

        private bool LuhnCheck(string cardNumber)
        {
            int sum = 0;
            bool alternate = false;
            for (int i = cardNumber.Length - 1; i >= 0; i--)
            {
                int n = int.Parse(cardNumber[i].ToString());
                if (alternate)
                {
                    n *= 2;
                    if (n > 9) n -= 9;
                }
                sum += n;
                alternate = !alternate;
            }
            return (sum % 10 == 0);
        }

        private string DetectCardType(string cardNumber)
        {
            if (cardNumber.StartsWith("4")) return "Visa";
            if (Regex.IsMatch(cardNumber, "^5[1-5]")) return "Mastercard";
            if (Regex.IsMatch(cardNumber, "^6[0-9]")) return "RuPay";
            if (cardNumber.StartsWith("34") || cardNumber.StartsWith("37")) return "Amex";
            return "Card";
        }
    }
}
