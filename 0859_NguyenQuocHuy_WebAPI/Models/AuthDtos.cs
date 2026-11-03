using System.ComponentModel.DataAnnotations;

namespace _0859_NguyenQuocHuy_WebAPI.Models;

/// <summary>
/// DTO chứa thông tin chi tiết của người dùng trả về cho client/admin
/// </summary>
public class UserDto
{
    public string Id { get; set; } = string.Empty;
    public string UserName { get; set; } = string.Empty;
    public string? Email { get; set; }
    public string? PhoneNumber { get; set; }
    public string? Initials { get; set; }
    public IList<string> Roles { get; set; } = new List<string>();
}

/// <summary>
/// Dữ liệu yêu cầu đổi mật khẩu
/// </summary>
public class ChangePasswordModel
{
    [Required(ErrorMessage = "Mật khẩu hiện tại là bắt buộc")]
    public string CurrentPassword { get; set; } = string.Empty;

    [Required(ErrorMessage = "Mật khẩu mới là bắt buộc")]
    [MinLength(6, ErrorMessage = "Mật khẩu mới phải có ít nhất 6 ký tự")]
    public string NewPassword { get; set; } = string.Empty;

    [Compare(nameof(NewPassword), ErrorMessage = "Xác nhận mật khẩu mới không khớp")]
    public string ConfirmNewPassword { get; set; } = string.Empty;
}

/// <summary>
/// Dữ liệu cập nhật thông tin cá nhân
/// </summary>
public class UpdateProfileModel
{
    [EmailAddress(ErrorMessage = "Địa chỉ email không hợp lệ")]
    public string? Email { get; set; }

    [MaxLength(5, ErrorMessage = "Tên viết tắt tối đa 5 ký tự")]
    public string? Initials { get; set; }

    [Phone(ErrorMessage = "Số điện thoại không hợp lệ")]
    public string? PhoneNumber { get; set; }
}

/// <summary>
/// Dữ liệu gửi mã OTP cho tính năng Smart Auth
/// </summary>
public class SendOtpModel
{
    [Required(ErrorMessage = "Số điện thoại là bắt buộc")]
    [Phone(ErrorMessage = "Số điện thoại không hợp lệ")]
    public string PhoneNumber { get; set; } = string.Empty;
}

/// <summary>
/// Dữ liệu xác thực mã OTP Smart Auth
/// </summary>
public class VerifyOtpModel
{
    [Required(ErrorMessage = "Số điện thoại là bắt buộc")]
    public string PhoneNumber { get; set; } = string.Empty;

    [Required(ErrorMessage = "Mã OTP là bắt buộc")]
    [StringLength(6, MinimumLength = 6, ErrorMessage = "Mã OTP gồm 6 chữ số")]
    public string OtpCode { get; set; } = string.Empty;
}

/// <summary>
/// Dữ liệu cập nhật vai trò người dùng (Dành cho Admin)
/// </summary>
public class UpdateUserRoleModel
{
    [Required(ErrorMessage = "Vai trò không được để trống")]
    public string Role { get; set; } = string.Empty;
}
