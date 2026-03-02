namespace posbillingapp.api.Models
{
    public class EmployeeAddRequest
    {
        [System.Text.Json.Serialization.JsonPropertyName("companyId")]
        public long CompanyId { get; set; }
        [System.Text.Json.Serialization.JsonPropertyName("name")]
        public string Name { get; set; } = string.Empty;
        [System.Text.Json.Serialization.JsonPropertyName("email")]
        public string Email { get; set; } = string.Empty;
        [System.Text.Json.Serialization.JsonPropertyName("phoneNumber")]
        public string PhoneNumber { get; set; } = string.Empty;
        [System.Text.Json.Serialization.JsonPropertyName("role")]
        public string Role { get; set; } = "Limited Access";
        [System.Text.Json.Serialization.JsonPropertyName("photoUrl")]
        public string? PhotoUrl { get; set; }
    }

    public class EmployeeUpdateRequest
    {
        [System.Text.Json.Serialization.JsonPropertyName("name")]
        public string Name { get; set; } = string.Empty;
        [System.Text.Json.Serialization.JsonPropertyName("email")]
        public string Email { get; set; } = string.Empty;
        [System.Text.Json.Serialization.JsonPropertyName("phoneNumber")]
        public string PhoneNumber { get; set; } = string.Empty;
        [System.Text.Json.Serialization.JsonPropertyName("role")]
        public string Role { get; set; } = "Limited Access";
        [System.Text.Json.Serialization.JsonPropertyName("isActive")]
        public bool IsActive { get; set; }
        [System.Text.Json.Serialization.JsonPropertyName("photoUrl")]
        public string? PhotoUrl { get; set; }
    }

    public class MenuItemRequest
    {
        public long CompanyId { get; set; }
        public string Name { get; set; } = string.Empty;
        public string Category { get; set; } = string.Empty;
        public decimal Price { get; set; }
        public string? Description { get; set; }
        public string? ImageUrl { get; set; }
        public bool IsAvailable { get; set; } = true;
    }

    public class CompanyProfileRequest
    {
        public string CompanyName { get; set; } = string.Empty;
        public string Email { get; set; } = string.Empty;
        public string? Address { get; set; }
        public string? LogoUrl { get; set; }
        public string? CurrencySymbol { get; set; }
    }

    public class EmployeeResponse
    {
        [System.Text.Json.Serialization.JsonPropertyName("id")]
        public long Id { get; set; }
        [System.Text.Json.Serialization.JsonPropertyName("name")]
        public string Name { get; set; } = string.Empty;
        [System.Text.Json.Serialization.JsonPropertyName("email")]
        public string Email { get; set; } = string.Empty;
        [System.Text.Json.Serialization.JsonPropertyName("phoneNumber")]
        public string PhoneNumber { get; set; } = string.Empty;
        [System.Text.Json.Serialization.JsonPropertyName("role")]
        public string Role { get; set; } = string.Empty;
        [System.Text.Json.Serialization.JsonPropertyName("photoUrl")]
        public string? PhotoUrl { get; set; }
        [System.Text.Json.Serialization.JsonPropertyName("isActive")]
        public bool IsActive { get; set; }
    }

    public class MenuItemResponse
    {
        public long Id { get; set; }
        public long CompanyId { get; set; }
        public string Name { get; set; } = string.Empty;
        public string Category { get; set; } = string.Empty;
        public decimal Price { get; set; }
        public string? Description { get; set; }
        public string? ImageUrl { get; set; }
        public bool IsAvailable { get; set; }
    }

    public class MenuAssignmentRequest
    {
        public long UserId { get; set; }
        public List<long> MenuItemIds { get; set; } = new List<long>();
    }
}
