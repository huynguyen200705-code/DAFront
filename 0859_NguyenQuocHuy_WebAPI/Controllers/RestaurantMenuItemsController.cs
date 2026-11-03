using _0859_NguyenQuocHuy_WebAPI.Models;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;

namespace _0859_NguyenQuocHuy_WebAPI.Controllers;

[Route("api/menu-items")]
[ApiController]
public class RestaurantMenuItemsController : ControllerBase
{
    private readonly ApplicationDbContext _context;

    public RestaurantMenuItemsController(ApplicationDbContext context)
    {
        _context = context;
    }

    [HttpGet]
    public async Task<IActionResult> GetAll([FromQuery] string? category, [FromQuery] string? search)
    {
        var query = _context.MenuItems.AsQueryable();

        if (!string.IsNullOrWhiteSpace(category) && category != "Tất cả")
        {
            query = query.Where(m => m.Category == category);
        }

        if (!string.IsNullOrWhiteSpace(search))
        {
            var s = search.Trim().ToLower();
            query = query.Where(m => m.Name.ToLower().Contains(s) || (m.Description != null && m.Description.ToLower().Contains(s)));
        }

        var items = await query.OrderBy(m => m.Category).ThenBy(m => m.Name).ToListAsync();
        return Ok(new { success = true, data = items });
    }

    [HttpGet("categories")]
    public async Task<IActionResult> GetCategories()
    {
        var categories = await _context.MenuItems
            .Select(m => m.Category)
            .Distinct()
            .ToListAsync();

        return Ok(new { success = true, data = categories });
    }

    [HttpGet("{id}")]
    public async Task<IActionResult> GetById(long id)
    {
        var item = await _context.MenuItems.FindAsync(id);
        if (item == null)
        {
            return NotFound(new { success = false, message = "Không tìm thấy món ăn." });
        }

        return Ok(new { success = true, data = item });
    }

    [HttpPost]
    public async Task<IActionResult> Create([FromBody] MenuItem item)
    {
        if (string.IsNullOrWhiteSpace(item.Name))
        {
            return BadRequest(new { success = false, message = "Tên món không được để trống." });
        }

        _context.MenuItems.Add(item);
        await _context.SaveChangesAsync();

        return CreatedAtAction(nameof(GetById), new { id = item.Id }, new { success = true, data = item });
    }

    [HttpPut("{id}")]
    public async Task<IActionResult> Update(long id, [FromBody] MenuItem item)
    {
        var existing = await _context.MenuItems.FindAsync(id);
        if (existing == null)
        {
            return NotFound(new { success = false, message = "Không tìm thấy món ăn." });
        }

        existing.Name = item.Name;
        existing.Category = item.Category;
        existing.Price = item.Price;
        existing.Description = item.Description;
        existing.IsAvailable = item.IsAvailable;

        await _context.SaveChangesAsync();
        return Ok(new { success = true, data = existing });
    }

    [HttpDelete("{id}")]
    public async Task<IActionResult> Delete(long id)
    {
        var existing = await _context.MenuItems.FindAsync(id);
        if (existing == null)
        {
            return NotFound(new { success = false, message = "Không tìm thấy món ăn." });
        }

        _context.MenuItems.Remove(existing);
        await _context.SaveChangesAsync();
        return Ok(new { success = true, message = "Đã xóa món ăn thành công." });
    }
}
