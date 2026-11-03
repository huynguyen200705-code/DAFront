using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace _0859_NguyenQuocHuy_WebAPI.Models;

[Table("active_table_orders")]
public class ActiveTableOrder
{
    [Key]
    [DatabaseGenerated(DatabaseGeneratedOption.Identity)]
    public long Id { get; set; }

    public long TableId { get; set; }

    public long MenuItemId { get; set; }

    [Required]
    [MaxLength(150)]
    public string MenuItemName { get; set; } = string.Empty;

    public double Price { get; set; }

    public int Quantity { get; set; } = 1;

    [MaxLength(255)]
    public string? Note { get; set; } = string.Empty;
}
