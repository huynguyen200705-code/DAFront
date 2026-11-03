using _0859_NguyenQuocHuy_WebAPI.Models;
using _0859_NguyenQuocHuy_WebAPI.Services;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;

namespace _0859_NguyenQuocHuy_WebAPI.Controllers;

[Route("api/attendance")]
[ApiController]
public class RestaurantAttendanceController : ControllerBase
{
    private readonly ApplicationDbContext _context;
    private readonly IAppNotificationService _notificationService;

    public RestaurantAttendanceController(
        ApplicationDbContext context,
        IAppNotificationService notificationService)
    {
        _context = context;
        _notificationService = notificationService;
    }

    [HttpGet]
    public async Task<IActionResult> GetAll([FromQuery] long? employeeId)
    {
        var query = _context.AttendanceRecords.AsQueryable();

        if (employeeId.HasValue && employeeId.Value > 0)
        {
            query = query.Where(a => a.EmployeeId == employeeId.Value);
        }

        var records = await query
            .OrderByDescending(a => a.Timestamp)
            .ToListAsync();

        var result = records.Select(r => new
        {
            r.Id,
            r.EmployeeId,
            r.EmployeeName,
            r.Type,
            r.Method,
            r.Timestamp,
            r.DurationHours,
            r.Note,
            formattedTime = r.FormattedTime,
            formattedHourOnly = r.FormattedHourOnly
        });

        return Ok(new { success = true, data = result });
    }

    [HttpGet("last/{employeeId}")]
    public async Task<IActionResult> GetLastAttendance(long employeeId)
    {
        var record = await _context.AttendanceRecords
            .Where(a => a.EmployeeId == employeeId)
            .OrderByDescending(a => a.Timestamp)
            .FirstOrDefaultAsync();

        if (record == null)
        {
            return NotFound(new { success = false, message = "Chưa có lượt chấm công nào." });
        }

        return Ok(new
        {
            success = true,
            data = new
            {
                record.Id,
                record.EmployeeId,
                record.EmployeeName,
                record.Type,
                record.Method,
                record.Timestamp,
                record.DurationHours,
                record.Note,
                formattedTime = record.FormattedTime,
                formattedHourOnly = record.FormattedHourOnly
            }
        });
    }

    [HttpPost("check-in")]
    public async Task<IActionResult> CheckIn([FromBody] AttendanceCheckInRequest request)
    {
        var employee = await _context.Employees.FindAsync(request.EmployeeId);
        if (employee == null)
        {
            return NotFound(new { success = false, message = "Không tìm thấy nhân viên." });
        }

        var now = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();
        var record = new AttendanceRecord
        {
            EmployeeId = employee.Id,
            EmployeeName = employee.Name,
            Type = "CHECK_IN",
            Method = string.IsNullOrWhiteSpace(request.Method) ? "NFC" : request.Method.Trim().ToUpper(),
            Timestamp = now,
            DurationHours = 0.0,
            Note = request.Note ?? $"Vào ca lúc {DateTime.Now:HH:mm}"
        };

        _context.AttendanceRecords.Add(record);
        await _context.SaveChangesAsync();

        await _notificationService.RecordActivityAsync(new AppActivity
        {
            Source = "📱 App Nhân Viên",
            ClientIp = HttpContext.Connection.RemoteIpAddress?.ToString() ?? "127.0.0.1",
            Action = "CHẤM CÔNG VÀO CA",
            Description = $"Nhân viên {employee.Name} đã điểm danh vào ca qua {record.Method} lúc {DateTime.Now:HH:mm}",
            Category = "Attendance",
            Status = "SUCCESS"
        });

        return Ok(new
        {
            success = true,
            message = $"Nhân viên {employee.Name} đã điểm danh vào ca thành công ({record.Method})!",
            data = record
        });
    }

    [HttpPost("check-out")]
    public async Task<IActionResult> CheckOut([FromBody] AttendanceCheckOutRequest request)
    {
        var employee = await _context.Employees.FindAsync(request.EmployeeId);
        if (employee == null)
        {
            return NotFound(new { success = false, message = "Không tìm thấy nhân viên." });
        }

        var now = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();

        // Find last check-in
        var lastCheckIn = await _context.AttendanceRecords
            .Where(a => a.EmployeeId == request.EmployeeId && a.Type == "CHECK_IN")
            .OrderByDescending(a => a.Timestamp)
            .FirstOrDefaultAsync();

        double workedHoursThisShift = 0.0;
        if (lastCheckIn != null && lastCheckIn.Timestamp < now)
        {
            var diffMillis = now - lastCheckIn.Timestamp;
            workedHoursThisShift = Math.Round(diffMillis / (1000.0 * 60.0 * 60.0), 2);
        }

        var record = new AttendanceRecord
        {
            EmployeeId = employee.Id,
            EmployeeName = employee.Name,
            Type = "CHECK_OUT",
            Method = string.IsNullOrWhiteSpace(request.Method) ? "NFC" : request.Method.Trim().ToUpper(),
            Timestamp = now,
            DurationHours = workedHoursThisShift,
            Note = request.Note ?? $"Tan ca lúc {DateTime.Now:HH:mm} (Làm việc: {workedHoursThisShift}h)"
        };

        // Update employee hours or days
        if (employee.ContractType == "HOURLY")
        {
            employee.WorkedHours += workedHoursThisShift;
        }
        else
        {
            if (workedHoursThisShift >= 4.0)
            {
                employee.WorkedDays += 1.0;
            }
            else if (workedHoursThisShift > 0.0)
            {
                employee.WorkedDays += 0.5;
            }
        }

        _context.AttendanceRecords.Add(record);
        await _context.SaveChangesAsync();

        await _notificationService.RecordActivityAsync(new AppActivity
        {
            Source = "📱 App Nhân Viên",
            ClientIp = HttpContext.Connection.RemoteIpAddress?.ToString() ?? "127.0.0.1",
            Action = "CHẤM CÔNG TAN CA",
            Description = $"Nhân viên {employee.Name} đã chấm công tan ca qua {record.Method} (Thời lượng ca: {workedHoursThisShift} giờ)",
            Category = "Attendance",
            Status = "SUCCESS"
        });

        return Ok(new
        {
            success = true,
            message = $"Nhân viên {employee.Name} đã chấm công tan ca ({record.Method}). Thời lượng ca: {workedHoursThisShift} giờ.",
            data = record
        });
    }

    [HttpPost("record")]
    public async Task<IActionResult> RecordDirect([FromBody] AttendanceRecord record)
    {
        if (record.EmployeeId <= 0)
        {
            return BadRequest(new { success = false, message = "EmployeeId không hợp lệ." });
        }

        if (string.IsNullOrWhiteSpace(record.EmployeeName))
        {
            var emp = await _context.Employees.FindAsync(record.EmployeeId);
            if (emp != null) record.EmployeeName = emp.Name;
        }

        if (record.Timestamp <= 0)
        {
            record.Timestamp = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();
        }

        _context.AttendanceRecords.Add(record);
        await _context.SaveChangesAsync();

        await _notificationService.RecordActivityAsync(new AppActivity
        {
            Source = "📱 App Chấm Công",
            ClientIp = HttpContext.Connection.RemoteIpAddress?.ToString() ?? "127.0.0.1",
            Action = $"CHẤM CÔNG {(record.Type == "CHECK_IN" ? "VÀO CA" : "TAN CA")}",
            Description = $"Nhân viên {record.EmployeeName} ghi nhận chấm công ({record.Method})",
            Category = "Attendance",
            Status = "SUCCESS"
        });

        return Ok(new { success = true, data = record });
    }
}
