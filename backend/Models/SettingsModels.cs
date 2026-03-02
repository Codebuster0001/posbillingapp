namespace posbillingapp.api.Models
{
    public class CompanyProfileUpdate
    {
        public string? CompanyName { get; set; }
        public string? PhoneNumber { get; set; }
        public int? CountryId { get; set; }
        public int? StateId { get; set; }
        public int? CityId { get; set; }
    }

    public class BankDetailsModel
    {
        public long Id { get; set; }
        public string? AccountHolderName { get; set; }
        public string? AccountNumber { get; set; }
        public string? IFSCCode { get; set; }
        public string? BankName { get; set; }
        public bool IsPrimary { get; set; }
        public string Status { get; set; } = "Pending"; // Pending, Verified, Mismatch, Failed
        public string? RegisteredName { get; set; }
        public int MatchScore { get; set; }
        public string? Branch { get; set; }
        public string? BankType { get; set; }
    }

    public class CardDetailsModel
    {
        public long Id { get; set; }
        public string? CardHolderName { get; set; }
        public string? CardNumber { get; set; } // Masked/Tokenized
        public string? CardType { get; set; } // Visa, Mastercard, etc.
        public int ExpiryMonth { get; set; }
        public int ExpiryYear { get; set; }
        public string Status { get; set; } = "Pending";
        public bool IsPrimary { get; set; }
        public DateTime CreatedAt { get; set; } = DateTime.UtcNow;
    }

    public class ValidationResult
    {
        public bool Success { get; set; }
        public bool Verified { get; set; }
        public string? RegisteredName { get; set; }
        public int MatchScore { get; set; }
        public string? BankName { get; set; }
        public string? Branch { get; set; }
        public string? BankType { get; set; }
        public string? Message { get; set; }
    }

    public class PaymentModeModel
    {
        public int Id { get; set; }
        public string Name { get; set; } = string.Empty;
        public string Type { get; set; } = string.Empty;
        public string? Provider { get; set; }
        public bool IsActive { get; set; }
    }

    public class StatusUpdateModel
    {
        public string Status { get; set; } = "Verified";
    }
}
