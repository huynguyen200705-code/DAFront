using System.ComponentModel.DataAnnotations;

namespace _0859_NguyenQuocHuy_WebAPI.Models;

/// <summary>
/// Dữ liệu đầu vào đăng ký tài khoản mới — Đồng bộ đầy đủ với App Android
/// </summary>
public class RegistrationModel
{
    [Required(ErrorMessage = "Tên đăng nhập là bắt buộc")]
    [StringLength(50, MinimumLength = 3, ErrorMessage = "Tên đăng nhập phải từ 3 đến 50 ký tự")]
    public string Username { get; set; } = string.Empty;

    [Required(ErrorMessage = "Mật khẩu là bắt buộc")]
    [MinLength(3, ErrorMessage = "Mật khẩu phải chứa ít nhất 3 ký tự")]
    [DataType(DataType.Password)]
    public string Password { get; set; } = string.Empty;

    /// <summary>
    /// Họ và tên người dùng (từ App Android)
    /// </summary>
    public string? FullName { get; set; }

    /// <summary>
    /// Số điện thoại (từ App Android)
    /// </summary>
    public string? Phone { get; set; }

    /// <summary>
    /// Email (tùy chọn, tự động sinh nếu để trống: {username}@restaurant.com)
    /// </summary>
    [EmailAddress(ErrorMessage = "Định dạng email không hợp lệ")]
    public string? Email { get; set; }

    /// <summary>
    /// Dữ liệu nhận diện khuôn mặt Face ID từ App
    /// </summary>
    public string? FaceIdData { get; set; }

    /// <summary>
    /// Chữ cái viết tắt hiển thị avatar (tự động tạo từ FullName nếu để trống)
    /// </summary>
    [MaxLength(10, ErrorMessage = "Chữ cái viết tắt tối đa 10 ký tự")]
    public string? Initials { get; set; }

    /// <summary>
    /// Vai trò người dùng (ADMIN hoặc USER, mặc định là USER)
    /// </summary>
    public string? Role { get; set; } = "USER";
}
