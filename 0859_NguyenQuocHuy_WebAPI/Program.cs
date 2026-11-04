using _0859_NguyenQuocHuy_WebAPI.Hubs;
using _0859_NguyenQuocHuy_WebAPI.Middleware;
using _0859_NguyenQuocHuy_WebAPI.Models;
using _0859_NguyenQuocHuy_WebAPI.Repositories;
using _0859_NguyenQuocHuy_WebAPI.Services;
using Microsoft.AspNetCore.Authentication.JwtBearer;
using Microsoft.AspNetCore.Identity;
using Microsoft.EntityFrameworkCore;
using Microsoft.IdentityModel.Tokens;
using Microsoft.OpenApi.Models;
using System.Net;
using System.Net.Sockets;
using System.Text;

var builder = WebApplication.CreateBuilder(args);

// Đảm bảo máy chủ lắng nghe trên tất cả các mạng (LAN, Wi-Fi, Internet tunnel)
builder.WebHost.UseUrls("http://0.0.0.0:5276");

// 1. Đăng ký CSDL — tự động dùng SQLite nếu UseSqlite=true hoặc SQL Server không khả dụng
var useSqlite = builder.Configuration.GetValue<bool>("UseSqlite");
var connectionString = builder.Configuration.GetConnectionString("DefaultConnection") ?? "";

builder.Services.AddDbContext<ApplicationDbContext>(options =>
{
    if (useSqlite || string.IsNullOrEmpty(connectionString))
    {
        // SQLite — lưu cố định tại thư mục ContentRoot của dự án để đảm bảo dữ liệu luôn đồng nhất
        var dbPath = Path.Combine(builder.Environment.ContentRootPath, "RestaurantPOS.db");
        options.UseSqlite($"Data Source={dbPath}");
    }
    else
    {
        options.UseSqlServer(connectionString);
    }
});

// 2. Cấu hình Identity quản lý tài khoản và phân quyền
builder.Services.AddIdentity<User, IdentityRole>(cfg =>
{
    cfg.Password.RequireDigit = false;
    cfg.Password.RequiredLength = 3;
    cfg.Password.RequireNonAlphanumeric = false;
    cfg.Password.RequireUppercase = false;
    cfg.Password.RequireLowercase = false;
    cfg.User.RequireUniqueEmail = false;
})
.AddEntityFrameworkStores<ApplicationDbContext>()
.AddDefaultTokenProviders();

// 3. Đăng ký Controllers & Repositories (Kèm IgnoreCycles tránh vòng lặp JSON)
builder.Services.AddControllers()
    .AddJsonOptions(options =>
    {
        options.JsonSerializerOptions.ReferenceHandler = System.Text.Json.Serialization.ReferenceHandler.IgnoreCycles;
        options.JsonSerializerOptions.DefaultIgnoreCondition = System.Text.Json.Serialization.JsonIgnoreCondition.WhenWritingNull;
    });

builder.Services.AddScoped<IproductRepository, ProductRepository>();
builder.Services.AddSingleton<IAppNotificationService, AppNotificationService>();
builder.Services.AddSignalR();
builder.Services.AddEndpointsApiExplorer();

// 4. Cấu hình Swagger UI hỗ trợ thử nghiệm JWT Bearer
builder.Services.AddSwaggerGen(c =>
{
    c.SwaggerDoc("v1", new OpenApiInfo 
    { 
        Title = "DA Restaurant API - Quản Lý Nhà Hàng & POS", 
        Version = "v1",
        Description = "ASP.NET Core Web API hoàn chỉnh quản lý toàn bộ Nhà Hàng & POS (Đồng bộ đa thiết bị, Real-time Notification qua SignalR): Quản lý Bàn ăn, Thực đơn món ăn, Danh mục & Sản phẩm ẩm thực, Đơn hàng & Hóa đơn POS, Nhân viên, Chấm công NFC/QR, Khách hàng tích điểm & Doanh thu, Phân quyền Identity JWT & Smart Auth OTP."
    });

    var jwtSecurityScheme = new OpenApiSecurityScheme
    {
        Name = "Authorization",
        Description = "Nhập mã Token theo định dạng: Bearer {token}",
        In = ParameterLocation.Header,
        Type = SecuritySchemeType.Http,
        Scheme = "Bearer",
        BearerFormat = "JWT",
        Reference = new OpenApiReference
        {
            Type = ReferenceType.SecurityScheme,
            Id = JwtBearerDefaults.AuthenticationScheme
        }
    };

    c.AddSecurityDefinition(JwtBearerDefaults.AuthenticationScheme, jwtSecurityScheme);
    c.AddSecurityRequirement(new OpenApiSecurityRequirement
    {
        { jwtSecurityScheme, Array.Empty<string>() }
    });
});

// 5. Cấu hình CORS mở toàn quyền cho Android App & Frontend
const string AppCorsPolicy = "MyAllowOrigins";
builder.Services.AddCors(corsOptions =>
{
    corsOptions.AddPolicy(AppCorsPolicy, policyBuilder =>
    {
        policyBuilder.SetIsOriginAllowed(_ => true)
                     .AllowAnyHeader()
                     .AllowAnyMethod()
                     .AllowCredentials();
    });
});

// 6. Cấu hình JWT Authentication & Authorization
var jwtSection = builder.Configuration.GetSection("JWTKey");
var jwtSecretKey = jwtSection["Secret"] ?? "AReallyLongSecretKeyForYourJWTTokenSigning123456!";
var signingKeyBytes = Encoding.UTF8.GetBytes(jwtSecretKey);

builder.Services.AddAuthentication(authOpts =>
{
    authOpts.DefaultAuthenticateScheme = JwtBearerDefaults.AuthenticationScheme;
    authOpts.DefaultChallengeScheme = JwtBearerDefaults.AuthenticationScheme;
})
.AddJwtBearer(JwtBearerDefaults.AuthenticationScheme, bearerOpts =>
{
    bearerOpts.RequireHttpsMetadata = false;
    bearerOpts.SaveToken = true;
    bearerOpts.TokenValidationParameters = new TokenValidationParameters
    {
        ValidateIssuer = true,
        ValidateAudience = true,
        ValidateLifetime = true,
        ValidateIssuerSigningKey = true,
        ValidIssuer = jwtSection["ValidIssuer"] ?? "YourIssuer",
        ValidAudience = jwtSection["ValidAudience"] ?? "YourAudience",
        IssuerSigningKey = new SymmetricSecurityKey(signingKeyBytes),
        ClockSkew = TimeSpan.Zero
    };
});

builder.Services.AddAuthorization();

var app = builder.Build();

// 7. Tự động khởi tạo Database và Seeding dữ liệu ban đầu
using (var scope = app.Services.CreateScope())
{
    var services = scope.ServiceProvider;
    try
    {
        var dbContext = services.GetRequiredService<ApplicationDbContext>();
        await dbContext.Database.EnsureCreatedAsync();
        await DatabaseSeeder.SeedAsync(dbContext);
        await InitializeSystemUsersAndRolesAsync(
            services.GetRequiredService<RoleManager<IdentityRole>>(),
            services.GetRequiredService<UserManager<User>>()
        );
    }
    catch (Exception ex)
    {
        var logger = services.GetRequiredService<ILogger<Program>>();
        logger.LogError(ex, "Lỗi xảy ra trong quá trình khởi tạo CSDL hoặc Seeder.");
    }
}

// 8. Cấu hình Pipeline phục vụ Static Files & Web Management Dashboard
app.UseDefaultFiles();
app.UseStaticFiles();

// 9. Cấu hình Swagger UI
app.UseSwagger();
app.UseSwaggerUI(c =>
{
    c.SwaggerEndpoint("/swagger/v1/swagger.json", "DA Restaurant API v1");
    c.RoutePrefix = "swagger";
    c.DocumentTitle = "DA Restaurant API - Quản Lý Nhà Hàng & POS";
    c.HeadContent = @"
        <style>
            .swagger-ui .topbar { background-color: #1e293b !important; }
            .topbar-wrapper a span { display: none !important; }
            .topbar-wrapper a:after {
                content: '🏪 DA Restaurant API — BẤM VÀO ĐÂY ĐỂ MỞ TRANG QUẢN LÝ WEB';
                color: #f59e0b;
                font-weight: 700;
                font-size: 15px;
            }
        </style>
        <script>
            document.addEventListener('DOMContentLoaded', function() {
                setTimeout(function() {
                    var link = document.querySelector('.topbar-wrapper a');
                    if (link) {
                        link.href = '/';
                        link.title = 'Mở Trang Web Quản Lý Hoạt Động API';
                    }
                }, 300);
            });
        </script>";
});

app.UseCors(AppCorsPolicy);

// Middleware ghi nhận mọi thao tác từ app và phát thông báo Real-time
app.UseMiddleware<AppActivityMiddleware>();

app.UseAuthentication();
app.UseAuthorization();

// SignalR Hub cho thông báo trực tiếp
app.MapHub<NotificationHub>("/hubs/notifications");

// Điều hướng trang admin
app.MapGet("/admin", () => Results.Redirect("/"));
app.MapGet("/dashboard", () => Results.Redirect("/"));

app.MapControllers();

// In thông tin mạng LAN để người dùng cấu hình app điện thoại kết nối đồng bộ
PrintServerNetworkInfo();

app.Run();

static void PrintServerNetworkInfo()
{
    try
    {
        var hostName = Dns.GetHostName();
        var ipAddresses = Dns.GetHostAddresses(hostName)
            .Where(ip => ip.AddressFamily == AddressFamily.InterNetwork && !IPAddress.IsLoopback(ip))
            .Select(ip => ip.ToString())
            .ToList();

        Console.ForegroundColor = ConsoleColor.Cyan;
        Console.WriteLine("\n============================================================");
        Console.WriteLine("🚀 DA RESTAURANT API ĐÃ SẴN SÀNG ĐỒNG BỘ ĐA THIẾT BỊ!");
        Console.WriteLine("============================================================");
        Console.ForegroundColor = ConsoleColor.White;
        Console.WriteLine("👉 Web Admin Dashboard: http://localhost:5276/");
        Console.WriteLine("👉 Tài liệu Swagger UI:  http://localhost:5276/swagger");
        Console.WriteLine("👉 SignalR Realtime Hub: http://localhost:5276/hubs/notifications");
        Console.ForegroundColor = ConsoleColor.Yellow;
        Console.WriteLine("\n📱 ĐƯỜNG DẪN KẾT NỐI TỪ ĐIỆN THOẠI / APP CÙNG MẠNG WI-FI:");
        foreach (var ip in ipAddresses)
        {
            Console.WriteLine($"   🔗 http://{ip}:5276/api/");
        }
        Console.ForegroundColor = ConsoleColor.Green;
        Console.WriteLine("🔔 Hệ thống thông báo tự động (Real-time App Notification): ĐANG BẬT");
        Console.WriteLine("============================================================\n");
        Console.ResetColor();
    }
    catch { }
}

// Hàm hỗ trợ khởi tạo vai trò và tài khoản người dùng ban đầu
static async Task InitializeSystemUsersAndRolesAsync(RoleManager<IdentityRole> roleMgr, UserManager<User> userMgr)
{
    var defaultRoles = new[] { "Admin", "User" };
    foreach (var roleName in defaultRoles)
    {
        if (!await roleMgr.RoleExistsAsync(roleName))
        {
            await roleMgr.CreateAsync(new IdentityRole(roleName));
        }
    }

    // Khởi tạo tài khoản Quản trị viên (Admin / 123456)
    var adminUser = await userMgr.FindByNameAsync("admin");
    if (adminUser is null)
    {
        adminUser = new User
        {
            UserName = "admin",
            Email = "admin@restaurant.com",
            Initials = "ADM"
        };
        var createAdminResult = await userMgr.CreateAsync(adminUser, "123456");
        if (createAdminResult.Succeeded)
        {
            await userMgr.AddToRoleAsync(adminUser, "Admin");
        }
    }

    // Khởi tạo tài khoản Người dùng phổ thông (user / 123456)
    var normalUser = await userMgr.FindByNameAsync("user");
    if (normalUser is null)
    {
        normalUser = new User
        {
            UserName = "user",
            Email = "user@restaurant.com",
            Initials = "USR"
        };
        var createUserResult = await userMgr.CreateAsync(normalUser, "123456");
        if (createUserResult.Succeeded)
        {
            await userMgr.AddToRoleAsync(normalUser, "User");
        }
    }
}
