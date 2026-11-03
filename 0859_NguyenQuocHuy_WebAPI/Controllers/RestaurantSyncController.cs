using _0859_NguyenQuocHuy_WebAPI.Models;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;

namespace _0859_NguyenQuocHuy_WebAPI.Controllers;

[Route("api/sync")]
[ApiController]
public class RestaurantSyncController : ControllerBase
{
    private readonly ApplicationDbContext _context;

    public RestaurantSyncController(ApplicationDbContext context)
    {
        _context = context;
    }

    /// <summary>
    /// Đồng bộ toàn bộ dữ liệu máy chủ về cho ứng dụng Android Demo
    /// </summary>
    [HttpGet("all")]
    public async Task<IActionResult> GetAllDataForSync()
    {
        var tables = await _context.RestaurantTables.ToListAsync();
        var menuItems = await _context.MenuItems.ToListAsync();
        var employees = await _context.Employees.ToListAsync();
        var customers = await _context.Customers.ToListAsync();
        var activeOrders = await _context.ActiveTableOrders.ToListAsync();
        var attendance = await _context.AttendanceRecords.OrderByDescending(a => a.Timestamp).Take(30).ToListAsync();

        return Ok(new
        {
            success = true,
            syncedAt = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds(),
            tables,
            menuItems,
            employees,
            customers,
            activeOrders,
            attendance
        });
    }

    /// <summary>
    /// Endpoint kiểm tra trạng thái kết nối song song giữa Android App và Web API
    /// </summary>
    [HttpGet("ping")]
    public IActionResult Ping()
    {
        return Ok(new
        {
            success = true,
            status = "ONLINE",
            service = "Restaurant Management & POS Web API",
            timestamp = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds(),
            clientIp = HttpContext.Connection.RemoteIpAddress?.ToString()
        });
    }
}
