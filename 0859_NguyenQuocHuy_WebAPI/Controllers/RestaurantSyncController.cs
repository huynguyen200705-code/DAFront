using _0859_NguyenQuocHuy_WebAPI.Models;
using _0859_NguyenQuocHuy_WebAPI.Services;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using System.Text;

namespace _0859_NguyenQuocHuy_WebAPI.Controllers;

[Route("api/sync")]
[ApiController]
public class RestaurantSyncController : ControllerBase
{
    private readonly ApplicationDbContext _context;
    private readonly IAppNotificationService _notificationService;
    private readonly IConfiguration _configuration;

    public RestaurantSyncController(
        ApplicationDbContext context,
        IAppNotificationService notificationService,
        IConfiguration configuration)
    {
        _context = context;
        _notificationService = notificationService;
        _configuration = configuration;
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
        var attendance = await _context.AttendanceRecords.OrderByDescending(a => a.Timestamp).Take(50).ToListAsync();
        var orders = await _context.Orders.Include(o => o.Items).OrderByDescending(o => o.Timestamp).Take(30).ToListAsync();

        return Ok(new
        {
            success = true,
            provider = _context.Database.ProviderName,
            syncedAt = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds(),
            tables,
            menuItems,
            employees,
            customers,
            activeOrders,
            attendance,
            orders
        });
    }

    /// <summary>
    /// Tiếp nhận và lưu toàn bộ thông tin từ thiết bị vào CSDL SQL
    /// </summary>
    [HttpPost("upload")]
    [HttpPost("save-all")]
    public async Task<IActionResult> SaveAllData([FromBody] SyncUploadRequest request)
    {
        if (request == null)
        {
            return BadRequest(new { success = false, message = "Dữ liệu gửi lên không hợp lệ." });
        }

        using var transaction = await _context.Database.BeginTransactionAsync();
        try
        {
            int tablesAdded = 0, menuAdded = 0, empAdded = 0, custAdded = 0, attAdded = 0, ordersAdded = 0;

            // 1. Lưu thông tin Bàn ăn
            if (request.Tables != null && request.Tables.Any())
            {
                foreach (var table in request.Tables)
                {
                    var existing = await _context.RestaurantTables.FindAsync(table.Id);
                    if (existing == null)
                    {
                        _context.RestaurantTables.Add(new RestaurantTable
                        {
                            Name = table.Name,
                            Floor = table.Floor,
                            Seats = table.Seats,
                            Status = table.Status,
                            CurrentOrderId = table.CurrentOrderId
                        });
                        tablesAdded++;
                    }
                    else
                    {
                        existing.Name = table.Name;
                        existing.Floor = table.Floor;
                        existing.Seats = table.Seats;
                        existing.Status = table.Status;
                        existing.CurrentOrderId = table.CurrentOrderId;
                    }
                }
            }

            // 2. Lưu thông tin Món ăn
            if (request.MenuItems != null && request.MenuItems.Any())
            {
                foreach (var item in request.MenuItems)
                {
                    var existing = await _context.MenuItems.FindAsync(item.Id);
                    if (existing == null)
                    {
                        _context.MenuItems.Add(new MenuItem
                        {
                            Name = item.Name,
                            Category = item.Category,
                            Price = item.Price,
                            Description = item.Description,
                            IsAvailable = item.IsAvailable
                        });
                        menuAdded++;
                    }
                    else
                    {
                        existing.Name = item.Name;
                        existing.Category = item.Category;
                        existing.Price = item.Price;
                        existing.Description = item.Description;
                        existing.IsAvailable = item.IsAvailable;
                    }
                }
            }

            // 3. Lưu thông tin Khách hàng
            if (request.Customers != null && request.Customers.Any())
            {
                foreach (var cust in request.Customers)
                {
                    var existing = await _context.Customers.FindAsync(cust.Id);
                    if (existing == null)
                    {
                        _context.Customers.Add(new Customer
                        {
                            Name = cust.Name,
                            Phone = cust.Phone,
                            Email = cust.Email,
                            TotalSpent = cust.TotalSpent,
                            LoyaltyPoints = cust.LoyaltyPoints
                        });
                        custAdded++;
                    }
                    else
                    {
                        existing.Name = cust.Name;
                        existing.Phone = cust.Phone;
                        existing.Email = cust.Email;
                        existing.TotalSpent = cust.TotalSpent;
                        existing.LoyaltyPoints = cust.LoyaltyPoints;
                    }
                }
            }

            // 4. Lưu thông tin Nhân viên
            if (request.Employees != null && request.Employees.Any())
            {
                foreach (var emp in request.Employees)
                {
                    var existing = await _context.Employees.FindAsync(emp.Id);
                    if (existing == null)
                    {
                        _context.Employees.Add(new Employee
                        {
                            Name = emp.Name,
                            Phone = emp.Phone,
                            Position = emp.Position,
                            Tier = emp.Tier,
                            ContractType = emp.ContractType,
                            HourlyRate = emp.HourlyRate,
                            MonthlySalary = emp.MonthlySalary,
                            WorkedHours = emp.WorkedHours,
                            WorkedDays = emp.WorkedDays,
                            Bonus = emp.Bonus,
                            Penalty = emp.Penalty
                        });
                        empAdded++;
                    }
                    else
                    {
                        existing.Name = emp.Name;
                        existing.Phone = emp.Phone;
                        existing.Position = emp.Position;
                        existing.Tier = emp.Tier;
                        existing.ContractType = emp.ContractType;
                        existing.HourlyRate = emp.HourlyRate;
                        existing.MonthlySalary = emp.MonthlySalary;
                        existing.WorkedHours = emp.WorkedHours;
                        existing.WorkedDays = emp.WorkedDays;
                        existing.Bonus = emp.Bonus;
                        existing.Penalty = emp.Penalty;
                    }
                }
            }

            // 5. Lưu thông tin Chấm công
            if (request.Attendance != null && request.Attendance.Any())
            {
                foreach (var att in request.Attendance)
                {
                    var exists = await _context.AttendanceRecords.AnyAsync(a => a.EmployeeId == att.EmployeeId && a.Timestamp == att.Timestamp);
                    if (!exists)
                    {
                        _context.AttendanceRecords.Add(new AttendanceRecord
                        {
                            EmployeeId = att.EmployeeId,
                            EmployeeName = att.EmployeeName,
                            Type = att.Type,
                            Method = att.Method,
                            Timestamp = att.Timestamp,
                            DurationHours = att.DurationHours,
                            Note = att.Note
                        });
                        attAdded++;
                    }
                }
            }

            // 6. Lưu thông tin Đơn hàng & Hóa đơn
            if (request.Orders != null && request.Orders.Any())
            {
                foreach (var ord in request.Orders)
                {
                    var exists = await _context.Orders.AnyAsync(o => o.Timestamp == ord.Timestamp && o.TableId == ord.TableId);
                    if (!exists)
                    {
                        var newOrder = new Order
                        {
                            TableId = ord.TableId,
                            TableName = ord.TableName,
                            CustomerId = ord.CustomerId,
                            CustomerName = ord.CustomerName,
                            CustomerTierName = ord.CustomerTierName,
                            EmployeeId = ord.EmployeeId,
                            EmployeeName = ord.EmployeeName,
                            Subtotal = ord.Subtotal,
                            DiscountPercent = ord.DiscountPercent,
                            PaymentMethod = ord.PaymentMethod,
                            Status = ord.Status,
                            Timestamp = ord.Timestamp
                        };

                        if (ord.Items != null && ord.Items.Any())
                        {
                            foreach (var item in ord.Items)
                            {
                                newOrder.Items.Add(new OrderItem
                                {
                                    MenuItemId = item.MenuItemId,
                                    MenuItemName = item.MenuItemName,
                                    Price = item.Price,
                                    Quantity = item.Quantity,
                                    Note = item.Note
                                });
                            }
                        }

                        _context.Orders.Add(newOrder);
                        ordersAdded++;
                    }
                }
            }

            await _context.SaveChangesAsync();
            await transaction.CommitAsync();

            var summary = $"Đã lưu toàn bộ thông tin vào SQL: {tablesAdded} bàn mới, {menuAdded} món mới, {custAdded} khách hàng, {empAdded} nhân viên, {attAdded} chấm công, {ordersAdded} hóa đơn";

            await _notificationService.RecordActivityAsync(new AppActivity
            {
                Source = "📱 App Đồng Bộ",
                ClientIp = HttpContext.Connection.RemoteIpAddress?.ToString() ?? "127.0.0.1",
                Action = "LƯU DỮ LIỆU SQL",
                Description = summary,
                Category = "Sync",
                Status = "SUCCESS"
            });

            return Ok(new
            {
                success = true,
                message = summary,
                details = new { tablesAdded, menuAdded, custAdded, empAdded, attAdded, ordersAdded }
            });
        }
        catch (Exception ex)
        {
            await transaction.RollbackAsync();
            return StatusCode(500, new { success = false, message = "Lỗi khi lưu dữ liệu vào CSDL: " + ex.Message });
        }
    }

    /// <summary>
    /// Xuất toàn bộ CSDL ra định dạng file Script SQL (T-SQL/ANSI SQL) có thể chạy trên SQL Server hoặc SQLite
    /// </summary>
    [HttpGet("export-sql")]
    public async Task<IActionResult> ExportDatabaseSql()
    {
        var sb = new StringBuilder();
        var nowStr = DateTime.Now.ToString("dd/MM/yyyy HH:mm:ss");

        sb.AppendLine("-- =============================================================================");
        sb.AppendLine($"-- DA RESTAURANT POS & MANAGEMENT - FULL DATABASE EXPORT (SQL SCRIPT)");
        sb.AppendLine($"-- Thời gian xuất: {nowStr}");
        sb.AppendLine($"-- Hệ Quản Trị: Microsoft SQL Server / SQLite Compatible");
        sb.AppendLine("-- =============================================================================");
        sb.AppendLine();
        sb.AppendLine("IF NOT EXISTS (SELECT * FROM sys.databases WHERE name = 'RestaurantPOS_DB')");
        sb.AppendLine("BEGIN");
        sb.AppendLine("    CREATE DATABASE [RestaurantPOS_DB];");
        sb.AppendLine("END;");
        sb.AppendLine("GO");
        sb.AppendLine("USE [RestaurantPOS_DB];");
        sb.AppendLine("GO");
        sb.AppendLine();

        // 1. Restaurant Users
        var users = await _context.RestaurantUsers.ToListAsync();
        sb.AppendLine("-- 1. DỮ LIỆU TÀI KHOẢN (restaurant_users)");
        sb.AppendLine("IF OBJECT_ID('restaurant_users', 'U') IS NOT NULL");
        sb.AppendLine("BEGIN");
        foreach (var u in users)
        {
            sb.AppendLine($"    INSERT INTO [restaurant_users] ([Username], [Password], [FullName], [Phone], [Role])");
            sb.AppendLine($"    VALUES (N'{EscapeSql(u.Username)}', N'{EscapeSql(u.Password)}', N'{EscapeSql(u.FullName)}', N'{EscapeSql(u.Phone)}', N'{EscapeSql(u.Role)}');");
        }
        sb.AppendLine("END;");
        sb.AppendLine("GO");
        sb.AppendLine();

        // 2. Restaurant Tables
        var tables = await _context.RestaurantTables.ToListAsync();
        sb.AppendLine("-- 2. DỮ LIỆU BÀN ĂN (restaurant_tables)");
        sb.AppendLine("IF OBJECT_ID('restaurant_tables', 'U') IS NOT NULL");
        sb.AppendLine("BEGIN");
        foreach (var t in tables)
        {
            var ordVal = t.CurrentOrderId.HasValue ? t.CurrentOrderId.Value.ToString() : "NULL";
            sb.AppendLine($"    INSERT INTO [restaurant_tables] ([Name], [Floor], [Seats], [Status], [CurrentOrderId])");
            sb.AppendLine($"    VALUES (N'{EscapeSql(t.Name)}', N'{EscapeSql(t.Floor)}', {t.Seats}, N'{EscapeSql(t.Status)}', {ordVal});");
        }
        sb.AppendLine("END;");
        sb.AppendLine("GO");
        sb.AppendLine();

        // 3. Menu Items
        var menuItems = await _context.MenuItems.ToListAsync();
        sb.AppendLine("-- 3. DỮ LIỆU THỰC ĐƠN (menu_items)");
        sb.AppendLine("IF OBJECT_ID('menu_items', 'U') IS NOT NULL");
        sb.AppendLine("BEGIN");
        foreach (var m in menuItems)
        {
            var avail = m.IsAvailable ? "1" : "0";
            sb.AppendLine($"    INSERT INTO [menu_items] ([Name], [Category], [Price], [Description], [IsAvailable])");
            sb.AppendLine($"    VALUES (N'{EscapeSql(m.Name)}', N'{EscapeSql(m.Category)}', {m.Price}, N'{EscapeSql(m.Description ?? "")}', {avail});");
        }
        sb.AppendLine("END;");
        sb.AppendLine("GO");
        sb.AppendLine();

        // 4. Customers
        var customers = await _context.Customers.ToListAsync();
        sb.AppendLine("-- 4. DỮ LIỆU KHÁCH HÀNG THÀNH VIÊN (customers)");
        sb.AppendLine("IF OBJECT_ID('customers', 'U') IS NOT NULL");
        sb.AppendLine("BEGIN");
        foreach (var c in customers)
        {
            sb.AppendLine($"    INSERT INTO [customers] ([Name], [Phone], [Email], [TotalSpent], [LoyaltyPoints])");
            sb.AppendLine($"    VALUES (N'{EscapeSql(c.Name)}', N'{EscapeSql(c.Phone)}', N'{EscapeSql(c.Email ?? "")}', {c.TotalSpent}, {c.LoyaltyPoints});");
        }
        sb.AppendLine("END;");
        sb.AppendLine("GO");
        sb.AppendLine();

        // 5. Employees
        var employees = await _context.Employees.ToListAsync();
        sb.AppendLine("-- 5. DỮ LIỆU NHÂN VIÊN & TIỀN LƯƠNG (employees)");
        sb.AppendLine("IF OBJECT_ID('employees', 'U') IS NOT NULL");
        sb.AppendLine("BEGIN");
        foreach (var e in employees)
        {
            sb.AppendLine($"    INSERT INTO [employees] ([Name], [Phone], [Position], [Tier], [ContractType], [HourlyRate], [MonthlySalary], [WorkedHours], [WorkedDays], [Bonus], [Penalty])");
            sb.AppendLine($"    VALUES (N'{EscapeSql(e.Name)}', N'{EscapeSql(e.Phone)}', N'{EscapeSql(e.Position)}', N'{EscapeSql(e.Tier)}', N'{EscapeSql(e.ContractType)}', {e.HourlyRate}, {e.MonthlySalary}, {e.WorkedHours}, {e.WorkedDays}, {e.Bonus}, {e.Penalty});");
        }
        sb.AppendLine("END;");
        sb.AppendLine("GO");
        sb.AppendLine();

        // 6. Attendance
        var attendance = await _context.AttendanceRecords.Take(100).ToListAsync();
        sb.AppendLine("-- 6. DỮ LIỆU CHẤM CÔNG (attendance_records)");
        sb.AppendLine("IF OBJECT_ID('attendance_records', 'U') IS NOT NULL");
        sb.AppendLine("BEGIN");
        foreach (var a in attendance)
        {
            sb.AppendLine($"    INSERT INTO [attendance_records] ([EmployeeId], [EmployeeName], [Type], [Method], [Timestamp], [DurationHours], [Note])");
            sb.AppendLine($"    VALUES ({a.EmployeeId}, N'{EscapeSql(a.EmployeeName)}', N'{EscapeSql(a.Type)}', N'{EscapeSql(a.Method)}', {a.Timestamp}, {a.DurationHours}, N'{EscapeSql(a.Note ?? "")}');");
        }
        sb.AppendLine("END;");
        sb.AppendLine("GO");
        sb.AppendLine();

        // 7. Orders & Items
        var orders = await _context.Orders.Include(o => o.Items).ToListAsync();
        sb.AppendLine("-- 7. DỮ LIỆU HÓA ĐƠN & CHI TIẾT ĐƠN HÀNG (orders & order_items)");
        sb.AppendLine("IF OBJECT_ID('orders', 'U') IS NOT NULL");
        sb.AppendLine("BEGIN");
        foreach (var o in orders)
        {
            var custIdStr = o.CustomerId.HasValue ? o.CustomerId.Value.ToString() : "NULL";
            sb.AppendLine($"    INSERT INTO [orders] ([TableId], [TableName], [CustomerId], [CustomerName], [CustomerTierName], [EmployeeId], [EmployeeName], [Subtotal], [DiscountPercent], [PaymentMethod], [Status], [Timestamp])");
            sb.AppendLine($"    VALUES ({o.TableId}, N'{EscapeSql(o.TableName)}', {custIdStr}, N'{EscapeSql(o.CustomerName)}', N'{EscapeSql(o.CustomerTierName)}', {o.EmployeeId}, N'{EscapeSql(o.EmployeeName)}', {o.Subtotal}, {o.DiscountPercent}, N'{EscapeSql(o.PaymentMethod)}', N'{EscapeSql(o.Status)}', {o.Timestamp});");
        }
        sb.AppendLine("END;");
        sb.AppendLine("GO");
        sb.AppendLine();

        var sqlBytes = Encoding.UTF8.GetBytes(sb.ToString());
        return File(sqlBytes, "application/sql", $"DA_Restaurant_Database_Backup_{DateTime.Now:yyyyMMdd_HHmmss}.sql");
    }

    /// <summary>
    /// Thống kê tình trạng cơ sở dữ liệu hiện tại (SQLite / SQL Server, số bản ghi từng bảng)
    /// </summary>
    [HttpGet("status")]
    public async Task<IActionResult> GetDatabaseStatus()
    {
        var provider = _context.Database.ProviderName ?? "Unknown";
        var isSqlite = provider.Contains("Sqlite", StringComparison.OrdinalIgnoreCase);

        var tablesCount = await _context.RestaurantTables.CountAsync();
        var menuItemsCount = await _context.MenuItems.CountAsync();
        var customersCount = await _context.Customers.CountAsync();
        var employeesCount = await _context.Employees.CountAsync();
        var ordersCount = await _context.Orders.CountAsync();
        var attendanceCount = await _context.AttendanceRecords.CountAsync();
        var activitiesCount = await _context.AppActivities.CountAsync();
        var activeOrdersCount = await _context.ActiveTableOrders.CountAsync();

        return Ok(new
        {
            success = true,
            provider,
            databaseType = isSqlite ? "SQLite (Tệp lưu trữ độc lập RestaurantPOS.db)" : "Microsoft SQL Server",
            connectionInfo = isSqlite ? "RestaurantPOS.db" : _configuration.GetConnectionString("DefaultConnection"),
            counts = new
            {
                tables = tablesCount,
                menuItems = menuItemsCount,
                customers = customersCount,
                employees = employeesCount,
                orders = ordersCount,
                attendance = attendanceCount,
                activities = activitiesCount,
                activeTableOrders = activeOrdersCount
            },
            status = "HEALTHY"
        });
    }

    [HttpGet("ping")]
    public IActionResult Ping()
    {
        return Ok(new
        {
            success = true,
            status = "ONLINE",
            service = "DA Restaurant Management & POS Web API",
            provider = _context.Database.ProviderName,
            timestamp = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds(),
            clientIp = HttpContext.Connection.RemoteIpAddress?.ToString()
        });
    }


    // ============================================================
    // ENDPOINTS NHAN TUNG RECORD DON LE TU APP (AUTO-SYNC)
    // ============================================================

    [HttpPost("customer")]
    public async Task<IActionResult> UpsertCustomer([FromBody] CustomerSyncDto dto)
    {
        if (dto == null || string.IsNullOrWhiteSpace(dto.Phone))
            return BadRequest(new { success = false, message = "Thieu so dien thoai." });

        var ex = await _context.Customers.FirstOrDefaultAsync(c => c.Phone == dto.Phone.Trim());
        if (ex == null)
        {
            ex = new Customer { Name = dto.Name?.Trim() ?? "", Phone = dto.Phone.Trim(), Email = dto.Email?.Trim() ?? "", TotalSpent = dto.TotalSpent, LoyaltyPoints = dto.LoyaltyPoints };
            _context.Customers.Add(ex);
        }
        else
        {
            ex.Name = dto.Name?.Trim() ?? ex.Name;
            ex.Email = dto.Email?.Trim() ?? ex.Email;
            ex.TotalSpent = Math.Max(ex.TotalSpent, dto.TotalSpent);
            ex.LoyaltyPoints = Math.Max(ex.LoyaltyPoints, dto.LoyaltyPoints);
        }
        await _context.SaveChangesAsync();
        await _notificationService.RecordActivityAsync(new AppActivity { Action = "KHACH HANG: " + ex.Name, Description = "App dong bo: " + ex.Name + " (" + ex.Phone + ") | " + ex.Tier, Category = "Customer", Source = "App Android", Status = "SUCCESS", Timestamp = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds() });
        return Ok(new { success = true, id = ex.Id, message = "Da luu khach hang." });
    }

    [HttpPost("employee")]
    public async Task<IActionResult> UpsertEmployee([FromBody] EmployeeSyncDto dto)
    {
        if (dto == null || string.IsNullOrWhiteSpace(dto.Phone))
            return BadRequest(new { success = false, message = "Thieu so dien thoai nhan vien." });

        var ex = await _context.Employees.FirstOrDefaultAsync(e => e.Phone == dto.Phone.Trim());
        if (ex == null)
        {
            ex = new Employee { Name = dto.Name?.Trim() ?? "", Phone = dto.Phone.Trim(), Position = dto.Position?.Trim() ?? "Nhan vien", Tier = dto.Tier ?? "CHINH_THUC", ContractType = dto.ContractType ?? "MONTHLY", MonthlySalary = dto.MonthlySalary, HourlyRate = dto.HourlyRate, Bonus = dto.Bonus, Penalty = dto.Penalty, WorkedDays = dto.WorkedDays };
            _context.Employees.Add(ex);
        }
        else
        {
            ex.Name = dto.Name?.Trim() ?? ex.Name;
            ex.Position = dto.Position?.Trim() ?? ex.Position;
            ex.Tier = dto.Tier ?? ex.Tier;
            ex.MonthlySalary = dto.MonthlySalary > 0 ? dto.MonthlySalary : ex.MonthlySalary;
            ex.HourlyRate = dto.HourlyRate > 0 ? dto.HourlyRate : ex.HourlyRate;
            ex.Bonus = dto.Bonus;
            ex.Penalty = dto.Penalty;
        }
        await _context.SaveChangesAsync();
        await _notificationService.RecordActivityAsync(new AppActivity { Action = "NHAN VIEN: " + ex.Name, Description = "App dong bo: " + ex.Name + " (" + ex.Phone + ") | " + ex.Position, Category = "Employee", Source = "App Android", Status = "SUCCESS", Timestamp = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds() });
        return Ok(new { success = true, id = ex.Id, message = "Da luu nhan vien." });
    }

    [HttpPost("table")]
    public async Task<IActionResult> UpsertTable([FromBody] TableSyncDto dto)
    {
        if (dto == null || string.IsNullOrWhiteSpace(dto.Name))
            return BadRequest(new { success = false, message = "Thieu ten ban." });

        var ex = await _context.RestaurantTables.FirstOrDefaultAsync(t => t.Name == dto.Name.Trim());
        if (ex == null)
        {
            ex = new RestaurantTable { Name = dto.Name.Trim(), Floor = dto.Floor?.Trim() ?? "Tang 1", Seats = dto.Seats > 0 ? dto.Seats : 4, Status = dto.Status ?? "AVAILABLE" };
            _context.RestaurantTables.Add(ex);
        }
        else
        {
            ex.Floor = dto.Floor?.Trim() ?? ex.Floor;
            ex.Seats = dto.Seats > 0 ? dto.Seats : ex.Seats;
            ex.Status = dto.Status ?? ex.Status;
        }
        await _context.SaveChangesAsync();
        return Ok(new { success = true, id = ex.Id, message = "Da luu ban." });
    }

    [HttpPost("menuitem")]
    public async Task<IActionResult> UpsertMenuItem([FromBody] MenuItemSyncDto dto)
    {
        if (dto == null || string.IsNullOrWhiteSpace(dto.Name))
            return BadRequest(new { success = false, message = "Thieu ten mon." });

        var ex = await _context.MenuItems.FirstOrDefaultAsync(m => m.Name == dto.Name.Trim());
        if (ex == null)
        {
            ex = new MenuItem { Name = dto.Name.Trim(), Category = dto.Category?.Trim() ?? "Chua phan loai", Price = dto.Price, Description = dto.Description?.Trim() ?? "", IsAvailable = dto.IsAvailable };
            _context.MenuItems.Add(ex);
        }
        else
        {
            ex.Price = dto.Price > 0 ? dto.Price : ex.Price;
            ex.Category = dto.Category?.Trim() ?? ex.Category;
            ex.Description = dto.Description?.Trim() ?? ex.Description;
            ex.IsAvailable = dto.IsAvailable;
        }
        await _context.SaveChangesAsync();
        return Ok(new { success = true, id = ex.Id, message = "Da luu mon an." });
    }

    [HttpPost("order")]
    public async Task<IActionResult> SaveOrder([FromBody] OrderSyncDto dto)
    {
        if (dto == null)
            return BadRequest(new { success = false, message = "Du lieu don hang khong hop le." });

        var exists = await _context.Orders.AnyAsync(o => o.TableId == dto.TableId && o.Timestamp == dto.CreatedAt);
        if (!exists)
        {
            var order = new Order
            {
                TableId = dto.TableId,
                TableName = dto.TableName ?? "",
                Status = dto.Status ?? "COMPLETED",
                Subtotal = dto.FinalAmount,
                PaymentMethod = dto.PaymentMethod ?? "Tien mat",
                EmployeeName = dto.StaffName ?? "",
                CustomerName = dto.CustomerName ?? "Khach vang lai",
                DiscountPercent = dto.DiscountPercent,
                Timestamp = dto.CreatedAt > 0 ? dto.CreatedAt : DateTimeOffset.UtcNow.ToUnixTimeMilliseconds()
            };
            _context.Orders.Add(order);
            await _context.SaveChangesAsync();
            await _notificationService.RecordActivityAsync(new AppActivity { Action = "THANH TOAN: " + dto.TableName, Description = "App dong bo don hang '" + dto.TableName + "' | " + dto.FinalAmount.ToString("N0") + "d | " + dto.PaymentMethod, Category = "Order", Source = "App Android", Status = "SUCCESS", Timestamp = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds() });
        }
        return Ok(new { success = true, message = "Da luu don hang." });
    }
    private static string EscapeSql(string input)
    {
        if (string.IsNullOrEmpty(input)) return string.Empty;
        return input.Replace("'", "''");
    }
}

public class SyncUploadRequest
{
    public List<RestaurantTable>? Tables { get; set; }
    public List<MenuItem>? MenuItems { get; set; }
    public List<Customer>? Customers { get; set; }
    public List<Employee>? Employees { get; set; }
    public List<AttendanceRecord>? Attendance { get; set; }
    public List<Order>? Orders { get; set; }
}


// DTOs nhan du lieu tung record tu App Android
public class CustomerSyncDto
{
    public string? Name { get; set; }
    public string? Phone { get; set; }
    public string? Email { get; set; }
    public double TotalSpent { get; set; }
    public int LoyaltyPoints { get; set; }
    public string? Tier { get; set; }
}

public class EmployeeSyncDto
{
    public string? Name { get; set; }
    public string? Phone { get; set; }
    public string? Email { get; set; }
    public string? Position { get; set; }
    public string? Tier { get; set; }
    public string? ContractType { get; set; }
    public double MonthlySalary { get; set; }
    public double HourlyRate { get; set; }
    public int WorkedDays { get; set; }
    public double Bonus { get; set; }
    public double Penalty { get; set; }
}

public class TableSyncDto
{
    public string? Name { get; set; }
    public string? Floor { get; set; }
    public int Seats { get; set; }
    public string? Status { get; set; }
}

public class MenuItemSyncDto
{
    public string? Name { get; set; }
    public string? Category { get; set; }
    public double Price { get; set; }
    public string? Description { get; set; }
    public bool IsAvailable { get; set; } = true;
}

public class OrderSyncDto
{
    public long TableId { get; set; }
    public string? TableName { get; set; }
    public string? CustomerName { get; set; }
    public string? Status { get; set; }
    public double Subtotal { get; set; }
    public double DiscountPercent { get; set; }
    public double VatPercent { get; set; }
    public double FinalAmount { get; set; }
    public string? PaymentMethod { get; set; }
    public string? StaffName { get; set; }
    public long CreatedAt { get; set; }
}
