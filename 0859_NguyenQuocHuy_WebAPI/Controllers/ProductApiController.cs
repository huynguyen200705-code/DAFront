using _0859_NguyenQuocHuy_WebAPI.Models;
using _0859_NguyenQuocHuy_WebAPI.Repositories;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;

namespace _0859_NguyenQuocHuy_WebAPI.Controllers;

[Route("api/[controller]")]
[ApiController]
public class ProductApiController : ControllerBase
{
    private readonly IproductRepository _productRepo;
    private readonly ILogger<ProductApiController> _logger;

    public ProductApiController(
        IproductRepository productRepo,
        ILogger<ProductApiController> logger)
    {
        _productRepo = productRepo;
        _logger = logger;
    }

    /// <summary>
    /// Lấy danh sách sản phẩm có hỗ trợ lọc, tìm kiếm, khoảng giá và sắp xếp (Yêu cầu đăng nhập: Role User hoặc Admin)
    /// </summary>
    [HttpGet]
    [Authorize]
    public async Task<IActionResult> GetProducts(
        [FromQuery] string? search = null,
        [FromQuery] int? categoryId = null,
        [FromQuery] decimal? minPrice = null,
        [FromQuery] decimal? maxPrice = null,
        [FromQuery] string? sortBy = null)
    {
        try
        {
            var productList = await _productRepo.GetProductsAsync(search, categoryId, minPrice, maxPrice, sortBy);
            return Ok(productList);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Lỗi xảy ra khi lấy danh sách sản phẩm");
            return StatusCode(StatusCodes.Status500InternalServerError, new { message = "Lỗi máy chủ nội bộ", detail = ex.Message });
        }
    }

    /// <summary>
    /// Lấy thông tin chi tiết một sản phẩm theo ID (Yêu cầu đăng nhập)
    /// </summary>
    [HttpGet("{id:int}")]
    [Authorize]
    public async Task<IActionResult> GetProductById(int id)
    {
        try
        {
            var foundProduct = await _productRepo.GetProductByIdAsync(id);
            if (foundProduct is null)
            {
                return NotFound(new { message = $"Không tìm thấy sản phẩm với ID = {id}" });
            }

            return Ok(foundProduct);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Lỗi xảy ra khi lấy chi tiết sản phẩm {Id}", id);
            return StatusCode(StatusCodes.Status500InternalServerError, new { message = "Lỗi máy chủ nội bộ", detail = ex.Message });
        }
    }

    /// <summary>
    /// Lấy danh sách sản phẩm thuộc một danh mục cụ thể (Yêu cầu đăng nhập)
    /// </summary>
    [HttpGet("by-category/{categoryId:int}")]
    [Authorize]
    public async Task<IActionResult> GetProductsByCategory(int categoryId)
    {
        try
        {
            var products = await _productRepo.GetProductsByCategoryIdAsync(categoryId);
            return Ok(products);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Lỗi khi lấy sản phẩm theo danh mục {CategoryId}", categoryId);
            return StatusCode(StatusCodes.Status500InternalServerError, new { message = "Lỗi máy chủ nội bộ", detail = ex.Message });
        }
    }

    /// <summary>
    /// Thêm sản phẩm mới (Chỉ dành cho quyền Admin)
    /// </summary>
    [HttpPost]
    [Authorize(Roles = "Admin")]
    public async Task<IActionResult> AddProduct([FromBody] Product product)
    {
        if (!ModelState.IsValid)
        {
            return BadRequest(ModelState);
        }

        try
        {
            await _productRepo.AddProductAsync(product);
            return CreatedAtAction(nameof(GetProductById), new { id = product.Id }, product);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Lỗi xảy ra khi thêm mới sản phẩm");
            return StatusCode(StatusCodes.Status500InternalServerError, new { message = "Lỗi máy chủ nội bộ", detail = ex.Message });
        }
    }

    /// <summary>
    /// Cập nhật thông tin sản phẩm (Chỉ dành cho quyền Admin)
    /// </summary>
    [HttpPut("{id:int}")]
    [Authorize(Roles = "Admin")]
    public async Task<IActionResult> UpdateProduct(int id, [FromBody] Product product)
    {
        if (id != product.Id)
        {
            return BadRequest(new { message = "ID sản phẩm trong đường dẫn không khớp với dữ liệu gửi lên" });
        }

        if (!ModelState.IsValid)
        {
            return BadRequest(ModelState);
        }

        try
        {
            var existing = await _productRepo.GetProductByIdAsync(id);
            if (existing is null)
            {
                return NotFound(new { message = $"Sản phẩm ID = {id} không tồn tại" });
            }

            await _productRepo.UpdateProductAsync(product);
            return Ok(new { status = true, message = "Cập nhật sản phẩm thành công", data = product });
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Lỗi xảy ra khi cập nhật sản phẩm {Id}", id);
            return StatusCode(StatusCodes.Status500InternalServerError, new { message = "Lỗi máy chủ nội bộ", detail = ex.Message });
        }
    }

    /// <summary>
    /// Xóa sản phẩm khỏi hệ thống (Chỉ dành cho quyền Admin)
    /// </summary>
    [HttpDelete("{id:int}")]
    [Authorize(Roles = "Admin")]
    public async Task<IActionResult> DeleteProduct(int id)
    {
        try
        {
            var existingProduct = await _productRepo.GetProductByIdAsync(id);
            if (existingProduct is null)
            {
                return NotFound(new { message = $"Sản phẩm với ID = {id} không tồn tại" });
            }

            await _productRepo.DeleteProductAsync(id);
            return Ok(new { status = true, message = $"Đã xóa sản phẩm ID = {id} thành công" });
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Lỗi xảy ra khi xóa sản phẩm {Id}", id);
            return StatusCode(StatusCodes.Status500InternalServerError, new { message = "Lỗi máy chủ nội bộ", detail = ex.Message });
        }
    }
}
