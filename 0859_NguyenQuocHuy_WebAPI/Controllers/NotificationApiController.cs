using _0859_NguyenQuocHuy_WebAPI.Models;
using _0859_NguyenQuocHuy_WebAPI.Services;
using Microsoft.AspNetCore.Mvc;

namespace _0859_NguyenQuocHuy_WebAPI.Controllers;

[Route("api/notifications")]
[ApiController]
public class NotificationApiController : ControllerBase
{
    private readonly IAppNotificationService _notificationService;

    public NotificationApiController(IAppNotificationService notificationService)
    {
        _notificationService = notificationService;
    }

    /// <summary>
    /// Lấy danh sách thông báo và hoạt động gần nhất từ các app kết nối
    /// </summary>
    [HttpGet("recent")]
    public IActionResult GetRecentNotifications([FromQuery] int limit = 50)
    {
        var activities = _notificationService.GetRecentActivities(limit);
        return Ok(new
        {
            success = true,
            total = activities.Count(),
            data = activities
        });
    }

    /// <summary>
    /// Cho phép Mobile App (Flutter/Android) chủ động gửi thông báo hoạt động lên Web API
    /// </summary>
    [HttpPost("log")]
    public async Task<IActionResult> LogNotification([FromBody] CustomNotificationRequest request)
    {
        if (string.IsNullOrWhiteSpace(request.Action) || string.IsNullOrWhiteSpace(request.Description))
        {
            return BadRequest(new { success = false, message = "Action và Description không được để trống." });
        }

        var ip = HttpContext.Connection.RemoteIpAddress?.ToString() ?? "127.0.0.1";

        var activity = new AppActivity
        {
            Timestamp = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds(),
            FormattedTime = DateTime.Now.ToString("HH:mm:ss dd/MM/yyyy"),
            Source = string.IsNullOrWhiteSpace(request.Source) ? "📱 Mobile App" : request.Source,
            ClientIp = ip,
            Action = request.Action,
            Description = request.Description,
            Category = string.IsNullOrWhiteSpace(request.Category) ? "General" : request.Category,
            Status = string.IsNullOrWhiteSpace(request.Status) ? "INFO" : request.Status,
            PayloadJson = request.PayloadJson
        };

        await _notificationService.RecordActivityAsync(activity);

        return Ok(new
        {
            success = true,
            message = "Thông báo hoạt động đã được phát lên Web API thành công!",
            data = activity
        });
    }

    /// <summary>
    /// Phát thông báo thử nghiệm để kiểm tra tính năng Real-time trên Web API
    /// </summary>
    [HttpPost("test")]
    public async Task<IActionResult> TestNotification([FromQuery] string? action, [FromQuery] string? message)
    {
        var activity = new AppActivity
        {
            Timestamp = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds(),
            FormattedTime = DateTime.Now.ToString("HH:mm:ss dd/MM/yyyy"),
            Source = "🧪 Thử Nghiệm Hệ Thống",
            ClientIp = HttpContext.Connection.RemoteIpAddress?.ToString() ?? "127.0.0.1",
            Action = string.IsNullOrWhiteSpace(action) ? "GỌI MÓN MỚI" : action,
            Description = string.IsNullOrWhiteSpace(message) ? "Bàn VIP 1 vừa gọi: 2 Bò Wagyu Nướng, 1 Lẩu Hải Sản Thái (Tổng tiền: 650,000 đ)" : message,
            Category = "Order",
            Status = "SUCCESS"
        };

        await _notificationService.RecordActivityAsync(activity);

        return Ok(new
        {
            success = true,
            message = "Đã phát thông báo mẫu thành công! Hãy kiểm tra cửa sổ Console và trang Web API.",
            data = activity
        });
    }

    /// <summary>
    /// Xóa lịch sử thông báo
    /// </summary>
    [HttpDelete("clear")]
    public async Task<IActionResult> ClearNotifications()
    {
        await _notificationService.ClearActivitiesAsync();
        return Ok(new { success = true, message = "Đã dọn dẹp toàn bộ lịch sử thông báo." });
    }
}

public class CustomNotificationRequest
{
    public string? Source { get; set; } = "📱 Mobile App";
    public string Action { get; set; } = string.Empty;
    public string Description { get; set; } = string.Empty;
    public string? Category { get; set; } = "General";
    public string? Status { get; set; } = "INFO";
    public string? PayloadJson { get; set; }
}

