using System.Collections.Concurrent;

namespace posbillingapp.api.Services
{
    public interface ITokenBlacklistService
    {
        void BlacklistToken(string token, DateTime expiry);
        bool IsTokenBlacklisted(string token);
    }

    public class TokenBlacklistService : ITokenBlacklistService
    {
        // Simple in-memory thread-safe blacklist
        // For production, use Redis or a DB table
        private static readonly ConcurrentDictionary<string, DateTime> _blacklistedTokens = new();

        public void BlacklistToken(string token, DateTime expiry)
        {
            _blacklistedTokens.TryAdd(token, expiry);
            
            // Clean up expired tokens from memory periodically
            CleanupExpiredTokens();
        }

        public bool IsTokenBlacklisted(string token)
        {
            if (_blacklistedTokens.TryGetValue(token, out var expiry))
            {
                if (DateTime.UtcNow < expiry)
                {
                    return true;
                }
                else
                {
                    _blacklistedTokens.TryRemove(token, out _);
                }
            }
            return false;
        }

        private void CleanupExpiredTokens()
        {
            var now = DateTime.UtcNow;
            foreach (var item in _blacklistedTokens)
            {
                if (item.Value < now)
                {
                    _blacklistedTokens.TryRemove(item.Key, out _);
                }
            }
        }
    }
}
