using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace _0859_NguyenQuocHuy_WebAPI.Models;

[Table("orders")]
public class Order
{
    [Key]
    [DatabaseGenerated(DatabaseGeneratedOption.Identity)]
    public long Id { get; set; }

    public long TableId { get; set; }

    [Required]
    [MaxLength(100)]
    public string TableName { get; set; } = string.Empty;

    public long? CustomerId { get; set; }

    [MaxLength(150)]
    public string CustomerName { get; set; } = "Khách vãng lai";

    [MaxLength(100)]
    public string CustomerTierName { get; set; } = "Bậc Đồng";

    public long EmployeeId { get; set; }

    [MaxLength(150)]
    public string EmployeeName { get; set; } = "Nhân viên";

    public double Subtotal { get; set; } = 0.0;

    public double DiscountPercent { get; set; } = 0.0;

    [MaxLength(100)]
    public string PaymentMethod { get; set; } = "Tiền mặt"; // Tiền mặt, VietQR

    [MaxLength(50)]
    public string Status { get; set; } = "COMPLETED"; // PENDING, COMPLETED

    public long Timestamp { get; set; } = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();

    [NotMapped]
    public double DiscountAmount => Subtotal * (DiscountPercent / 100.0);

    [NotMapped]
    public double FinalTotal => Subtotal - DiscountAmount;

    public virtual List<OrderItem> Items { get; set; } = new();
}

[Table("order_items")]
public class OrderItem
{
    [Key]
    [DatabaseGenerated(DatabaseGeneratedOption.Identity)]
    public long Id { get; set; }

    public long OrderId { get; set; }

    public long MenuItemId { get; set; }

    [Required]
    [MaxLength(150)]
    public string MenuItemName { get; set; } = string.Empty;

    public double Price { get; set; }

    public int Quantity { get; set; } = 1;

    [MaxLength(255)]
    public string? Note { get; set; } = string.Empty;

    [NotMapped]
    public double TotalPrice => Price * Quantity;
}
