using _0859_NguyenQuocHuy_WebAPI.Models;

namespace _0859_NguyenQuocHuy_WebAPI.Services;

public interface IAppNotificationService
{
    Task RecordActivityAsync(AppActivity activity);
    IEnumerable<AppActivity> GetRecentActivities(int limit = 50);
    Task ClearActivitiesAsync();
}

