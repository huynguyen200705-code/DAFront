using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace _0859_NguyenQuocHuy_WebAPI.Models;

[Table("customers")]
public class Customer
{
    [Key]
    [DatabaseGenerated(DatabaseGeneratedOption.Identity)]
    public long Id { get; set; }

    [Required]
    [MaxLength(150)]
    public string Name { get; set; } = string.Empty;

    [Required]
    [MaxLength(20)]
    public string Phone { get; set; } = string.Empty;

    [MaxLength(150)]
    public string? Email { get; set; } = string.Empty;

    public double TotalSpent { get; set; } = 0.0;

    public int LoyaltyPoints { get; set; } = 0;

    [NotMapped]
    public int TotalPoints
    {
        get => LoyaltyPoints;
        set => LoyaltyPoints = value;
    }

    [NotMapped]
    public string TierName => TierDisplayName;

    [NotMapped]
    public string Tier => TotalSpent switch
    {
        >= 50000000.0 => "KIM_CUONG",
        >= 30000000.0 => "BACH_KIM",
        >= 15000000.0 => "VANG",
        >= 5000000.0 => "BAC",
        _ => "DONG"
    };

    [NotMapped]
    public string TierDisplayName => TotalSpent switch
    {
        >= 50000000.0 => "Bậc Kim Cương",
        >= 30000000.0 => "Bậc Bạch Kim",
        >= 15000000.0 => "Bậc Vàng",
        >= 5000000.0 => "Bậc Bạc",
        _ => "Bậc Đồng"
    };

    [NotMapped]
    public double DiscountPercent => TotalSpent switch
    {
        >= 50000000.0 => 20.0,
        >= 30000000.0 => 15.0,
        >= 15000000.0 => 10.0,
        >= 5000000.0 => 5.0,
        _ => 0.0
    };
}
