using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace _0859_NguyenQuocHuy_WebAPI.Models;

[Table("menu_items")]
public class MenuItem
{
    [Key]
    [DatabaseGenerated(DatabaseGeneratedOption.Identity)]
    public long Id { get; set; }

    [Required]
    [MaxLength(150)]
    public string Name { get; set; } = string.Empty;

    [Required]
    [MaxLength(100)]
    public string Category { get; set; } = "Món chính"; // Khai vị, Món chính, Đồ uống, Tráng miệng

    public double Price { get; set; }

    [MaxLength(500)]
    public string? Description { get; set; } = string.Empty;

    public bool IsAvailable { get; set; } = true;
}
