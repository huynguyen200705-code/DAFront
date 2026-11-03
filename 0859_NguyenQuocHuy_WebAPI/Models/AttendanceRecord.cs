using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace _0859_NguyenQuocHuy_WebAPI.Models;

[Table("attendance_records")]
public class AttendanceRecord
{
    [Key]
    [DatabaseGenerated(DatabaseGeneratedOption.Identity)]
    public long Id { get; set; }

    public long EmployeeId { get; set; }

    [Required]
    [MaxLength(150)]
    public string EmployeeName { get; set; } = string.Empty;

    [Required]
    [MaxLength(50)]
    public string Type { get; set; } = "CHECK_IN"; // CHECK_IN, CHECK_OUT

    [Required]
    [MaxLength(50)]
    public string Method { get; set; } = "NFC"; // NFC, QR_CODE, FACE_ID

    public long Timestamp { get; set; } = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();

    public double DurationHours { get; set; } = 0.0;

    [MaxLength(255)]
    public string? Note { get; set; } = string.Empty;

    [NotMapped]
    public string FormattedTime => DateTimeOffset.FromUnixTimeMilliseconds(Timestamp).ToLocalTime().ToString("HH:mm - dd/MM/yyyy");

    [NotMapped]
    public string FormattedHourOnly => DateTimeOffset.FromUnixTimeMilliseconds(Timestamp).ToLocalTime().ToString("HH:mm");
}
