using _0859_NguyenQuocHuy_WebAPI.Models;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;

namespace _0859_NguyenQuocHuy_WebAPI.Controllers;

[Route("api/employees")]
[ApiController]
public class RestaurantEmployeesController : ControllerBase
{
    private readonly ApplicationDbContext _context;

    public RestaurantEmployeesController(ApplicationDbContext context)
    {
        _context = context;
    }

    [HttpGet]
    public async Task<IActionResult> GetAll([FromQuery] string? search)
    {
        var query = _context.Employees.AsQueryable();

        if (!string.IsNullOrWhiteSpace(search))
        {
            var s = search.Trim().ToLower();
            query = query.Where(e => e.Name.ToLower().Contains(s) || e.Phone.Contains(s) || e.Position.ToLower().Contains(s));
        }

        var employees = await query.ToListAsync();

        var result = employees.Select(e => new
        {
            e.Id,
            e.Name,
            e.Phone,
            e.Position,
            e.Tier,
            e.ContractType,
            e.HourlyRate,
            e.MonthlySalary,
            e.WorkedHours,
            e.WorkedDays,
            e.Bonus,
            e.Penalty,
            calculatedSalary = e.CalculatedSalary,
            nfcTagCode = e.NfcTagCode,
            qrCodeContent = e.QrCodeContent
        });

        return Ok(new { success = true, data = result });
    }

    [HttpGet("{id}")]
    public async Task<IActionResult> GetById(long id)
    {
        var emp = await _context.Employees.FindAsync(id);
        if (emp == null)
        {
            return NotFound(new { success = false, message = "Không tìm thấy nhân viên." });
        }

        return Ok(new
        {
            success = true,
            data = new
            {
                emp.Id,
                emp.Name,
                emp.Phone,
                emp.Position,
                emp.Tier,
                emp.ContractType,
                emp.HourlyRate,
                emp.MonthlySalary,
                emp.WorkedHours,
                emp.WorkedDays,
                emp.Bonus,
                emp.Penalty,
                calculatedSalary = emp.CalculatedSalary,
                nfcTagCode = emp.NfcTagCode,
                qrCodeContent = emp.QrCodeContent
            }
        });
    }

    [HttpPost]
    public async Task<IActionResult> Create([FromBody] Employee emp)
    {
        if (string.IsNullOrWhiteSpace(emp.Name) || string.IsNullOrWhiteSpace(emp.Phone))
        {
            return BadRequest(new { success = false, message = "Tên và số điện thoại không được để trống." });
        }

        _context.Employees.Add(emp);
        await _context.SaveChangesAsync();

        return CreatedAtAction(nameof(GetById), new { id = emp.Id }, new { success = true, data = emp });
    }

    [HttpPut("{id}")]
    public async Task<IActionResult> Update(long id, [FromBody] Employee emp)
    {
        var existing = await _context.Employees.FindAsync(id);
        if (existing == null)
        {
            return NotFound(new { success = false, message = "Không tìm thấy nhân viên." });
        }

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

        await _context.SaveChangesAsync();
        return Ok(new { success = true, data = existing });
    }

    [HttpDelete("{id}")]
    public async Task<IActionResult> Delete(long id)
    {
        var existing = await _context.Employees.FindAsync(id);
        if (existing == null)
        {
            return NotFound(new { success = false, message = "Không tìm thấy nhân viên." });
        }

        _context.Employees.Remove(existing);
        await _context.SaveChangesAsync();
        return Ok(new { success = true, message = "Đã xóa nhân viên thành công." });
    }

    [HttpGet("{id}/payslip")]
    public async Task<IActionResult> GetPayslip(long id)
    {
        var emp = await _context.Employees.FindAsync(id);
        if (emp == null)
        {
            return NotFound(new { success = false, message = "Không tìm thấy nhân viên." });
        }

        var baseAmount = emp.ContractType == "HOURLY"
            ? emp.HourlyRate * emp.WorkedHours
            : emp.MonthlySalary * (emp.WorkedDays / 26.0);

        var payslip = new
        {
            employeeId = emp.Id,
            name = emp.Name,
            position = emp.Position,
            tier = emp.Tier,
            contractType = emp.ContractType,
            baseCalculation = emp.ContractType == "HOURLY"
                ? $"{emp.WorkedHours} giờ x {emp.HourlyRate:N0} đ"
                : $"{emp.WorkedDays}/26 ngày x {emp.MonthlySalary:N0} đ",
            baseAmount,
            bonus = emp.Bonus,
            penalty = emp.Penalty,
            netSalary = emp.CalculatedSalary
        };

        return Ok(new { success = true, data = payslip });
    }
}
