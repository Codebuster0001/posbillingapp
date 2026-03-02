using posbillingapp.api.Models;

namespace posbillingapp.api.Services
{
    public interface IBankValidationService
    {
        Task<ValidationResult> ValidateBankAsync(BankDetailsModel model, long companyId);
        string EncryptAccountNumber(string accountNumber);
        string DecryptAccountNumber(string encryptedAccountNumber);
    }

    public interface ICardValidationService
    {
        ValidationResult ValidateCard(string cardNumber, string? holderName, int expiryMonth, int expiryYear);
        string MaskCardNumber(string cardNumber);
    }
}
