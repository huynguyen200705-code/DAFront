using _0859_NguyenQuocHuy_WebAPI.Models;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using System.Diagnostics;
using System.Runtime.InteropServices;

namespace _0859_NguyenQuocHuy_WebAPI.Controllers;

[Route("api/app")]
[ApiController]
public class AppManagementController : ControllerBase
{
    private readonly ApplicationDbContext _dbContext;
    private readonly UserManager<User> _userManager;
    private readonly RoleManager<IdentityRole> _roleManager;
    private readonly ILogger<AppManagementController> _logger;
    private static readonly DateTime _serverStartTime = DateTime.UtcNow;

    public AppManagementController(
        ApplicationDbContext dbContext,
        UserManager<User> userManager,
        RoleManager<IdentityRole> roleManager,
        ILogger<AppManagementController> logger)
    {
        _dbContext = dbContext;
        _userManager = userManager;
        _roleManager = roleManager;
        _logger = logger;
    }

    /// <summary>
    /// Thống kê tổng quan toàn bộ hệ thống (Sản phẩm, Danh mục, Người dùng, Bàn ăn, Đơn hàng, Doanh thu, Nhân sự)
    /// </summary>
    [HttpGet("overview")]
    public async Task<IActionResult> GetSystemOverview()
    {
        try
        {
            // 1. Thống kê sản phẩm và danh mục
            var totalProducts = await _dbContext.Products.CountAsync();
            var totalCategories = await _dbContext.Categories.CountAsync();
            var unassignedProducts = await _dbContext.Products.CountAsync(p => p.CategoryId == null);

            // 2. Thống kê tài khoản Identity
            var totalUsers = await _userManager.Users.CountAsync();
            var adminUsers = await _userManager.GetUsersInRoleAsync("Admin");
            var regularUsers = await _userManager.GetUsersInRoleAsync("User");

            // 3. Thống kê Nhà hàng & POS
            var totalTables = await _dbContext.RestaurantTables.CountAsync();
            var activeTables = await _dbContext.RestaurantTables.CountAsync(t => t.Status == "DANG_PHUC_VU");
            var reservedTables = await _dbContext.RestaurantTables.CountAsync(t => t.Status == "DA_DAT");
            var availableTables = await _dbContext.RestaurantTables.CountAsync(t => t.Status == "TRONG");

            var totalMenuItems = await _dbContext.MenuItems.CountAsync();
            var totalEmployees = await _dbContext.Employees.CountAsync();
            var totalCustomers = await _dbContext.Customers.CountAsync();
            var totalOrders = await _dbContext.Orders.CountAsync();

            var totalRevenue = await _dbContext.Orders.SumAsync(o => (double?)o.FinalTotal) ?? 0.0;

            var now = DateTimeOffset.UtcNow;
            var startOfToday = new DateTimeOffset(now.Year, now.Month, now.Day, 0, 0, 0, TimeSpan.Zero).ToUnixTimeMilliseconds();

            var todayOrders = await _dbContext.Orders
                .Where(o => o.Timestamp >= startOfToday)
                .ToListAsync();

            var todayRevenue = todayOrders.Sum(o => o.FinalTotal);
            var todayOrdersCount = todayOrders.Count;

            // 4. Thông tin máy chủ
            var uptime = DateTime.UtcNow - _serverStartTime;

            return Ok(new
            {
                success = true,
                system = new
                {
                    status = "ONLINE",
                    name = "DA Restaurant API - Realtime Sync & POS",
                    version = "2.0.0",
                    framework = RuntimeInformation.FrameworkDescription,
                    os = RuntimeInformation.OSDescription,
                    serverTime = DateTime.UtcNow,
                    uptimeFormatted = $"{uptime.Days}d {uptime.Hours}h {uptime.Minutes}m {uptime.Seconds}s",
                    lanIps = GetLocalIpAddresses()
                },
                catalog = new
                {
                    totalProducts,
                    totalCategories,
                    unassignedProducts
                },
                users = new
                {
                    totalUsers,
                    adminCount = adminUsers.Count,
                    userCount = regularUsers.Count
                },
                restaurantPOS = new
                {
                    tables = new
                    {
                        total = totalTables,
                        available = availableTables,
                        active = activeTables,
                        reserved = reservedTables
                    },
                    menuItemsCount = totalMenuItems,
                    employeesCount = totalEmployees,
                    customersCount = totalCustomers,
                    orders = new
                    {
                        totalOrders,
                        totalRevenue,
                        todayOrdersCount,
                        todayRevenue
                    }
                }
            });
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Lỗi xảy ra khi lấy thống kê hệ thống");
            return StatusCode(StatusCodes.Status500InternalServerError, new { success = false, message = "Lỗi khi tổng hợp thống kê", error = ex.Message });
        }
    }

    /// <summary>
    /// Nạp dữ liệu mẫu nếu hệ thống bị trống (Yêu cầu vai trò Admin)
    /// </summary>
    [HttpPost("seed-demo")]
    [Authorize(Roles = "Admin")]
    public async Task<IActionResult> SeedDemoData()
    {
        try
        {
            await DatabaseSeeder.SeedAsync(_dbContext);
            return Ok(new { success = true, message = "Đã kiểm tra và nạp dữ liệu mẫu ban đầu thành công." });
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Lỗi nạp dữ liệu mẫu");
            return StatusCode(StatusCodes.Status500InternalServerError, new { success = false, message = "Không thể nạp dữ liệu", error = ex.Message });
        }
    }

    /// <summary>
    /// Xem toàn bộ cấu trúc CSDL và dữ liệu
    /// </summary>
    [HttpGet("db-tables")]
    public async Task<IActionResult> GetDatabaseSchema()
    {
        try
        {
            var dbType = _dbContext.Database.ProviderName?.Contains("Sqlite") == true ? "SQLite" : "SQL Server";
            var dbName = dbType == "SQLite" ? "RestaurantPOS.db" : "RestaurantPOS_DB";

            // Đếm số dòng từng bảng chính
            var tableStats = new List<object>
            {
                new { table = "AspNetUsers",        rows = await _dbContext.Users.CountAsync(),             description = "Bảng tài khoản người dùng (Identity)" },
                new { table = "AspNetRoles",        rows = await _dbContext.Roles.CountAsync(),             description = "Bảng vai trò phân quyền (Admin/User)" },
                new { table = "Categories",         rows = await _dbContext.Categories.CountAsync(),        description = "Danh mục sản phẩm" },
                new { table = "Products",           rows = await _dbContext.Products.CountAsync(),          description = "Sản phẩm (thực đơn)" },
                new { table = "restaurant_tables",  rows = await _dbContext.RestaurantTables.CountAsync(),  description = "Sơ đồ bàn ăn nhà hàng" },
                new { table = "menu_items",         rows = await _dbContext.MenuItems.CountAsync(),         description = "Món ăn trong thực đơn POS" },
                new { table = "orders",             rows = await _dbContext.Orders.CountAsync(),            description = "Đơn hàng & Hóa đơn POS" },
                new { table = "order_items",        rows = await _dbContext.OrderItems.CountAsync(),        description = "Chi tiết dòng món ăn trong đơn hàng" },
                new { table = "employees",          rows = await _dbContext.Employees.CountAsync(),         description = "Nhân viên" },
                new { table = "attendance_records", rows = await _dbContext.AttendanceRecords.CountAsync(), description = "Lịch sử chấm công NFC/QR" },
                new { table = "customers",          rows = await _dbContext.Customers.CountAsync(),         description = "Khách hàng tích điểm thành viên" },
                new { table = "restaurant_users",   rows = await _dbContext.RestaurantUsers.CountAsync(),   description = "Tài khoản đăng nhập POS nội bộ" },
                new { table = "app_activities",     rows = await _dbContext.AppActivities.CountAsync(),     description = "Nhật ký thông báo hoạt động từ App" },
            };

            // Lấy dữ liệu mẫu từng bảng (tối đa 5 dòng)
            var sampleData = new
            {
                users = await _dbContext.Users
                    .Select(u => new { u.Id, u.UserName, u.Email, u.PhoneNumber })
                    .Take(5).ToListAsync(),

                categories = await _dbContext.Categories
                    .Select(c => new { c.Id, c.Name, c.Description })
                    .Take(5).ToListAsync(),

                products = await _dbContext.Products
                    .Select(p => new { p.Id, p.Name, p.Price, p.Description, p.CategoryId })
                    .Take(5).ToListAsync(),

                tables = await _dbContext.RestaurantTables
                    .Select(t => new { t.Id, t.Name, Capacity = t.Seats, t.Status, t.Floor })
                    .Take(5).ToListAsync(),

                menuItems = await _dbContext.MenuItems
                    .Select(m => new { m.Id, m.Name, m.Price, m.Category, m.IsAvailable })
                    .Take(5).ToListAsync(),

                employees = await _dbContext.Employees
                    .Select(e => new { e.Id, e.Name, Role = e.Position, Department = e.Tier, e.Phone })
                    .Take(5).ToListAsync(),

                customers = await _dbContext.Customers
                    .Select(c => new { c.Id, c.Name, c.Phone, TotalSpent = c.TotalSpent, LoyaltyPoints = c.LoyaltyPoints })
                    .Take(5).ToListAsync(),

                orders = await _dbContext.Orders
                    .Select(o => new { o.Id, o.TableName, o.EmployeeName, o.FinalTotal, o.PaymentMethod, o.Status })
                    .Take(5).ToListAsync(),
            };

            var totalTables = tableStats.Count;
            var totalRows   = tableStats.Sum(t => (int)((dynamic)t).rows);

            return Ok(new
            {
                success     = true,
                title       = "📊 Cấu Trúc CSDL — DA Restaurant API & POS",
                database    = new { type = dbType, name = dbName, totalTables, totalRows },
                schema      = tableStats,
                sampleData
            });
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Lỗi khi lấy thông tin CSDL");
            return StatusCode(500, new { success = false, message = "Lỗi truy vấn CSDL", error = ex.Message });
        }
    }

    /// <summary>
    /// Xuất script SQL tạo bảng (schema)
    /// </summary>
    [HttpGet("sql-schema")]
    public IActionResult GetSqlSchema()
    {
        var schema = @"-- ============================================================
-- SQL SCHEMA — DA RESTAURANT API & POS
-- Framework: ASP.NET Core 10 + Entity Framework Core + SQLite
-- ============================================================

-- 1. BẢNG TÀI KHOẢN NGƯỜI DÙNG (ASP.NET Identity)
CREATE TABLE AspNetUsers (
    Id           TEXT PRIMARY KEY,
    UserName     TEXT NOT NULL UNIQUE,
    Email        TEXT,
    PhoneNumber  TEXT,
    Initials     TEXT,
    PasswordHash TEXT,
    NormalizedUserName TEXT,
    NormalizedEmail    TEXT,
    SecurityStamp      TEXT,
    ConcurrencyStamp   TEXT,
    EmailConfirmed     INTEGER DEFAULT 0,
    PhoneNumberConfirmed INTEGER DEFAULT 0,
    TwoFactorEnabled   INTEGER DEFAULT 0,
    LockoutEnabled     INTEGER DEFAULT 1,
    AccessFailedCount  INTEGER DEFAULT 0
);

-- 2. BẢNG VAI TRÒ PHÂN QUYỀN
CREATE TABLE AspNetRoles (
    Id               TEXT PRIMARY KEY,
    Name             TEXT NOT NULL,
    NormalizedName   TEXT,
    ConcurrencyStamp TEXT
);

-- 3. BẢNG LIÊN KẾT USER - ROLE
CREATE TABLE AspNetUserRoles (
    UserId TEXT NOT NULL REFERENCES AspNetUsers(Id) ON DELETE CASCADE,
    RoleId TEXT NOT NULL REFERENCES AspNetRoles(Id) ON DELETE CASCADE,
    PRIMARY KEY (UserId, RoleId)
);

-- 4. BẢNG DANH MỤC SẢN PHẨM
CREATE TABLE Categories (
    Id          INTEGER PRIMARY KEY AUTOINCREMENT,
    Name        TEXT NOT NULL,
    Description TEXT
);

-- 5. BẢNG SẢN PHẨM
CREATE TABLE Products (
    Id          INTEGER PRIMARY KEY AUTOINCREMENT,
    Name        TEXT    NOT NULL,
    Price       REAL    NOT NULL DEFAULT 0,
    Image       TEXT,
    Description TEXT,
    CategoryId  INTEGER REFERENCES Categories(Id) ON DELETE SET NULL
);

-- 6. BẢNG BÀN ĂN
CREATE TABLE restaurant_tables (
    Id        INTEGER PRIMARY KEY AUTOINCREMENT,
    Name      TEXT NOT NULL,
    Seats     INTEGER DEFAULT 4,
    Status    TEXT DEFAULT 'TRONG',
    Floor     TEXT DEFAULT 'Tầng 1',
    CurrentOrderId INTEGER
);

-- 7. BẢNG MÓN ĂN (THỰC ĐƠN POS)
CREATE TABLE menu_items (
    Id          INTEGER PRIMARY KEY AUTOINCREMENT,
    Name        TEXT NOT NULL,
    Price       REAL NOT NULL DEFAULT 0,
    Category    TEXT,
    ImageUrl    TEXT,
    Description TEXT,
    IsAvailable INTEGER DEFAULT 1
);

-- 8. BẢNG ĐƠN HÀNG / HÓA ĐƠN POS
CREATE TABLE orders (
    Id               INTEGER PRIMARY KEY AUTOINCREMENT,
    TableId          INTEGER,
    TableName        TEXT,
    CustomerId       INTEGER,
    CustomerName     TEXT,
    CustomerTierName TEXT,
    EmployeeId       INTEGER,
    EmployeeName     TEXT,
    Subtotal         REAL DEFAULT 0,
    DiscountPercent  REAL DEFAULT 0,
    FinalTotal       REAL DEFAULT 0,
    PaymentMethod    TEXT DEFAULT 'TIEN_MAT',
    Status           TEXT DEFAULT 'COMPLETED',
    Timestamp        INTEGER
);

-- 9. BẢNG CHI TIẾT ĐƠN HÀNG
CREATE TABLE order_items (
    Id           INTEGER PRIMARY KEY AUTOINCREMENT,
    OrderId      INTEGER NOT NULL REFERENCES orders(Id) ON DELETE CASCADE,
    MenuItemId   INTEGER,
    MenuItemName TEXT NOT NULL,
    Price        REAL NOT NULL DEFAULT 0,
    Quantity     INTEGER NOT NULL DEFAULT 1,
    Note         TEXT
);

-- 10. BẢNG NHÂN VIÊN
CREATE TABLE employees (
    Id           INTEGER PRIMARY KEY AUTOINCREMENT,
    Name         TEXT NOT NULL,
    Phone        TEXT,
    Position     TEXT,
    Tier         TEXT,
    ContractType TEXT,
    HourlyRate   REAL,
    MonthlySalary REAL
);

-- 11. BẢNG CHẤM CÔNG
CREATE TABLE attendance_records (
    Id            INTEGER PRIMARY KEY AUTOINCREMENT,
    EmployeeId    INTEGER NOT NULL,
    EmployeeName  TEXT,
    Type          TEXT,
    Method        TEXT,
    Timestamp     INTEGER,
    DurationHours REAL,
    Note          TEXT
);

-- 12. BẢNG KHÁCH HÀNG THÀNH VIÊN
CREATE TABLE customers (
    Id            INTEGER PRIMARY KEY AUTOINCREMENT,
    Name          TEXT NOT NULL,
    Phone         TEXT UNIQUE,
    Email         TEXT,
    TotalSpent    REAL DEFAULT 0,
    LoyaltyPoints INTEGER DEFAULT 0
);

-- 13. BẢNG NHẬT KÝ HOẠT ĐỘNG APP REAL-TIME
CREATE TABLE app_activities (
    Id            INTEGER PRIMARY KEY AUTOINCREMENT,
    Timestamp     INTEGER,
    FormattedTime TEXT,
    Source        TEXT,
    ClientIp      TEXT,
    Action        TEXT NOT NULL,
    Description   TEXT NOT NULL,
    Category      TEXT,
    Status        TEXT,
    PayloadJson   TEXT
);
";
        return Content(schema, "text/plain; charset=utf-8");
    }

    private static List<string> GetLocalIpAddresses()
    {
        try
        {
            var host = System.Net.Dns.GetHostName();
            return System.Net.Dns.GetHostAddresses(host)
                .Where(ip => ip.AddressFamily == System.Net.Sockets.AddressFamily.InterNetwork && !System.Net.IPAddress.IsLoopback(ip))
                .Select(ip => ip.ToString())
                .ToList();
        }
        catch
        {
            return new List<string> { "127.0.0.1" };
        }
    }
}
