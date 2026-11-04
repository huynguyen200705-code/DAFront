using _0859_NguyenQuocHuy_WebAPI.Services;
using _0859_NguyenQuocHuy_WebAPI.Models;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using Microsoft.IdentityModel.Tokens;
using System.Collections.Concurrent;
using System.IdentityModel.Tokens.Jwt;
using System.Security.Claims;
using System.Text;

namespace _0859_NguyenQuocHuy_WebAPI.Controllers;

[Route("api/[controller]")]
[ApiController]
public class AuthenticateController : ControllerBase
{
    private readonly UserManager<User> _userManager;
    private readonly RoleManager<IdentityRole> _roleManager;
    private readonly ApplicationDbContext _context;
    private readonly IAppNotificationService _notificationService;
    private readonly IConfiguration _config;
    private readonly ILogger<AuthenticateController> _logger;

    // Lưu trữ mã OTP Smart Auth tạm thời trong bộ nhớ kèm thời gian hết hạn
    private static readonly ConcurrentDictionary<string, (string Otp, DateTime Expiry)> _otpCache = new();

    public AuthenticateController(
        UserManager<User> userManager,
        RoleManager<IdentityRole> roleManager,
        ApplicationDbContext context,
        IAppNotificationService notificationService,
        IConfiguration config,
        ILogger<AuthenticateController> logger)
    {
        _userManager = userManager;
        _roleManager = roleManager;
        _context = context;
        _notificationService = notificationService;
        _config = config;
        _logger = logger;
    }

    /// <summary>
    /// Đăng ký tài khoản người dùng mới kèm phân quyền (Admin / User)
    /// </summary>
    [HttpPost("register")]
    public async Task<IActionResult> Register([FromBody] RegistrationModel request)
    {
        if (!ModelState.IsValid)
        {
            return BadRequest(ModelState);
        }

        // 1. Chuẩn hóa Email (tự động tạo nếu để trống từ App)
        var email = string.IsNullOrWhiteSpace(request.Email)
            ? $"{request.Username.Trim().ToLower()}@restaurant.com"
            : request.Email.Trim();

        // 2. Tự động tính chữ cái viết tắt (Initials) nếu để trống
        var initials = request.Initials?.Trim();
        if (string.IsNullOrEmpty(initials) && !string.IsNullOrWhiteSpace(request.FullName))
        {
            var parts = request.FullName.Trim().Split(' ', StringSplitOptions.RemoveEmptyEntries);
            initials = string.Concat(parts.Select(p => p[0])).ToUpper();
            if (initials.Length > 5) initials = initials.Substring(0, 5);
        }

        // 3. Kiểm tra xem tên đăng nhập đã tồn tại trong hệ thống Identity hoặc POS chưa
        var accountByUsername = await _userManager.FindByNameAsync(request.Username.Trim());
        if (accountByUsername is not null)
        {
            return BadRequest(new 
            { 
                status = false, 
                message = "Tên đăng nhập đã tồn tại trong hệ thống." 
            });
        }

        var accountByEmail = await _userManager.FindByEmailAsync(email);
        if (accountByEmail is not null)
        {
            if (string.IsNullOrWhiteSpace(request.Email))
            {
                email = $"{request.Username.Trim().ToLower()}_{Guid.NewGuid().ToString().Substring(0, 4)}@restaurant.com";
            }
            else
            {
                return BadRequest(new 
                { 
                    status = false, 
                    message = "Địa chỉ email đã được sử dụng." 
                });
            }
        }

        // 4. Khởi tạo tài khoản Identity chuẩn ASP.NET Core
        var newUser = new User
        {
            UserName = request.Username.Trim(),
            Email = email,
            Initials = initials
        };

        var creationResult = await _userManager.CreateAsync(newUser, request.Password);
        if (!creationResult.Succeeded)
        {
            var errorDetails = string.Join("; ", creationResult.Errors.Select(err => err.Description));
            _logger.LogWarning("Tạo tài khoản thất bại cho {Username}: {Errors}", request.Username, errorDetails);

            return StatusCode(StatusCodes.Status500InternalServerError, new 
            { 
                status = false, 
                message = $"Đăng ký tài khoản không thành công: {errorDetails}" 
            });
        }

        // 5. Chuẩn hóa và gán vai trò (ADMIN / USER)
        var roleUpper = (request.Role ?? "USER").Trim().ToUpper();
        var targetRole = roleUpper == "ADMIN" ? "Admin" : "User";
        if (!await _roleManager.RoleExistsAsync(targetRole))
        {
            await _roleManager.CreateAsync(new IdentityRole(targetRole));
        }
        await _userManager.AddToRoleAsync(newUser, targetRole);

        // 6. Đồng bộ lưu vào bảng restaurant_users của POS (đồng bộ 100% với App Android)
        var restUser = await _context.RestaurantUsers.FirstOrDefaultAsync(u => u.Username.ToLower() == request.Username.Trim().ToLower());
        if (restUser == null)
        {
            restUser = new RestaurantUser
            {
                Username = request.Username.Trim(),
                Password = request.Password,
                FullName = !string.IsNullOrWhiteSpace(request.FullName) ? request.FullName.Trim() : request.Username.Trim(),
                Phone = request.Phone?.Trim() ?? "",
                Role = roleUpper
            };
            _context.RestaurantUsers.Add(restUser);
            await _context.SaveChangesAsync();
        }

        // 7. Ghi nhận hoạt động và phát thông báo Real-time cho Web Admin Dashboard
        await _notificationService.RecordActivityAsync(new AppActivity
        {
            Action = "TÀI KHOẢN MỚI: @" + request.Username,
            Description = $"Đăng ký tài khoản mới: @{request.Username} ({request.FullName ?? "Người dùng"}) | Quyền: {roleUpper}",
            Category = "User",
            Source = "📱 App Android",
            Status = "SUCCESS",
            Timestamp = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds()
        });

        _logger.LogInformation("Người dùng {Username} đăng ký thành công với quyền {Role}", newUser.UserName, targetRole);
        return Ok(new 
        { 
            status = true, 
            message = "Tạo tài khoản người dùng thành công.",
            role = targetRole,
            username = newUser.UserName,
            fullName = request.FullName ?? request.Username,
            phone = request.Phone,
            email = email
        });
    }

    /// <summary>
    /// Xác thực đăng nhập và cấp phát JWT Bearer Token chứa quyền hạn
    /// </summary>
    [HttpPost("login")]
    public async Task<IActionResult> Login([FromBody] LoginModel request)
    {
        if (!ModelState.IsValid)
        {
            return BadRequest(ModelState);
        }

        // Tìm người dùng theo username trong hệ thống Identity hoặc POS
        var appUser = await _userManager.FindByNameAsync(request.Username.Trim());
        if (appUser is null || !await _userManager.CheckPasswordAsync(appUser, request.Password))
        {
            // Kiểm tra trong bảng restaurant_users của App POS
            var restUser = await _context.RestaurantUsers
                .FirstOrDefaultAsync(u => u.Username.ToLower() == request.Username.Trim().ToLower() && u.Password == request.Password);

            if (restUser != null)
            {
                var targetRole = restUser.Role.ToUpper() == "ADMIN" ? "Admin" : "User";
                if (appUser == null)
                {
                    appUser = new User
                    {
                        UserName = restUser.Username,
                        Email = $"{restUser.Username.ToLower()}@restaurant.com"
                    };
                    await _userManager.CreateAsync(appUser, request.Password);
                }
                else
                {
                    var resetToken = await _userManager.GeneratePasswordResetTokenAsync(appUser);
                    await _userManager.ResetPasswordAsync(appUser, resetToken, request.Password);
                }

                if (!await _roleManager.RoleExistsAsync(targetRole))
                {
                    await _roleManager.CreateAsync(new IdentityRole(targetRole));
                }
                if (!await _userManager.IsInRoleAsync(appUser, targetRole))
                {
                    await _userManager.AddToRoleAsync(appUser, targetRole);
                }
            }
            else
            {
                return Unauthorized(new 
                { 
                    status = false, 
                    message = "Tài khoản hoặc mật khẩu không chính xác." 
                });
            }
        }

        // Lấy danh sách vai trò
        var rolesList = await _userManager.GetRolesAsync(appUser);
        var primaryRole = rolesList.FirstOrDefault() ?? "User";

        // Khởi tạo Claims (Bao gồm cả ClaimTypes.Role và role claim ngắn chuẩn JWT cho Flutter)
        var tokenClaims = new List<Claim>
        {
            new(ClaimTypes.Name, appUser.UserName ?? string.Empty),
            new(ClaimTypes.NameIdentifier, appUser.Id),
            new(JwtRegisteredClaimNames.Jti, Guid.NewGuid().ToString()),
            new("id", appUser.Id),
            new("username", appUser.UserName ?? string.Empty),
            new("role", primaryRole)
        };

        if (!string.IsNullOrEmpty(appUser.Email))
        {
            tokenClaims.Add(new Claim(ClaimTypes.Email, appUser.Email));
            tokenClaims.Add(new Claim("email", appUser.Email));
        }

        if (!string.IsNullOrEmpty(appUser.Initials))
        {
            tokenClaims.Add(new Claim("initials", appUser.Initials));
        }

        foreach (var roleName in rolesList)
        {
            tokenClaims.Add(new Claim(ClaimTypes.Role, roleName));
        }

        var jwtToken = CreateJwtToken(tokenClaims);

        _logger.LogInformation("Người dùng {Username} ({Role}) đăng nhập thành công", appUser.UserName, primaryRole);

        return Ok(new
        {
            status = true,
            message = "Đăng nhập thành công",
            token = jwtToken,
            role = primaryRole,
            roles = rolesList,
            username = appUser.UserName,
            userId = appUser.Id,
            email = appUser.Email,
            initials = appUser.Initials
        });
    }

    /// <summary>
    /// Đăng xuất khỏi hệ thống
    /// </summary>
    [HttpPost("logout")]
    public IActionResult Logout()
    {
        return Ok(new
        {
            status = true,
            message = "Đăng xuất thành công. Vui lòng xóa Token ở phía Client/SharedPreferences."
        });
    }

    /// <summary>
    /// Lấy thông tin tài khoản hiện tại từ JWT Token đã đăng nhập
    /// </summary>
    [HttpGet("me")]
    [Authorize]
    public async Task<IActionResult> GetCurrentUserProfile()
    {
        var username = User.Identity?.Name;
        if (string.IsNullOrEmpty(username))
        {
            return Unauthorized(new { status = false, message = "Không xác định được danh tính người dùng" });
        }

        var appUser = await _userManager.FindByNameAsync(username);
        if (appUser is null)
        {
            return NotFound(new { status = false, message = "Không tìm thấy thông tin tài khoản" });
        }

        var roles = await _userManager.GetRolesAsync(appUser);

        return Ok(new
        {
            status = true,
            data = new UserDto
            {
                Id = appUser.Id,
                UserName = appUser.UserName ?? string.Empty,
                Email = appUser.Email,
                PhoneNumber = appUser.PhoneNumber,
                Initials = appUser.Initials,
                Roles = roles
            }
        });
    }

    /// <summary>
    /// Cập nhật thông tin cá nhân (email, chữ cái viết tắt, số điện thoại)
    /// </summary>
    [HttpPost("update-profile")]
    [Authorize]
    public async Task<IActionResult> UpdateProfile([FromBody] UpdateProfileModel request)
    {
        if (!ModelState.IsValid)
        {
            return BadRequest(ModelState);
        }

        var username = User.Identity?.Name;
        if (string.IsNullOrEmpty(username))
        {
            return Unauthorized();
        }

        var user = await _userManager.FindByNameAsync(username);
        if (user is null)
        {
            return NotFound(new { status = false, message = "Người dùng không tồn tại" });
        }

        if (!string.IsNullOrWhiteSpace(request.Email))
        {
            user.Email = request.Email.Trim();
        }
        if (!string.IsNullOrWhiteSpace(request.Initials))
        {
            user.Initials = request.Initials.Trim();
        }
        if (!string.IsNullOrWhiteSpace(request.PhoneNumber))
        {
            user.PhoneNumber = request.PhoneNumber.Trim();
        }

        var result = await _userManager.UpdateAsync(user);
        if (!result.Succeeded)
        {
            return BadRequest(new { status = false, message = "Cập nhật hồ sơ thất bại", errors = result.Errors });
        }

        return Ok(new { status = true, message = "Cập nhật thông tin thành công" });
    }

    /// <summary>
    /// Đổi mật khẩu tài khoản đang đăng nhập
    /// </summary>
    [HttpPost("change-password")]
    [Authorize]
    public async Task<IActionResult> ChangePassword([FromBody] ChangePasswordModel request)
    {
        if (!ModelState.IsValid)
        {
            return BadRequest(ModelState);
        }

        var username = User.Identity?.Name;
        if (string.IsNullOrEmpty(username))
        {
            return Unauthorized();
        }

        var user = await _userManager.FindByNameAsync(username);
        if (user is null)
        {
            return NotFound(new { status = false, message = "Người dùng không tồn tại" });
        }

        var result = await _userManager.ChangePasswordAsync(user, request.CurrentPassword, request.NewPassword);
        if (!result.Succeeded)
        {
            var errors = string.Join(", ", result.Errors.Select(e => e.Description));
            return BadRequest(new { status = false, message = $"Đổi mật khẩu thất bại: {errors}" });
        }

        return Ok(new { status = true, message = "Đổi mật khẩu thành công" });
    }

    /// <summary>
    /// Gửi mã xác thực OTP (Hỗ trợ Smart Auth trên thiết bị di động Android / Flutter)
    /// </summary>
    [HttpPost("send-otp")]
    public IActionResult SendOtp([FromBody] SendOtpModel request)
    {
        if (!ModelState.IsValid)
        {
            return BadRequest(ModelState);
        }

        var cleanPhone = request.PhoneNumber.Trim();
        // Tạo mã ngẫu nhiên 6 chữ số
        var otpCode = Random.Shared.Next(100000, 999999).ToString();
        var expiry = DateTime.UtcNow.AddMinutes(5);

        _otpCache[cleanPhone] = (otpCode, expiry);
        _logger.LogInformation("Mã Smart Auth OTP cho SĐT {Phone}: {Otp} (Hết hạn lúc {Expiry})", cleanPhone, otpCode, expiry);

        return Ok(new
        {
            status = true,
            message = $"Mã OTP Smart Auth đã được gửi đến {cleanPhone}",
            phoneNumber = cleanPhone,
            otp = otpCode, // Trả về OTP phục vụ việc test nhanh trên Swagger và Smart Auth
            expiresInSeconds = 300
        });
    }

    /// <summary>
    /// Xác thực mã OTP Smart Auth để đăng nhập hoặc tự động tạo tài khoản theo SĐT
    /// </summary>
    [HttpPost("verify-otp")]
    public async Task<IActionResult> VerifyOtp([FromBody] VerifyOtpModel request)
    {
        if (!ModelState.IsValid)
        {
            return BadRequest(ModelState);
        }

        var cleanPhone = request.PhoneNumber.Trim();
        if (!_otpCache.TryGetValue(cleanPhone, out var cached) || cached.Expiry < DateTime.UtcNow)
        {
            return BadRequest(new { status = false, message = "Mã OTP đã hết hạn hoặc không tồn tại. Vui lòng gửi lại." });
        }

        if (cached.Otp != request.OtpCode.Trim())
        {
            return BadRequest(new { status = false, message = "Mã OTP không chính xác." });
        }

        // Xóa mã đã dùng
        _otpCache.TryRemove(cleanPhone, out _);

        // Tìm hoặc tự động tạo tài khoản theo SĐT
        var user = await _userManager.FindByNameAsync(cleanPhone);
        if (user is null)
        {
            user = new User
            {
                UserName = cleanPhone,
                PhoneNumber = cleanPhone,
                Email = $"{cleanPhone}@smartauth.local",
                Initials = cleanPhone.Length >= 3 ? cleanPhone[^3..] : "OTP"
            };

            var createRes = await _userManager.CreateAsync(user, "SmartAuth@123");
            if (createRes.Succeeded)
            {
                if (!await _roleManager.RoleExistsAsync("User"))
                {
                    await _roleManager.CreateAsync(new IdentityRole("User"));
                }
                await _userManager.AddToRoleAsync(user, "User");
            }
        }

        var roles = await _userManager.GetRolesAsync(user);
        var primaryRole = roles.FirstOrDefault() ?? "User";

        var claims = new List<Claim>
        {
            new(ClaimTypes.Name, user.UserName ?? cleanPhone),
            new(ClaimTypes.NameIdentifier, user.Id),
            new(JwtRegisteredClaimNames.Jti, Guid.NewGuid().ToString()),
            new("role", primaryRole),
            new("username", user.UserName ?? cleanPhone)
        };
        foreach (var r in roles)
        {
            claims.Add(new Claim(ClaimTypes.Role, r));
        }

        var token = CreateJwtToken(claims);

        return Ok(new
        {
            status = true,
            message = "Xác thực Smart Auth thành công",
            token,
            role = primaryRole,
            username = user.UserName
        });
    }

    // ==========================================
    // Quản lý người dùng và phân quyền (Admin Only)
    // ==========================================

    /// <summary>
    /// Lấy danh sách toàn bộ người dùng trong hệ thống (Dành cho Admin)
    /// </summary>
    [HttpGet("users")]
    [Authorize(Roles = "Admin")]
    public async Task<IActionResult> GetAllUsers()
    {
        var usersList = await _userManager.Users.ToListAsync();
        var result = new List<UserDto>();

        foreach (var u in usersList)
        {
            var userRoles = await _userManager.GetRolesAsync(u);
            result.Add(new UserDto
            {
                Id = u.Id,
                UserName = u.UserName ?? string.Empty,
                Email = u.Email,
                PhoneNumber = u.PhoneNumber,
                Initials = u.Initials,
                Roles = userRoles
            });
        }

        return Ok(new { status = true, data = result });
    }

    /// <summary>
    /// Lấy chi tiết thông tin một tài khoản người dùng theo ID (Dành cho Admin)
    /// </summary>
    [HttpGet("users/{id}")]
    [Authorize(Roles = "Admin")]
    public async Task<IActionResult> GetUserById(string id)
    {
        var user = await _userManager.FindByIdAsync(id);
        if (user is null)
        {
            return NotFound(new { status = false, message = "Không tìm thấy người dùng" });
        }

        var roles = await _userManager.GetRolesAsync(user);
        return Ok(new
        {
            status = true,
            data = new UserDto
            {
                Id = user.Id,
                UserName = user.UserName ?? string.Empty,
                Email = user.Email,
                PhoneNumber = user.PhoneNumber,
                Initials = user.Initials,
                Roles = roles
            }
        });
    }

    /// <summary>
    /// Cập nhật vai trò phân quyền cho tài khoản (Admin / User)
    /// </summary>
    [HttpPut("users/{id}/role")]
    [Authorize(Roles = "Admin")]
    public async Task<IActionResult> UpdateUserRole(string id, [FromBody] UpdateUserRoleModel model)
    {
        var user = await _userManager.FindByIdAsync(id);
        if (user is null)
        {
            return NotFound(new { status = false, message = "Không tìm thấy tài khoản để cập nhật" });
        }

        var targetRole = model.Role.Trim();
        if (!await _roleManager.RoleExistsAsync(targetRole))
        {
            await _roleManager.CreateAsync(new IdentityRole(targetRole));
        }

        var currentRoles = await _userManager.GetRolesAsync(user);
        await _userManager.RemoveFromRolesAsync(user, currentRoles);
        await _userManager.AddToRoleAsync(user, targetRole);

        return Ok(new
        {
            status = true,
            message = $"Đã cập nhật vai trò của tài khoản '{user.UserName}' thành '{targetRole}'"
        });
    }

    /// <summary>
    /// Xóa tài khoản người dùng khỏi hệ thống (Dành cho Admin)
    /// </summary>
    [HttpDelete("users/{id}")]
    [Authorize(Roles = "Admin")]
    public async Task<IActionResult> DeleteUser(string id)
    {
        var user = await _userManager.FindByIdAsync(id);
        if (user is null)
        {
            return NotFound(new { status = false, message = "Người dùng không tồn tại" });
        }

        if (user.UserName?.ToLower() == "admin" || user.UserName == User.Identity?.Name)
        {
            return BadRequest(new { status = false, message = "Không thể tự xóa tài khoản Admin đang hoạt động" });
        }

        var result = await _userManager.DeleteAsync(user);
        if (!result.Succeeded)
        {
            return BadRequest(new { status = false, message = "Không thể xóa tài khoản", errors = result.Errors });
        }

        return Ok(new { status = true, message = $"Đã xóa thành công tài khoản '{user.UserName}'" });
    }

    /// <summary>
    /// Lấy danh sách tất cả các vai trò hiện có trong hệ thống (Dành cho Admin)
    /// </summary>
    [HttpGet("roles")]
    [Authorize(Roles = "Admin")]
    public async Task<IActionResult> GetRoles()
    {
        var roles = await _roleManager.Roles.Select(r => r.Name).ToListAsync();
        return Ok(new { status = true, data = roles });
    }

    /// <summary>
    /// Trợ hàm ký và tạo chuỗi JWT Token
    /// </summary>
    private string CreateJwtToken(IEnumerable<Claim> claims)
    {
        var jwtConfig = _config.GetSection("JWTKey");
        var secretKey = jwtConfig["Secret"] ?? "AReallyLongSecretKeyForYourJWTTokenSigning123456!";
        var symmetricKey = new SymmetricSecurityKey(Encoding.UTF8.GetBytes(secretKey));

        var expiryHours = double.TryParse(jwtConfig["TokenExpiryTimeInHour"], out var parsedHours) 
            ? parsedHours 
            : 24.0;

        var tokenDescriptor = new SecurityTokenDescriptor
        {
            Subject = new ClaimsIdentity(claims),
            Expires = DateTime.UtcNow.AddHours(expiryHours),
            Issuer = jwtConfig["ValidIssuer"] ?? "YourIssuer",
            Audience = jwtConfig["ValidAudience"] ?? "YourAudience",
            SigningCredentials = new SigningCredentials(symmetricKey, SecurityAlgorithms.HmacSha256Signature)
        };

        var handler = new JwtSecurityTokenHandler();
        var securityToken = handler.CreateToken(tokenDescriptor);
        return handler.WriteToken(securityToken);
    }
}
