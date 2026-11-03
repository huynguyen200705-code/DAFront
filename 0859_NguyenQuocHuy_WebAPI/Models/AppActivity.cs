using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace _0859_NguyenQuocHuy_WebAPI.Models;

[Table("app_activities")]
public class AppActivity
{
    [Key]
    [DatabaseGenerated(DatabaseGeneratedOption.Identity)]
    public long Id { get; set; }

    public long Timestamp { get; set; } = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();

    [MaxLength(50)]
    public string FormattedTime { get; set; } = DateTime.Now.ToString("HH:mm:ss dd/MM/yyyy");

    [MaxLength(100)]
    public string Source { get; set; } = "Mobile App"; // Android App, Flutter, Web POS, API Client

    [MaxLength(100)]
    public string ClientIp { get; set; } = "127.0.0.1";

    [Required]
    [MaxLength(100)]
    public string Action { get; set; } = string.Empty; // GỌI MÓN, THANH TOÁN, CHẤM CÔNG, ĐĂNG NHẬP, CẬP NHẬT BÀN...

    [Required]
    [MaxLength(500)]
    public string Description { get; set; } = string.Empty;

    [MaxLength(50)]
    public string Category { get; set; } = "General"; // Order, Table, Auth, Attendance, Menu, Customer, Sync, General

    [MaxLength(20)]
    public string Status { get; set; } = "SUCCESS"; // SUCCESS, INFO, WARNING, ERROR

    public string? PayloadJson { get; set; }
}

