using _0859_NguyenQuocHuy_WebAPI.Models;
using _0859_NguyenQuocHuy_WebAPI.Services;

namespace _0859_NguyenQuocHuy_WebAPI.Middleware;

public class AppActivityMiddleware
{
    private readonly RequestDelegate _next;

    public AppActivityMiddleware(RequestDelegate next)
    {
        _next = next;
    }

    public async Task InvokeAsync(HttpContext context, IAppNotificationService notificationService)
    {
        var path = context.Request.Path.Value ?? "";
        var method = context.Request.Method;

        // Bỏ qua các file tĩnh, swagger, hub connection, hoặc websocket handshake nội bộ
        if (ShouldIgnore(path))
        {
            await _next(context);
            return;
        }

        // Thực thi request tiếp theo
        await _next(context);

        // Sau khi request hoàn tất, ghi nhận hoạt động nếu là API call có ý nghĩa
        if (path.StartsWith("/api/", StringComparison.OrdinalIgnoreCase))
        {
            var statusCode = context.Response.StatusCode;
            var isSuccess = statusCode >= 200 && statusCode < 400;

            // Chỉ ghi nhận: tất cả POST/PUT/DELETE, hoặc các GET quan trọng (như sync dữ liệu)
            if (method != "GET" || path.Contains("/sync/", StringComparison.OrdinalIgnoreCase))
            {
                var clientIp = GetClientIp(context);
                var source = DetectClientSource(context);
                var (action, description, category) = ParseActionAndDescription(method, path, isSuccess);

                var activity = new AppActivity
                {
                    Timestamp = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds(),
                    FormattedTime = DateTime.Now.ToString("HH:mm:ss dd/MM/yyyy"),
                    Source = source,
                    ClientIp = clientIp,
                    Action = action,
                    Description = description,
                    Category = category,
                    Status = isSuccess ? "SUCCESS" : "ERROR",
                    PayloadJson = $"{method} {path} (Mã phản hồi: {statusCode})"
                };

                await notificationService.RecordActivityAsync(activity);
            }
        }
    }

    private static bool ShouldIgnore(string path)
    {
        if (path == "/" ||
            path.StartsWith("/swagger", StringComparison.OrdinalIgnoreCase) ||
            path.StartsWith("/hubs", StringComparison.OrdinalIgnoreCase) ||
            path.EndsWith(".html", StringComparison.OrdinalIgnoreCase) ||
            path.EndsWith(".css", StringComparison.OrdinalIgnoreCase) ||
            path.EndsWith(".js", StringComparison.OrdinalIgnoreCase) ||
            path.EndsWith(".ico", StringComparison.OrdinalIgnoreCase) ||
            path.EndsWith(".png", StringComparison.OrdinalIgnoreCase) ||
            path.EndsWith(".jpg", StringComparison.OrdinalIgnoreCase) ||
            path.Equals("/api/notifications/recent", StringComparison.OrdinalIgnoreCase) ||
            path.Equals("/api/sync/ping", StringComparison.OrdinalIgnoreCase))
        {
            return true;
        }

        return false;
    }

    private static string GetClientIp(HttpContext context)
    {
        if (context.Request.Headers.TryGetValue("X-Forwarded-For", out var forwardedFor) && !string.IsNullOrWhiteSpace(forwardedFor))
        {
            var ip = forwardedFor.ToString().Split(',')[0].Trim();
            if (!string.IsNullOrEmpty(ip)) return ip;
        }

        return context.Connection.RemoteIpAddress?.ToString() ?? "127.0.0.1";
    }

    private static string DetectClientSource(HttpContext context)
    {
        var userAgent = context.Request.Headers.UserAgent.ToString().ToLowerInvariant();

        if (context.Request.Headers.TryGetValue("X-Client-App", out var customHeader) && !string.IsNullOrWhiteSpace(customHeader))
        {
            return customHeader.ToString();
        }

        if (userAgent.Contains("dart") || userAgent.Contains("flutter"))
            return "📱 Flutter App";
        if (userAgent.Contains("android"))
            return "📱 Android App";
        if (userAgent.Contains("iphone") || userAgent.Contains("ipad") || userAgent.Contains("darwin"))
            return "📱 iOS App";
        if (userAgent.Contains("okhttp"))
            return "📱 Android Mobile (OkHttp)";
        if (userAgent.Contains("postman"))
            return "🧪 Postman Client";

        return "💻 Thiết bị kết nối";
    }

    private static (string action, string description, string category) ParseActionAndDescription(string method, string path, bool isSuccess)
    {
        var p = path.ToLowerInvariant();

        if (p.Contains("/orders/checkout"))
            return ("POS - THANH TOÁN", isSuccess ? "Khách đã thanh toán đơn hàng thành công từ app" : "Lỗi khi xử lý thanh toán đơn hàng", "Order");

        if (p.Contains("/orders") && method == "POST")
            return ("POS - GỌI MÓN", isSuccess ? "Bàn vừa tạo đơn hàng / gọi món mới qua app" : "Gọi món thất bại", "Order");

        if (p.Contains("/tables") && p.Contains("/status"))
            return ("BÀN ĂN - ĐỔI TRẠNG THÁI", isSuccess ? "Trạng thái bàn ăn vừa được cập nhật từ thiết bị" : "Cập nhật trạng thái bàn thất bại", "Table");

        if (p.Contains("/tables") && (method == "POST" || method == "PUT"))
            return ("BÀN ĂN - CẬP NHẬT", isSuccess ? "Sơ đồ bàn ăn vừa được chỉnh sửa từ app" : "Lỗi thao tác bàn ăn", "Table");

        if (p.Contains("/attendance/check-in"))
            return ("CHẤM CÔNG - VÀO CA", isSuccess ? "Nhân viên vừa check-in vào ca làm việc" : "Lỗi chấm công vào ca", "Attendance");

        if (p.Contains("/attendance/check-out"))
            return ("CHẤM CÔNG - RA CA", isSuccess ? "Nhân viên vừa check-out hết ca làm việc" : "Lỗi chấm công hết ca", "Attendance");

        if (p.Contains("/restaurantauth/login") || p.Contains("/authenticate/login"))
            return ("TÀI KHOẢN - ĐĂNG NHẬP", isSuccess ? "Tài khoản vừa đăng nhập thành công vào hệ thống" : "Đăng nhập thất bại (sai mật khẩu)", "Auth");

        if (p.Contains("/authenticate/register"))
            return ("TÀI KHOẢN - ĐĂNG KÝ", isSuccess ? "Có tài khoản mới vừa đăng ký thành công" : "Đăng ký tài khoản thất bại", "Auth");

        if (p.Contains("/menu") && method == "POST")
            return ("THỰC ĐƠN - THÊM MÓN", isSuccess ? "Đã thêm món ăn mới vào thực đơn" : "Lỗi thêm món ăn", "Menu");

        if (p.Contains("/menu") && method == "PUT")
            return ("THỰC ĐƠN - SỬA MÓN", isSuccess ? "Thông tin món ăn đã được chỉnh sửa" : "Lỗi chỉnh sửa món ăn", "Menu");

        if (p.Contains("/menu") && method == "DELETE")
            return ("THỰC ĐƠN - XÓA MÓN", isSuccess ? "Món ăn đã được xóa khỏi thực đơn" : "Lỗi xóa món ăn", "Menu");

        if (p.Contains("/customers") && method == "POST")
            return ("KHÁCH HÀNG - THÊM MỚI", isSuccess ? "Hồ sơ khách hàng mới vừa được tạo" : "Lỗi thêm khách hàng", "Customer");

        if (p.Contains("/sync/all"))
            return ("ĐỒNG BỘ - TẢI DỮ LIỆU", isSuccess ? "Ứng dụng di động vừa đồng bộ toàn bộ dữ liệu từ máy chủ" : "Đồng bộ thất bại", "Sync");

        // Mặc định
        return ($"{method} {path}", isSuccess ? $"Thao tác thành công trên {path}" : $"Thao tác thất bại ({method} {path})", "General");
    }
}
