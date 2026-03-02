using System.Security.Cryptography;
using System.Text;

namespace posbillingapp.api.Services
{
    public interface IPasswordService
    {
        string HashPassword(string password, out string salt);
        bool VerifyPassword(string password, string hash, string salt);
    }

    public class PasswordService : IPasswordService
    {
        const int keySize = 64;
        const int iterations = 350000;
        HashAlgorithmName hashAlgorithm = HashAlgorithmName.SHA512;

        public string HashPassword(string password, out string saltString)
        {
            var salt = RandomNumberGenerator.GetBytes(keySize);
            saltString = Convert.ToHexString(salt);

            var hash = Rfc2898DeriveBytes.Pbkdf2(
                Encoding.UTF8.GetBytes(password),
                salt,
                iterations,
                hashAlgorithm,
                keySize);

            return Convert.ToHexString(hash);
        }

        public bool VerifyPassword(string password, string hash, string saltString)
        {
            var salt = Convert.FromHexString(saltString);
            var hashToCompare = Rfc2898DeriveBytes.Pbkdf2(
                Encoding.UTF8.GetBytes(password),
                salt,
                iterations,
                hashAlgorithm,
                keySize);

            return CryptographicOperations.FixedTimeEquals(hashToCompare, Convert.FromHexString(hash));
        }
    }
}
