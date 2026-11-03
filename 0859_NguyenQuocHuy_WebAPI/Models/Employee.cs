using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace _0859_NguyenQuocHuy_WebAPI.Models;

[Table("employees")]
public class Employee
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

    [Required]
    [MaxLength(100)]
    public string Position { get; set; } = string.Empty;

    [NotMapped]
    public string Role
    {
        get => Position;
        set => Position = value;
    }

    [Required]
    [MaxLength(50)]
    public string Tier { get; set; } = "CHINH_THUC"; // THU_VIEC, CHINH_THUC, QUAN_LY, GIAM_DOC

    [NotMapped]
    public string Department
    {
        get => Tier;
        set => Tier = value;
    }

    [Required]
    [MaxLength(50)]
    public string ContractType { get; set; } = "MONTHLY"; // HOURLY, MONTHLY

    public double HourlyRate { get; set; } = 25000.0;

    public double MonthlySalary { get; set; } = 8000000.0;

    public double WorkedHours { get; set; } = 0.0;

    public double WorkedDays { get; set; } = 0.0;

    public double Bonus { get; set; } = 0.0;

    public double Penalty { get; set; } = 0.0;

    [NotMapped]
    public double CalculatedSalary
    {
        get
        {
            var baseAmount = ContractType == "HOURLY"
                ? HourlyRate * WorkedHours
                : MonthlySalary * (WorkedDays / 26.0);
            var total = baseAmount + Bonus - Penalty;
            return total < 0 ? 0 : total;
        }
    }

    [NotMapped]
    public string NfcTagCode => $"NFC-EMP-{Id:D4}";

    [NotMapped]
    public string QrCodeContent => $"REST-EMP-{Id}-{(Phone.Length >= 4 ? Phone[^4..] : Phone)}";
}
