using Microsoft.AspNetCore.Identity.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore;

namespace _0859_NguyenQuocHuy_WebAPI.Models;

/// <summary>
/// DbContext quản lý kết nối CSDL và các tập thực thể
/// </summary>
public class ApplicationDbContext : IdentityDbContext<User>
{
    public ApplicationDbContext(DbContextOptions<ApplicationDbContext> options) 
        : base(options) 
    { 
    }

    // Existing products & categories
    public DbSet<Product> Products => Set<Product>();
    public DbSet<Category> Categories => Set<Category>();

    // Restaurant POS DbSets matching Android Demo App
    public DbSet<RestaurantUser> RestaurantUsers => Set<RestaurantUser>();
    public DbSet<Customer> Customers => Set<Customer>();
    public DbSet<Employee> Employees => Set<Employee>();
    public DbSet<AttendanceRecord> AttendanceRecords => Set<AttendanceRecord>();
    public DbSet<RestaurantTable> RestaurantTables => Set<RestaurantTable>();
    public DbSet<MenuItem> MenuItems => Set<MenuItem>();
    public DbSet<Order> Orders => Set<Order>();
    public DbSet<OrderItem> OrderItems => Set<OrderItem>();
    public DbSet<ActiveTableOrder> ActiveTableOrders => Set<ActiveTableOrder>();
    public DbSet<AppActivity> AppActivities => Set<AppActivity>();

    protected override void OnModelCreating(ModelBuilder builder)
    {
        base.OnModelCreating(builder);

        // Cấu hình thuộc tính cho User mở rộng
        builder.Entity<User>(entity =>
        {
            entity.Property(u => u.Initials)
                  .HasMaxLength(5);
        });

        // Thiết lập quan hệ 1 - N giữa Category và Product
        builder.Entity<Product>(entity =>
        {
            entity.HasOne(p => p.Category)
                  .WithMany(c => c.Products)
                  .HasForeignKey(p => p.CategoryId)
                  .OnDelete(DeleteBehavior.SetNull);
        });

        // Cấu hình Order và OrderItem
        builder.Entity<Order>(entity =>
        {
            entity.HasMany(o => o.Items)
                  .WithOne()
                  .HasForeignKey(i => i.OrderId)
                  .OnDelete(DeleteBehavior.Cascade);
        });
    }
}
