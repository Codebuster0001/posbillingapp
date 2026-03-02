namespace posbillingapp.api.Models
{
    public class OrderRequest
    {
        public long CompanyId { get; set; }
        public long? UserId { get; set; }
        public string BillNumber { get; set; } = string.Empty;
        public decimal TotalAmount { get; set; }
        public List<OrderItemRequest> Items { get; set; } = new();
    }

    public class OrderItemRequest
    {
        public long MenuItemId { get; set; }
        public string ItemName { get; set; } = string.Empty;
        public decimal Price { get; set; }
        public int Quantity { get; set; }
    }

    public class OrderResponse
    {
        public long Id { get; set; }
        public long CompanyId { get; set; }
        public long? UserId { get; set; }
        public string UserName { get; set; } = string.Empty;
        public string UserRole { get; set; } = string.Empty;
        public string BillNumber { get; set; } = string.Empty;
        public decimal TotalAmount { get; set; }
        public DateTime CreatedAt { get; set; }
    }
}
