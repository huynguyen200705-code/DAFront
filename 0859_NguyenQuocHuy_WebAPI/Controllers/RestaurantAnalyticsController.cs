using _0859_NguyenQuocHuy_WebAPI.Models;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;

namespace _0859_NguyenQuocHuy_WebAPI.Controllers;

[Route("api/analytics")]
[ApiController]
public class RestaurantAnalyticsController : ControllerBase
{
    private readonly ApplicationDbContext _context;

    public RestaurantAnalyticsController(ApplicationDbContext context)
    {
        _context = context;
    }

    [HttpGet("summary")]
    public async Task<IActionResult> GetSummary()
    {
        var now = DateTimeOffset.UtcNow;
        var startOfToday = new DateTimeOffset(now.Year, now.Month, now.Day, 0, 0, 0, TimeSpan.Zero).ToUnixTimeMilliseconds();

        var todayOrders = await _context.Orders
            .Where(o => o.Timestamp >= startOfToday)
            .ToListAsync();

        var todayRevenue = todayOrders.Sum(o => o.FinalTotal);
        var todayOrdersCount = todayOrders.Count;

        var totalTables = await _context.RestaurantTables.CountAsync();
        var activeTables = await _context.RestaurantTables.CountAsync(t => t.Status == "DANG_PHUC_VU");

        var totalEmployees = await _context.Employees.CountAsync();
        var totalCustomers = await _context.Customers.CountAsync();

        // Staff on duty: employees with last attendance today as CHECK_IN
        var recentCheckIns = await _context.AttendanceRecords
            .Where(a => a.Timestamp >= startOfToday)
            .GroupBy(a => a.EmployeeId)
            .Select(g => g.OrderByDescending(x => x.Timestamp).FirstOrDefault())
            .ToListAsync();

        var activeStaffCount = recentCheckIns.Count(r => r != null && r.Type == "CHECK_IN");

        var summary = new DashboardSummaryDto
        {
            TodayRevenue = todayRevenue,
            TodayOrdersCount = todayOrdersCount,
            ActiveTablesCount = activeTables,
            TotalTablesCount = totalTables,
            TotalEmployeesCount = totalEmployees,
            ActiveStaffOnDutyCount = activeStaffCount,
            TotalCustomersCount = totalCustomers
        };

        return Ok(new { success = true, data = summary });
    }

    [HttpGet("revenue-chart")]
    public async Task<IActionResult> GetRevenueChart([FromQuery] int days = 7)
    {
        var sinceTimestamp = DateTimeOffset.UtcNow.AddDays(-days).ToUnixTimeMilliseconds();

        var orders = await _context.Orders
            .Where(o => o.Timestamp >= sinceTimestamp)
            .ToListAsync();

        var grouped = orders
            .GroupBy(o => DateTimeOffset.FromUnixTimeMilliseconds(o.Timestamp).ToLocalTime().ToString("dd/MM"))
            .Select(g => new
            {
                date = g.Key,
                revenue = g.Sum(x => x.FinalTotal),
                orderCount = g.Count()
            })
            .ToList();

        return Ok(new { success = true, data = grouped });
    }

    [HttpGet("top-dishes")]
    public async Task<IActionResult> GetTopDishes([FromQuery] int limit = 5)
    {
        var topItems = await _context.OrderItems
            .GroupBy(i => new { i.MenuItemId, i.MenuItemName })
            .Select(g => new
            {
                menuItemId = g.Key.MenuItemId,
                name = g.Key.MenuItemName,
                totalQuantity = g.Sum(x => x.Quantity),
                totalRevenue = g.Sum(x => x.Price * x.Quantity)
            })
            .OrderByDescending(x => x.totalQuantity)
            .Take(limit)
            .ToListAsync();

        return Ok(new { success = true, data = topItems });
    }
}
