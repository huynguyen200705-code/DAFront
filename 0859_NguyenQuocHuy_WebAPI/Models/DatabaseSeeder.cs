using Microsoft.EntityFrameworkCore;

namespace _0859_NguyenQuocHuy_WebAPI.Models;

public static class DatabaseSeeder
{
    public static async Task SeedAsync(ApplicationDbContext context)
    {
        // 1. Seed Users
        if (!await context.RestaurantUsers.AnyAsync())
        {
            context.RestaurantUsers.AddRange(
                new RestaurantUser
                {
                    Username = "admin",
                    Password = "123", // Match demo app default test credentials
                    FullName = "Quản Trị Viên",
                    Phone = "0901234567",
                    Role = "ADMIN"
                },
                new RestaurantUser
                {
                    Username = "staff",
                    Password = "123",
                    FullName = "Nhân Viên Thu Ngân",
                    Phone = "0909876543",
                    Role = "USER"
                }
            );
            await context.SaveChangesAsync();
        }

        // 2. Seed Customers
        if (!await context.Customers.AnyAsync())
        {
            context.Customers.AddRange(
                new Customer { Name = "Nguyen Van A", Phone = "0911111111", Email = "a@gmail.com", TotalSpent = 2000000.0, LoyaltyPoints = 20 },
                new Customer { Name = "Tran Thi B", Phone = "0922222222", Email = "b@gmail.com", TotalSpent = 7500000.0, LoyaltyPoints = 75 },
                new Customer { Name = "Le Van C", Phone = "0933333333", Email = "c@gmail.com", TotalSpent = 18000000.0, LoyaltyPoints = 180 },
                new Customer { Name = "Pham Thi D", Phone = "0944444444", Email = "d@gmail.com", TotalSpent = 35000000.0, LoyaltyPoints = 350 },
                new Customer { Name = "Hoang Van E", Phone = "0955555555", Email = "e@gmail.com", TotalSpent = 60000000.0, LoyaltyPoints = 600 }
            );
            await context.SaveChangesAsync();
        }

        // 3. Seed Employees
        if (!await context.Employees.AnyAsync())
        {
            context.Employees.AddRange(
                new Employee
                {
                    Name = "Nguyen Van Binh",
                    Phone = "098111222",
                    Position = "Phục vụ",
                    Tier = "THU_VIEC",
                    ContractType = "HOURLY",
                    HourlyRate = 25000.0,
                    MonthlySalary = 0.0,
                    WorkedHours = 120.0,
                    WorkedDays = 0.0,
                    Bonus = 200000.0,
                    Penalty = 0.0
                },
                new Employee
                {
                    Name = "Tran Thi Hoa",
                    Phone = "098333444",
                    Position = "Thu ngân",
                    Tier = "CHINH_THUC",
                    ContractType = "MONTHLY",
                    HourlyRate = 0.0,
                    MonthlySalary = 8500000.0,
                    WorkedHours = 0.0,
                    WorkedDays = 24.0,
                    Bonus = 500000.0,
                    Penalty = 100000.0
                },
                new Employee
                {
                    Name = "Le Hoang Nam",
                    Phone = "098555666",
                    Position = "Bếp trưởng",
                    Tier = "QUAN_LY",
                    ContractType = "MONTHLY",
                    HourlyRate = 0.0,
                    MonthlySalary = 15000000.0,
                    WorkedHours = 0.0,
                    WorkedDays = 26.0,
                    Bonus = 1500000.0,
                    Penalty = 0.0
                },
                new Employee
                {
                    Name = "Pham Minh Duc",
                    Phone = "098777888",
                    Position = "Giám đốc VH",
                    Tier = "GIAM_DOC",
                    ContractType = "MONTHLY",
                    HourlyRate = 0.0,
                    MonthlySalary = 30000000.0,
                    WorkedHours = 0.0,
                    WorkedDays = 26.0,
                    Bonus = 5000000.0,
                    Penalty = 0.0
                }
            );
            await context.SaveChangesAsync();
        }

        // 4. Seed Menu Items
        if (!await context.MenuItems.AnyAsync())
        {
            context.MenuItems.AddRange(
                new MenuItem { Name = "Gỏi ngó sen tôm thịt", Category = "Khai vị", Price = 85000.0, Description = "Tôm tươi, thịt ba chỉ, ngó sen giòn", IsAvailable = true },
                new MenuItem { Name = "Chả giò hải sản", Category = "Khai vị", Price = 75000.0, Description = "Giòn rụm châm sốt mayonaise", IsAvailable = true },
                new MenuItem { Name = "Lẩu thái hải sản", Category = "Món chính", Price = 290000.0, Description = "Đậm đà vị chua cay tôm mực nấm", IsAvailable = true },
                new MenuItem { Name = "Bò lúc lắc hạt điều", Category = "Món chính", Price = 165000.0, Description = "Thịt bò Úc mềm xào rau củ", IsAvailable = true },
                new MenuItem { Name = "Cơm chiên hải sản", Category = "Món chính", Price = 110000.0, Description = "Hạt cơm vàng óng tôm mực", IsAvailable = true },
                new MenuItem { Name = "Trà đào cam sả", Category = "Đồ uống", Price = 45000.0, Description = "Thơm mát thanh nhiệt", IsAvailable = true },
                new MenuItem { Name = "Cà phê sữa đá", Category = "Đồ uống", Price = 35000.0, Description = "Đậm đà phong cách Việt", IsAvailable = true },
                new MenuItem { Name = "Chè khúc bạch", Category = "Tráng miệng", Price = 40000.0, Description = "Mát lạnh ngọt thanh", IsAvailable = true }
            );
            await context.SaveChangesAsync();
        }

        // 5. Seed Tables
        if (!await context.RestaurantTables.AnyAsync())
        {
            context.RestaurantTables.AddRange(
                new RestaurantTable { Name = "Bàn 101", Floor = "Tầng 1", Seats = 4, Status = "TRONG" },
                new RestaurantTable { Name = "Bàn 102", Floor = "Tầng 1", Seats = 4, Status = "TRONG" },
                new RestaurantTable { Name = "Bàn 103", Floor = "Tầng 1", Seats = 6, Status = "DA_DAT" },
                new RestaurantTable { Name = "Bàn 201", Floor = "Tầng 2", Seats = 4, Status = "TRONG" },
                new RestaurantTable { Name = "Bàn 202", Floor = "Tầng 2", Seats = 8, Status = "DANG_PHUC_VU" },
                new RestaurantTable { Name = "Phòng VIP 1", Floor = "Phòng VIP", Seats = 12, Status = "TRONG" },
                new RestaurantTable { Name = "Phòng VIP 2", Floor = "Phòng VIP", Seats = 10, Status = "TRONG" }
            );
            await context.SaveChangesAsync();
        }

        // 6. Seed Attendance Records
        if (!await context.AttendanceRecords.AnyAsync())
        {
            var now = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();
            var oneHourAgo = now - 3600000L * 4;
            var twoHoursAgo = now - 3600000L * 5;

            context.AttendanceRecords.AddRange(
                new AttendanceRecord
                {
                    EmployeeId = 1,
                    EmployeeName = "Nguyen Van Binh",
                    Type = "CHECK_IN",
                    Method = "NFC",
                    Timestamp = twoHoursAgo,
                    DurationHours = 0.0,
                    Note = "Chấm công thẻ NFC ca sáng"
                },
                new AttendanceRecord
                {
                    EmployeeId = 2,
                    EmployeeName = "Tran Thi Hoa",
                    Type = "CHECK_IN",
                    Method = "QR_CODE",
                    Timestamp = oneHourAgo,
                    DurationHours = 0.0,
                    Note = "Quét mã QR nhân viên"
                },
                new AttendanceRecord
                {
                    EmployeeId = 3,
                    EmployeeName = "Le Hoang Nam",
                    Type = "CHECK_IN",
                    Method = "NFC",
                    Timestamp = twoHoursAgo,
                    DurationHours = 0.0,
                    Note = "Chạm NFC bếp trưởng"
                }
            );
            await context.SaveChangesAsync();
        }

        // 7. Seed Sample Completed Orders (for analytics)
        if (!await context.Orders.AnyAsync())
        {
            var now = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();
            var sampleOrder1 = new Order
            {
                TableId = 1,
                TableName = "Bàn 101",
                CustomerId = 2,
                CustomerName = "Tran Thi B",
                CustomerTierName = "Bậc Bạc",
                EmployeeId = 2,
                EmployeeName = "Tran Thi Hoa",
                Subtotal = 425000.0,
                DiscountPercent = 5.0,
                PaymentMethod = "Tiền mặt",
                Status = "COMPLETED",
                Timestamp = now - 7200000L,
                Items = new List<OrderItem>
                {
                    new OrderItem { MenuItemId = 3, MenuItemName = "Lẩu thái hải sản", Price = 290000.0, Quantity = 1 },
                    new OrderItem { MenuItemId = 6, MenuItemName = "Trà đào cam sả", Price = 45000.0, Quantity = 3 }
                }
            };

            var sampleOrder2 = new Order
            {
                TableId = 4,
                TableName = "Bàn 201",
                CustomerId = 3,
                CustomerName = "Le Van C",
                CustomerTierName = "Bậc Vàng",
                EmployeeId = 1,
                EmployeeName = "Nguyen Van Binh",
                Subtotal = 275000.0,
                DiscountPercent = 10.0,
                PaymentMethod = "VietQR",
                Status = "COMPLETED",
                Timestamp = now - 3600000L,
                Items = new List<OrderItem>
                {
                    new OrderItem { MenuItemId = 4, MenuItemName = "Bò lúc lắc hạt điều", Price = 165000.0, Quantity = 1 },
                    new OrderItem { MenuItemId = 5, MenuItemName = "Cơm chiên hải sản", Price = 110000.0, Quantity = 1 }
                }
            };

            context.Orders.AddRange(sampleOrder1, sampleOrder2);
            await context.SaveChangesAsync();
        }

        // 8. Seed Categories & Products (Danh mục & Món ăn nhà hàng)
        if (!await context.Categories.AnyAsync())
        {
            var catAppetizer = new Category { Name = "Món Khai Vị", Description = "Các món gỏi, chả giò và salad thanh mát khai vị kích thích vị giác" };
            var catMainCourse = new Category { Name = "Món Chính & Đặc Sản", Description = "Các món bò, heo, gà và cơm chiên hoàng kim đặc sản nhà hàng" };
            var catSeafood = new Category { Name = "Hải Sản Tươi Sống", Description = "Tôm, cua, mực tươi ngon hấp dẫn được chế biến công phu" };
            var catHotpot = new Category { Name = "Lẩu & Nướng Than Hoa", Description = "Lẩu chua cay, lẩu nấm thanh đạm và các món nướng than hoa thơm lừng" };
            var catBeverage = new Category { Name = "Đồ Uống & Tráng Miệng", Description = "Trà hoa quả nhiệt đới, nước ép nguyên chất và chè thanh mát" };

            context.Categories.AddRange(catAppetizer, catMainCourse, catSeafood, catHotpot, catBeverage);
            await context.SaveChangesAsync();

            if (!await context.Products.AnyAsync())
            {
                context.Products.AddRange(
                    new Product
                    {
                        Name = "Gỏi Ngó Sen Tôm Thịt Hoàng Kim",
                        Price = 85000m,
                        Description = "Tôm thẻ tươi, thịt ba chỉ, ngó sen giòn hòa quyện nước mắm chua ngọt",
                        Image = "https://images.unsplash.com/photo-1540420773420-3366772f4999?w=600&auto=format&fit=crop",
                        CategoryId = catAppetizer.Id
                    },
                    new Product
                    {
                        Name = "Chả Giò Hải Sản Sốt Mayonnaise",
                        Price = 75000m,
                        Description = "Vỏ giòn rụm, nhân tôm mực thịt béo ngậy chấm cùng sốt mayonnaise béo thơm",
                        Image = "https://images.unsplash.com/photo-1563245372-f21724e3856d?w=600&auto=format&fit=crop",
                        CategoryId = catAppetizer.Id
                    },
                    new Product
                    {
                        Name = "Bò Úc Lúc Lắc Hạt Điều",
                        Price = 165000m,
                        Description = "Thịt bò thăn Úc mềm mọng xào rau củ tươi cùng hạt điều rang bùi béo",
                        Image = "https://images.unsplash.com/photo-1544025162-d76694265947?w=600&auto=format&fit=crop",
                        CategoryId = catMainCourse.Id
                    },
                    new Product
                    {
                        Name = "Cơm Chiên Hải Sản Dương Châu",
                        Price = 110000m,
                        Description = "Hạt cơm vàng óng ánh tơi xốp, tôm mực tươi ngon, lạp xưởng và đậu Hà Lan",
                        Image = "https://images.unsplash.com/photo-1603133872878-684f208fb84b?w=600&auto=format&fit=crop",
                        CategoryId = catMainCourse.Id
                    },
                    new Product
                    {
                        Name = "Lẩu Thái Chua Cay Hải Sản Thập Cẩm",
                        Price = 290000m,
                        Description = "Nước dùng lẩu Tomyum đậm đà chua cay cùng tôm sú, mực ống, nấm kim châm",
                        Image = "https://images.unsplash.com/photo-1569718212165-3a8278d5f624?w=600&auto=format&fit=crop",
                        CategoryId = catHotpot.Id
                    },
                    new Product
                    {
                        Name = "Tôm Sú Nướng Muối Ớt Xanh",
                        Price = 180000m,
                        Description = "Tôm sú loại lớn tươi sống ướp muối ớt nướng than hoa giòn ngọt",
                        Image = "https://images.unsplash.com/photo-1565680018434-b513d5e5fd47?w=600&auto=format&fit=crop",
                        CategoryId = catSeafood.Id
                    },
                    new Product
                    {
                        Name = "Trà Đào Cam Sả Tươi Mát",
                        Price = 45000m,
                        Description = "Vị thanh ngọt của đào, thơm nồng của sả và chua dịu của cam tươi",
                        Image = "https://images.unsplash.com/photo-1556679343-c7306c1976bc?w=600&auto=format&fit=crop",
                        CategoryId = catBeverage.Id
                    },
                    new Product
                    {
                        Name = "Chè Khúc Bạch Hạnh Nhân Hạt Chia",
                        Price = 40000m,
                        Description = "Những viên khúc bạch mềm béo, hạnh nhân giòn tan trong nước đường phèn nhãn nhục",
                        Image = "https://images.unsplash.com/photo-1563805042-7684c019e1cb?w=600&auto=format&fit=crop",
                        CategoryId = catBeverage.Id
                    }
                );
                await context.SaveChangesAsync();
            }
        }
    }
}
