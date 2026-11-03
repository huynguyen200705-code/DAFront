using _0859_NguyenQuocHuy_WebAPI.Models;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;

namespace _0859_NguyenQuocHuy_WebAPI.Controllers;

[Route("api/customers")]
[ApiController]
public class RestaurantCustomersController : ControllerBase
{
    private readonly ApplicationDbContext _context;

    public RestaurantCustomersController(ApplicationDbContext context)
    {
        _context = context;
    }

    [HttpGet]
    public async Task<IActionResult> GetAll([FromQuery] string? search)
    {
        var query = _context.Customers.AsQueryable();

        if (!string.IsNullOrWhiteSpace(search))
        {
            var s = search.Trim().ToLower();
            query = query.Where(c => c.Name.ToLower().Contains(s) || c.Phone.Contains(s));
        }

        var customers = await query
            .OrderByDescending(c => c.TotalSpent)
            .ToListAsync();

        var result = customers.Select(c => new
        {
            c.Id,
            c.Name,
            c.Phone,
            c.Email,
            c.TotalSpent,
            c.LoyaltyPoints,
            tier = c.Tier,
            tierDisplayName = c.TierDisplayName,
            discountPercent = c.DiscountPercent
        });

        return Ok(new { success = true, data = result });
    }

    [HttpGet("{id}")]
    public async Task<IActionResult> GetById(long id)
    {
        var customer = await _context.Customers.FindAsync(id);
        if (customer == null)
        {
            return NotFound(new { success = false, message = "Không tìm thấy khách hàng." });
        }

        return Ok(new
        {
            success = true,
            data = new
            {
                customer.Id,
                customer.Name,
                customer.Phone,
                customer.Email,
                customer.TotalSpent,
                customer.LoyaltyPoints,
                tier = customer.Tier,
                tierDisplayName = customer.TierDisplayName,
                discountPercent = customer.DiscountPercent
            }
        });
    }

    [HttpPost]
    public async Task<IActionResult> Create([FromBody] Customer customer)
    {
        if (string.IsNullOrWhiteSpace(customer.Name) || string.IsNullOrWhiteSpace(customer.Phone))
        {
            return BadRequest(new { success = false, message = "Tên và số điện thoại không được để trống." });
        }

        _context.Customers.Add(customer);
        await _context.SaveChangesAsync();

        return CreatedAtAction(nameof(GetById), new { id = customer.Id }, new { success = true, data = customer });
    }

    [HttpPut("{id}")]
    public async Task<IActionResult> Update(long id, [FromBody] Customer customer)
    {
        var existing = await _context.Customers.FindAsync(id);
        if (existing == null)
        {
            return NotFound(new { success = false, message = "Không tìm thấy khách hàng." });
        }

        existing.Name = customer.Name;
        existing.Phone = customer.Phone;
        existing.Email = customer.Email;
        existing.TotalSpent = customer.TotalSpent;
        existing.LoyaltyPoints = customer.LoyaltyPoints;

        await _context.SaveChangesAsync();
        return Ok(new { success = true, data = existing });
    }

    [HttpDelete("{id}")]
    public async Task<IActionResult> Delete(long id)
    {
        var existing = await _context.Customers.FindAsync(id);
        if (existing == null)
        {
            return NotFound(new { success = false, message = "Không tìm thấy khách hàng." });
        }

        _context.Customers.Remove(existing);
        await _context.SaveChangesAsync();
        return Ok(new { success = true, message = "Đã xóa khách hàng thành công." });
    }
}
