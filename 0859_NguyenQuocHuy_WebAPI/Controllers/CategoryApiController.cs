using _0859_NguyenQuocHuy_WebAPI.Models;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;

namespace _0859_NguyenQuocHuy_WebAPI.Controllers;

[Route("api/[controller]")]
[ApiController]
public class CategoryApiController : ControllerBase
{
    private readonly ApplicationDbContext _dbContext;
    private readonly ILogger<CategoryApiController> _logger;

    public CategoryApiController(
        ApplicationDbContext dbContext,
        ILogger<CategoryApiController> logger)
    {
        _dbContext = dbContext;
        _logger = logger;
    }

    /// <summary>
    /// Lấy danh sách tất cả các danh mục cùng các sản phẩm thuộc danh mục (Hỗ trợ tìm kiếm)
    /// </summary>
    [HttpGet]
    public async Task<IActionResult> GetCategories([FromQuery] string? search = null)
    {
        try
        {
            var query = _dbContext.Categories
                .AsNoTracking()
                .Include(c => c.Products)
                .AsQueryable();

            if (!string.IsNullOrWhiteSpace(search))
            {
                var s = search.Trim().ToLower();
                query = query.Where(c => c.Name.ToLower().Contains(s) || (c.Description != null && c.Description.ToLower().Contains(s)));
            }

            var categories = await query.ToListAsync();

            // Bổ sung thuộc tính thống kê số lượng sản phẩm
            var response = categories.Select(c => new
            {
                c.Id,
                c.Name,
                c.Description,
                productsCount = c.Products.Count,
                products = c.Products.Select(p => new
                {
                    p.Id,
                    p.Name,
                    p.Price,
                    p.Image,
                    p.Description
                })
            });

            return Ok(response);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Lỗi xảy ra khi truy vấn danh sách danh mục");
            return StatusCode(StatusCodes.Status500InternalServerError, new { message = "Lỗi khi tải danh mục", error = ex.Message });
        }
    }

    /// <summary>
    /// Xem chi tiết một danh mục theo mã ID
    /// </summary>
    [HttpGet("{id:int}")]
    public async Task<IActionResult> GetCategoryById(int id)
    {
        try
        {
            var targetCategory = await _dbContext.Categories
                .AsNoTracking()
                .Include(c => c.Products)
                .FirstOrDefaultAsync(c => c.Id == id);

            if (targetCategory is null)
            {
                return NotFound(new { message = $"Không tìm thấy danh mục mang ID = {id}" });
            }

            return Ok(new
            {
                targetCategory.Id,
                targetCategory.Name,
                targetCategory.Description,
                productsCount = targetCategory.Products.Count,
                products = targetCategory.Products
            });
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Lỗi khi lấy thông tin danh mục {Id}", id);
            return StatusCode(StatusCodes.Status500InternalServerError, new { message = "Lỗi máy chủ", error = ex.Message });
        }
    }

    /// <summary>
    /// Tạo mới một danh mục (Yêu cầu vai trò Admin)
    /// </summary>
    [HttpPost]
    [Authorize(Roles = "Admin")]
    public async Task<IActionResult> AddCategory([FromBody] Category category)
    {
        if (!ModelState.IsValid)
        {
            return BadRequest(ModelState);
        }

        try
        {
            await _dbContext.Categories.AddAsync(category);
            await _dbContext.SaveChangesAsync();

            return CreatedAtAction(nameof(GetCategoryById), new { id = category.Id }, category);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Lỗi khi thêm danh mục mới");
            return StatusCode(StatusCodes.Status500InternalServerError, new { message = "Không thể thêm danh mục", error = ex.Message });
        }
    }

    /// <summary>
    /// Chỉnh sửa danh mục hiện có (Yêu cầu vai trò Admin)
    /// </summary>
    [HttpPut("{id:int}")]
    [Authorize(Roles = "Admin")]
    public async Task<IActionResult> UpdateCategory(int id, [FromBody] Category category)
    {
        if (id != category.Id)
        {
            return BadRequest(new { message = "ID danh mục không trùng khớp" });
        }

        if (!ModelState.IsValid)
        {
            return BadRequest(ModelState);
        }

        var existing = await _dbContext.Categories.FindAsync(id);
        if (existing is null)
        {
            return NotFound(new { message = $"Danh mục mang mã {id} không tồn tại" });
        }

        existing.Name = category.Name;
        existing.Description = category.Description;

        try
        {
            await _dbContext.SaveChangesAsync();
            return Ok(new { status = true, message = "Cập nhật danh mục thành công", data = existing });
        }
        catch (DbUpdateConcurrencyException ex)
        {
            _logger.LogError(ex, "Lỗi xung đột khi cập nhật danh mục {Id}", id);
            return StatusCode(StatusCodes.Status409Conflict, new { message = "Dữ liệu đã bị thay đổi bởi phiên khác" });
        }
    }

    /// <summary>
    /// Xóa một danh mục (Yêu cầu Admin - sản phẩm thuộc danh mục sẽ chuyển sang CategoryId = null)
    /// </summary>
    [HttpDelete("{id:int}")]
    [Authorize(Roles = "Admin")]
    public async Task<IActionResult> DeleteCategory(int id)
    {
        var categoryToDelete = await _dbContext.Categories
            .Include(c => c.Products)
            .FirstOrDefaultAsync(c => c.Id == id);

        if (categoryToDelete is null)
        {
            return NotFound(new { message = $"Không tìm thấy danh mục {id} để xóa" });
        }

        try
        {
            // Thiết lập sản phẩm thuộc danh mục này thành null theo yêu cầu bài học
            foreach (var product in categoryToDelete.Products)
            {
                product.CategoryId = null;
            }

            _dbContext.Categories.Remove(categoryToDelete);
            await _dbContext.SaveChangesAsync();

            return Ok(new
            {
                status = true,
                message = $"Đã xóa danh mục '{categoryToDelete.Name}'. Tất cả sản phẩm thuộc danh mục đã được chuyển về trạng thái Chưa phân loại (CategoryId = null)."
            });
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Lỗi khi xóa danh mục {Id}", id);
            return StatusCode(StatusCodes.Status500InternalServerError, new { message = "Không thể xóa danh mục", error = ex.Message });
        }
    }
}
