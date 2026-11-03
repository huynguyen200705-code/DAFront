using Microsoft.AspNetCore.SignalR;

namespace _0859_NguyenQuocHuy_WebAPI.Hubs;

public class NotificationHub : Hub
{
    private static int _connectedClients = 0;

    public override async Task OnConnectedAsync()
    {
        Interlocked.Increment(ref _connectedClients);
        await Clients.All.SendAsync("ClientCountUpdated", _connectedClients);
        await base.OnConnectedAsync();
    }

    public override async Task OnDisconnectedAsync(Exception? exception)
    {
        Interlocked.Decrement(ref _connectedClients);
        await Clients.All.SendAsync("ClientCountUpdated", _connectedClients);
        await base.OnDisconnectedAsync(exception);
    }

    /// <summary>
    /// Cho phép client phát thông báo tới tất cả client khác
    /// </summary>
    public async Task BroadcastNotification(string action, string message, string source = "Client")
    {
        await Clients.All.SendAsync("ReceiveNotification", new
        {
            action,
            description = message,
            source,
            timestamp = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds(),
            formattedTime = DateTime.Now.ToString("HH:mm:ss dd/MM/yyyy"),
            status = "INFO"
        });
    }
}

