using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace _0859_NguyenQuocHuy_WebAPI.Models;

[Table("restaurant_tables")]
public class RestaurantTable
{
    [Key]
    [DatabaseGenerated(DatabaseGeneratedOption.Identity)]
    public long Id { get; set; }

    [Required]
    [MaxLength(100)]
    public string Name { get; set; } = string.Empty; // Bàn 101

    [Required]
    [MaxLength(100)]
    public string Floor { get; set; } = "Tầng 1"; // Tầng 1, Tầng 2, Phòng VIP

    public int Seats { get; set; } = 4;

    [NotMapped]
    public int Capacity
    {
        get => Seats;
        set => Seats = value;
    }

    [Required]
    [MaxLength(50)]
    public string Status { get; set; } = "TRONG"; // TRONG, DANG_PHUC_VU, DA_DAT

    public long? CurrentOrderId { get; set; }

    [NotMapped]
    public string QrCodeContent => $"REST-TABLE-{Id}-{Name.Replace(" ", "")}";

    [NotMapped]
    public string OrderUrl => $"https://goldenspoon.vn/order?tableId={Id}";
}
