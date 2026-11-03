using _0859_NguyenQuocHuy_WebAPI.Models;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;

namespace _0859_NguyenQuocHuy_WebAPI.Controllers;

[Route("api/tables")]
[ApiController]
public class RestaurantTablesController : ControllerBase
{
    private readonly ApplicationDbContext _context;

    public RestaurantTablesController(ApplicationDbContext context)
    {
        _context = context;
    }

    [HttpGet]
    public async Task<IActionResult> GetAllTables()
    {
        var tables = await _context.RestaurantTables
            .OrderBy(t => t.Floor)
            .ThenBy(t => t.Name)
            .ToListAsync();

        return Ok(new { success = true, data = tables });
    }

    [HttpGet("{id}")]
    public async Task<IActionResult> GetTableById(long id)
    {
        var table = await _context.RestaurantTables.FindAsync(id);
        if (table == null)
        {
            return NotFound(new { success = false, message = "Không tìm thấy bàn." });
        }

        var activeItems = await _context.ActiveTableOrders
            .Where(a => a.TableId == id)
            .ToListAsync();

        return Ok(new { success = true, data = table, activeItems });
    }

    [HttpPost]
    public async Task<IActionResult> CreateTable([FromBody] RestaurantTable table)
    {
        if (string.IsNullOrWhiteSpace(table.Name))
        {
            return BadRequest(new { success = false, message = "Tên bàn không được để trống." });
        }

        _context.RestaurantTables.Add(table);
        await _context.SaveChangesAsync();

        return CreatedAtAction(nameof(GetTableById), new { id = table.Id }, new { success = true, data = table });
    }

    [HttpPut("{id}")]
    public async Task<IActionResult> UpdateTable(long id, [FromBody] RestaurantTable table)
    {
        var existing = await _context.RestaurantTables.FindAsync(id);
        if (existing == null)
        {
            return NotFound(new { success = false, message = "Không tìm thấy bàn." });
        }

        existing.Name = table.Name;
        existing.Floor = table.Floor;
        existing.Seats = table.Seats;
        existing.Status = table.Status;
        existing.CurrentOrderId = table.CurrentOrderId;

        await _context.SaveChangesAsync();
        return Ok(new { success = true, data = existing });
    }

    [HttpPut("{id}/status")]
    public async Task<IActionResult> UpdateStatus(long id, [FromBody] UpdateTableStatusRequest request)
    {
        var existing = await _context.RestaurantTables.FindAsync(id);
        if (existing == null)
        {
            return NotFound(new { success = false, message = "Không tìm thấy bàn." });
        }

        existing.Status = request.Status;
        if (request.Status == "TRONG")
        {
            existing.CurrentOrderId = null;
            var activeItems = _context.ActiveTableOrders.Where(a => a.TableId == id);
            _context.ActiveTableOrders.RemoveRange(activeItems);
        }

        await _context.SaveChangesAsync();
        return Ok(new { success = true, data = existing });
    }

    [HttpDelete("{id}")]
    public async Task<IActionResult> DeleteTable(long id)
    {
        var existing = await _context.RestaurantTables.FindAsync(id);
        if (existing == null)
        {
            return NotFound(new { success = false, message = "Không tìm thấy bàn." });
        }

        var activeItems = _context.ActiveTableOrders.Where(a => a.TableId == id);
        _context.ActiveTableOrders.RemoveRange(activeItems);

        _context.RestaurantTables.Remove(existing);
        await _context.SaveChangesAsync();
        return Ok(new { success = true, message = "Đã xóa bàn thành công." });
    }

    [HttpGet("{id}/active-order")]
    public async Task<IActionResult> GetActiveOrder(long id)
    {
        var items = await _context.ActiveTableOrders
            .Where(a => a.TableId == id)
            .ToListAsync();

        return Ok(new { success = true, data = items });
    }

    [HttpPost("{id}/active-order")]
    public async Task<IActionResult> SaveActiveOrder(long id, [FromBody] SaveActiveOrderRequest request)
    {
        var table = await _context.RestaurantTables.FindAsync(id);
        if (table == null)
        {
            return NotFound(new { success = false, message = "Không tìm thấy bàn." });
        }

        // Clear existing active items for this table
        var oldItems = _context.ActiveTableOrders.Where(a => a.TableId == id);
        _context.ActiveTableOrders.RemoveRange(oldItems);

        if (request.Items != null && request.Items.Any())
        {
            var newEntities = request.Items.Select(i => new ActiveTableOrder
            {
                TableId = id,
                MenuItemId = i.MenuItemId,
                MenuItemName = i.MenuItemName,
                Price = i.Price,
                Quantity = i.Quantity,
                Note = i.Note
            });
            _context.ActiveTableOrders.AddRange(newEntities);
            table.Status = "DANG_PHUC_VU";
        }
        else
        {
            table.Status = "TRONG";
            table.CurrentOrderId = null;
        }

        await _context.SaveChangesAsync();
        return Ok(new { success = true, message = "Đã cập nhật thực đơn đang gọi của bàn.", tableStatus = table.Status });
    }

    [HttpDelete("{id}/active-order")]
    public async Task<IActionResult> ClearActiveOrder(long id)
    {
        var items = _context.ActiveTableOrders.Where(a => a.TableId == id);
        _context.ActiveTableOrders.RemoveRange(items);

        var table = await _context.RestaurantTables.FindAsync(id);
        if (table != null)
        {
            table.Status = "TRONG";
            table.CurrentOrderId = null;
        }

        await _context.SaveChangesAsync();
        return Ok(new { success = true, message = "Đã dọn bàn / xóa order đang phục vụ." });
    }
}
