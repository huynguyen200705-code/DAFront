using _0859_NguyenQuocHuy_WebAPI.Models;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using Microsoft.IdentityModel.Tokens;
using System.IdentityModel.Tokens.Jwt;
using System.Security.Claims;
using System.Text;

namespace _0859_NguyenQuocHuy_WebAPI.Controllers;

[Route("api/restaurant-auth")]
[ApiController]
public class RestaurantAuthController : ControllerBase
{
    private readonly ApplicationDbContext _context;
    private readonly IConfiguration _config;
    private readonly ILogger<RestaurantAuthController> _logger;

    public RestaurantAuthController(
        ApplicationDbContext context,
        IConfiguration config,
        ILogger<RestaurantAuthController> logger)
    {
        _context = context;
        _config = config;
        _logger = logger;
    }

    [HttpPost("login")]
    public async Task<IActionResult> Login([FromBody] RestaurantLoginRequest request)
    {
        if (string.IsNullOrWhiteSpace(request.Username) || string.IsNullOrWhiteSpace(request.Password))
        {
            return BadRequest(new { success = false, message = "Vui lòng nhập tên đăng nhập và mật khẩu." });
        }

        var user = await _context.RestaurantUsers
            .FirstOrDefaultAsync(u => u.Username.ToLower() == request.Username.Trim().ToLower() && u.Password == request.Password);

        if (user == null)
        {
            return Unauthorized(new { success = false, message = "Sai tên đăng nhập hoặc mật khẩu." });
        }

        var token = GenerateJwtToken(user);
        return Ok(new
        {
            success = true,
            message = "Đăng nhập thành công",
            user = new
            {
                id = user.Id,
                username = user.Username,
                fullName = user.FullName,
                phone = user.Phone,
                role = user.Role
            },
            token
        });
    }

    [HttpPost("register")]
    public async Task<IActionResult> Register([FromBody] RestaurantRegisterRequest request)
    {
        if (string.IsNullOrWhiteSpace(request.Username) || string.IsNullOrWhiteSpace(request.Password))
        {
            return BadRequest(new { success = false, message = "Tên đăng nhập và mật khẩu không được để trống." });
        }

        var exists = await _context.RestaurantUsers.AnyAsync(u => u.Username.ToLower() == request.Username.Trim().ToLower());
        if (exists)
        {
            return BadRequest(new { success = false, message = "Tên đăng nhập đã tồn tại." });
        }

        var newUser = new RestaurantUser
        {
            Username = request.Username.Trim(),
            Password = request.Password,
            FullName = string.IsNullOrWhiteSpace(request.FullName) ? request.Username : request.FullName.Trim(),
            Phone = request.Phone?.Trim() ?? string.Empty,
            Role = string.IsNullOrWhiteSpace(request.Role) ? "USER" : request.Role.Trim().ToUpper()
        };

        _context.RestaurantUsers.Add(newUser);
        await _context.SaveChangesAsync();

        return Ok(new
        {
            success = true,
            message = "Đăng ký tài khoản thành công",
            user = new
            {
                id = newUser.Id,
                username = newUser.Username,
                fullName = newUser.FullName,
                phone = newUser.Phone,
                role = newUser.Role
            }
        });
    }

    [HttpGet("users")]
    public async Task<IActionResult> GetUsers()
    {
        var users = await _context.RestaurantUsers
            .Select(u => new
            {
                u.Id,
                u.Username,
                u.FullName,
                u.Phone,
                u.Role
            })
            .ToListAsync();

        return Ok(new { success = true, data = users });
    }

    private string GenerateJwtToken(RestaurantUser user)
    {
        var jwtConfig = _config.GetSection("JWTKey");
        var secretKey = jwtConfig["Secret"] ?? "AReallyLongSecretKeyForYourJWTTokenSigning123456!";
        var symmetricKey = new SymmetricSecurityKey(Encoding.UTF8.GetBytes(secretKey));

        var claims = new List<Claim>
        {
            new(ClaimTypes.Name, user.Username),
            new(ClaimTypes.NameIdentifier, user.Id.ToString()),
            new(ClaimTypes.Role, user.Role),
            new("FullName", user.FullName)
        };

        var tokenDescriptor = new SecurityTokenDescriptor
        {
            Subject = new ClaimsIdentity(claims),
            Expires = DateTime.UtcNow.AddHours(24),
            Issuer = jwtConfig["ValidIssuer"] ?? "YourIssuer",
            Audience = jwtConfig["ValidAudience"] ?? "YourAudience",
            SigningCredentials = new SigningCredentials(symmetricKey, SecurityAlgorithms.HmacSha256Signature)
        };

        var handler = new JwtSecurityTokenHandler();
        return handler.WriteToken(handler.CreateToken(tokenDescriptor));
    }
}
