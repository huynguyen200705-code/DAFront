using _0859_NguyenQuocHuy_WebAPI.Hubs;
using _0859_NguyenQuocHuy_WebAPI.Models;
using Microsoft.AspNetCore.SignalR;
using System.Collections.Concurrent;

namespace _0859_NguyenQuocHuy_WebAPI.Services;

public class AppNotificationService : IAppNotificationService
{
    private static readonly ConcurrentQueue<AppActivity> _memoryQueue = new();
    private const int MaxMemoryItems = 150;
    private readonly IHubContext<NotificationHub> _hubContext;
    private readonly IServiceProvider _serviceProvider;
    private readonly ILogger<AppNotificationService> _logger;

    public AppNotificationService(
        IHubContext<NotificationHub> hubContext,
        IServiceProvider serviceProvider,
        ILogger<AppNotificationService> logger)
    {
        _hubContext = hubContext;
        _serviceProvider = serviceProvider;
        _logger = logger;
    }

    public async Task RecordActivityAsync(AppActivity activity)
    {
        // 1. Lưu vào hàng đợi bộ nhớ tạm (cho UI truy vấn nhanh)
        _memoryQueue.Enqueue(activity);
        while (_memoryQueue.Count > MaxMemoryItems && _memoryQueue.TryDequeue(out _)) { }

        // 2. Hiển thị thông báo nổi bật trên Console của Web API
        LogToConsole(activity);

        // 3. Phát thông báo thời gian thực (Real-time Push) qua SignalR tới Web Admin và các client
        try
        {
            await _hubContext.Clients.All.SendAsync("ReceiveNotification", new
            {
                id = activity.Id,
                timestamp = activity.Timestamp,
                formattedTime = activity.FormattedTime,
                source = activity.Source,
                clientIp = activity.ClientIp,
                action = activity.Action,
                description = activity.Description,
                category = activity.Category,
                status = activity.Status,
                payloadJson = activity.PayloadJson
            });
        }
        catch (Exception ex)
        {
            _logger.LogWarning("Không thể gửi thông báo SignalR: {Message}", ex.Message);
        }

        // 4. Lưu vào Database SQLite trong background task
        _ = Task.Run(async () =>
        {
            try
            {
                using var scope = _serviceProvider.CreateScope();
                var db = scope.ServiceProvider.GetRequiredService<ApplicationDbContext>();
                db.AppActivities.Add(activity);
                await db.SaveChangesAsync();
            }
            catch (Exception ex)
            {
                _logger.LogWarning("Không thể lưu hoạt động vào CSDL: {Message}", ex.Message);
            }
        });
    }

    public IEnumerable<AppActivity> GetRecentActivities(int limit = 50)
    {
        return _memoryQueue.Reverse().Take(limit).ToList();
    }

    public async Task ClearActivitiesAsync()
    {
        while (_memoryQueue.TryDequeue(out _)) { }

        try
        {
            using var scope = _serviceProvider.CreateScope();
            var db = scope.ServiceProvider.GetRequiredService<ApplicationDbContext>();
            db.AppActivities.RemoveRange(db.AppActivities);
            await db.SaveChangesAsync();
        }
        catch (Exception ex)
        {
            _logger.LogWarning("Lỗi khi xóa lịch sử hoạt động: {Message}", ex.Message);
        }
    }

    private static void LogToConsole(AppActivity activity)
    {
        var originalColor = Console.ForegroundColor;
        try
        {
            Console.ForegroundColor = ConsoleColor.DarkGray;
            Console.Write($"[{activity.FormattedTime}] ");

            Console.ForegroundColor = activity.Status switch
            {
                "SUCCESS" => ConsoleColor.Green,
                "WARNING" => ConsoleColor.Yellow,
                "ERROR" => ConsoleColor.Red,
                _ => ConsoleColor.Cyan
            };
            Console.Write($"🔔 [THÔNG BÁO HOẠT ĐỘNG APP: {activity.Action}] ");

            Console.ForegroundColor = ConsoleColor.Magenta;
            Console.Write($"[{activity.Source} | IP: {activity.ClientIp}] ");

            Console.ForegroundColor = ConsoleColor.White;
            Console.WriteLine(activity.Description);
        }
        finally
        {
            Console.ForegroundColor = originalColor;
        }
    }
}

