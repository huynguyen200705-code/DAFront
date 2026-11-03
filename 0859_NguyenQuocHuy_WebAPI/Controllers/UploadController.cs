using Microsoft.AspNetCore.Mvc;

namespace _0859_NguyenQuocHuy_WebAPI.Controllers;

[Route("api/[controller]")]
[ApiController]
public class UploadController : ControllerBase
{
    private readonly IWebHostEnvironment _env;
    private readonly ILogger<UploadController> _logger;

    public UploadController(IWebHostEnvironment env, ILogger<UploadController> logger)
    {
        _env = env;
        _logger = logger;
    }

    /// <summary>
    /// Tải lên hình ảnh sản phẩm hoặc avatar
    /// </summary>
    [HttpPost("image")]
    public async Task<IActionResult> UploadImage(IFormFile? file)
    {
        if (file is null || file.Length == 0)
        {
            return BadRequest(new { status = false, message = "Vui lòng chọn tập tin hình ảnh" });
        }

        // Kiểm tra kích thước (tối đa 10MB)
        if (file.Length > 10 * 1024 * 1024)
        {
            return BadRequest(new { status = false, message = "Dung lượng ảnh không được vượt quá 10MB" });
        }

        var allowedExtensions = new[] { ".jpg", ".jpeg", ".png", ".webp", ".gif", ".svg" };
        var extension = Path.GetExtension(file.FileName).ToLowerInvariant();

        if (!allowedExtensions.Contains(extension))
        {
            return BadRequest(new { status = false, message = "Định dạng tập tin không được hỗ trợ. Chỉ chấp nhận .jpg, .jpeg, .png, .webp, .gif, .svg" });
        }

        try
        {
            var webRoot = _env.WebRootPath;
            if (string.IsNullOrEmpty(webRoot))
            {
                webRoot = Path.Combine(Directory.GetCurrentDirectory(), "wwwroot");
            }

            var uploadsFolder = Path.Combine(webRoot, "uploads");
            if (!Directory.Exists(uploadsFolder))
            {
                Directory.CreateDirectory(uploadsFolder);
            }

            var uniqueFileName = $"{Guid.NewGuid():N}{extension}";
            var filePath = Path.Combine(uploadsFolder, uniqueFileName);

            using (var stream = new FileStream(filePath, FileMode.Create))
            {
                await file.CopyToAsync(stream);
            }

            var fileUrl = $"/uploads/{uniqueFileName}";
            return Ok(new
            {
                status = true,
                message = "Tải ảnh lên thành công",
                url = fileUrl,
                fileName = uniqueFileName,
                size = file.Length
            });
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Lỗi xảy ra khi tải ảnh lên");
            return StatusCode(StatusCodes.Status500InternalServerError, new { status = false, message = "Không thể lưu tập tin", error = ex.Message });
        }
    }
}
