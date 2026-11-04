-- =============================================================================
-- HỆ THỐNG CƠ SỞ DỮ LIỆU TOÀN DIỆN - DA RESTAURANT POS & MANAGEMENT
-- File: DA_Restaurant_Database_Full.sql
-- Tương thích: Microsoft SQL Server 2016 / 2019 / 2022 / Azure SQL / SQLite
-- Mô tả: Script khởi tạo CSDL, toàn bộ cấu trúc bảng và toàn bộ dữ liệu mẫu
-- =============================================================================

USE master;
GO

IF NOT EXISTS (SELECT * FROM sys.databases WHERE name = 'RestaurantPOS_DB')
BEGIN
    CREATE DATABASE [RestaurantPOS_DB];
    PRINT N'[+] Đã tạo Cơ sở dữ liệu RestaurantPOS_DB thành công.';
END
ELSE
BEGIN
    PRINT N'[*] Cơ sở dữ liệu RestaurantPOS_DB đã tồn tại.';
END
GO

USE [RestaurantPOS_DB];
GO

-- =============================================================================
-- PHẦN 1: KHỞI TẠO BẢNG DỮ LIỆU HỆ THỐNG NHÀ HÀNG & POS
-- =============================================================================

-- 1. Bảng Tài khoản người dùng nội bộ (Nhân viên / Admin)
IF OBJECT_ID('restaurant_users', 'U') IS NULL
BEGIN
    CREATE TABLE [restaurant_users] (
        [Id] BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        [Username] NVARCHAR(100) NOT NULL,
        [Password] NVARCHAR(255) NOT NULL,
        [FullName] NVARCHAR(150) NOT NULL,
        [Phone] NVARCHAR(20) NULL,
        [Role] NVARCHAR(50) NOT NULL DEFAULT N'USER' -- ADMIN, USER, MANAGER, WAITER
    );
    PRINT N'[+] Đã tạo bảng restaurant_users';
END
GO

-- 2. Bảng Bàn ăn
IF OBJECT_ID('restaurant_tables', 'U') IS NULL
BEGIN
    CREATE TABLE [restaurant_tables] (
        [Id] BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        [Name] NVARCHAR(100) NOT NULL,
        [Floor] NVARCHAR(100) NOT NULL DEFAULT N'Tầng 1',
        [Seats] INT NOT NULL DEFAULT 4,
        [Status] NVARCHAR(50) NOT NULL DEFAULT N'TRONG', -- TRONG, DANG_PHUC_VU, DA_DAT
        [CurrentOrderId] BIGINT NULL
    );
    PRINT N'[+] Đã tạo bảng restaurant_tables';
END
GO

-- 3. Bảng Thực đơn món ăn
IF OBJECT_ID('menu_items', 'U') IS NULL
BEGIN
    CREATE TABLE [menu_items] (
        [Id] BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        [Name] NVARCHAR(150) NOT NULL,
        [Category] NVARCHAR(100) NOT NULL DEFAULT N'Món chính', -- Khai vị, Món chính, Đồ uống, Tráng miệng
        [Price] FLOAT NOT NULL,
        [Description] NVARCHAR(500) NULL,
        [IsAvailable] BIT NOT NULL DEFAULT 1
    );
    PRINT N'[+] Đã tạo bảng menu_items';
END
GO

-- 4. Bảng Khách hàng & Thành viên tích điểm
IF OBJECT_ID('customers', 'U') IS NULL
BEGIN
    CREATE TABLE [customers] (
        [Id] BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        [Name] NVARCHAR(150) NOT NULL,
        [Phone] NVARCHAR(20) NOT NULL,
        [Email] NVARCHAR(150) NULL,
        [TotalSpent] FLOAT NOT NULL DEFAULT 0.0,
        [LoyaltyPoints] INT NOT NULL DEFAULT 0
    );
    PRINT N'[+] Đã tạo bảng customers';
END
GO

-- 5. Bảng Nhân viên & Quản lý tiền lương
IF OBJECT_ID('employees', 'U') IS NULL
BEGIN
    CREATE TABLE [employees] (
        [Id] BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        [Name] NVARCHAR(150) NOT NULL,
        [Phone] NVARCHAR(20) NOT NULL,
        [Position] NVARCHAR(100) NOT NULL,
        [Tier] NVARCHAR(50) NOT NULL DEFAULT N'CHINH_THUC', -- THU_VIEC, CHINH_THUC, QUAN_LY, GIAM_DOC
        [ContractType] NVARCHAR(50) NOT NULL DEFAULT N'MONTHLY', -- HOURLY, MONTHLY
        [HourlyRate] FLOAT NOT NULL DEFAULT 25000.0,
        [MonthlySalary] FLOAT NOT NULL DEFAULT 8000000.0,
        [WorkedHours] FLOAT NOT NULL DEFAULT 0.0,
        [WorkedDays] FLOAT NOT NULL DEFAULT 0.0,
        [Bonus] FLOAT NOT NULL DEFAULT 0.0,
        [Penalty] FLOAT NOT NULL DEFAULT 0.0
    );
    PRINT N'[+] Đã tạo bảng employees';
END
GO

-- 6. Bảng Nhật ký chấm công NFC / QR Code
IF OBJECT_ID('attendance_records', 'U') IS NULL
BEGIN
    CREATE TABLE [attendance_records] (
        [Id] BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        [EmployeeId] BIGINT NOT NULL,
        [EmployeeName] NVARCHAR(150) NOT NULL,
        [Type] NVARCHAR(50) NOT NULL, -- CHECK_IN, CHECK_OUT
        [Method] NVARCHAR(50) NOT NULL DEFAULT N'NFC', -- NFC, QR_CODE, FACE_ID, MANUAL
        [Timestamp] BIGINT NOT NULL,
        [DurationHours] FLOAT NOT NULL DEFAULT 0.0,
        [Note] NVARCHAR(255) NULL
    );
    PRINT N'[+] Đã tạo bảng attendance_records';
END
GO

-- 7. Bảng Hóa đơn bán hàng POS
IF OBJECT_ID('orders', 'U') IS NULL
BEGIN
    CREATE TABLE [orders] (
        [Id] BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        [TableId] BIGINT NOT NULL,
        [TableName] NVARCHAR(100) NOT NULL,
        [CustomerId] BIGINT NULL,
        [CustomerName] NVARCHAR(150) NULL DEFAULT N'Khách vãng lai',
        [CustomerTierName] NVARCHAR(100) NULL DEFAULT N'Bậc Đồng',
        [EmployeeId] BIGINT NOT NULL,
        [EmployeeName] NVARCHAR(150) NULL DEFAULT N'Nhân viên',
        [Subtotal] FLOAT NOT NULL DEFAULT 0.0,
        [DiscountPercent] FLOAT NOT NULL DEFAULT 0.0,
        [PaymentMethod] NVARCHAR(100) NOT NULL DEFAULT N'Tiền mặt', -- Tiền mặt, VietQR, Thẻ ngân hàng
        [Status] NVARCHAR(50) NOT NULL DEFAULT N'COMPLETED', -- PENDING, COMPLETED, CANCELLED
        [Timestamp] BIGINT NOT NULL
    );
    PRINT N'[+] Đã tạo bảng orders';
END
GO

-- 8. Bảng Chi tiết món ăn trong hóa đơn
IF OBJECT_ID('order_items', 'U') IS NULL
BEGIN
    CREATE TABLE [order_items] (
        [Id] BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        [OrderId] BIGINT NOT NULL,
        [MenuItemId] BIGINT NOT NULL,
        [MenuItemName] NVARCHAR(150) NOT NULL,
        [Price] FLOAT NOT NULL,
        [Quantity] INT NOT NULL DEFAULT 1,
        [Note] NVARCHAR(255) NULL,
        CONSTRAINT FK_OrderItems_Orders FOREIGN KEY ([OrderId]) REFERENCES [orders]([Id]) ON DELETE CASCADE
    );
    PRINT N'[+] Đã tạo bảng order_items';
END
GO

-- 9. Bảng Món đang phục vụ tại bàn thời gian thực (Active Table Orders)
IF OBJECT_ID('active_table_orders', 'U') IS NULL
BEGIN
    CREATE TABLE [active_table_orders] (
        [Id] BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        [TableId] BIGINT NOT NULL,
        [MenuItemId] BIGINT NOT NULL,
        [MenuItemName] NVARCHAR(150) NOT NULL,
        [Price] FLOAT NOT NULL,
        [Quantity] INT NOT NULL DEFAULT 1,
        [Note] NVARCHAR(255) NULL
    );
    PRINT N'[+] Đã tạo bảng active_table_orders';
END
GO

-- 10. Bảng Nhật ký hoạt động App Real-time (App Activities)
IF OBJECT_ID('app_activities', 'U') IS NULL
BEGIN
    CREATE TABLE [app_activities] (
        [Id] BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        [Timestamp] BIGINT NOT NULL,
        [FormattedTime] NVARCHAR(50) NOT NULL,
        [Source] NVARCHAR(100) NOT NULL DEFAULT N'Mobile App',
        [ClientIp] NVARCHAR(100) NOT NULL DEFAULT N'127.0.0.1',
        [Action] NVARCHAR(100) NOT NULL,
        [Description] NVARCHAR(500) NOT NULL,
        [Category] NVARCHAR(50) NOT NULL DEFAULT N'General',
        [Status] NVARCHAR(20) NOT NULL DEFAULT N'SUCCESS',
        [PayloadJson] NVARCHAR(MAX) NULL
    );
    PRINT N'[+] Đã tạo bảng app_activities';
END
GO

-- 11. Bảng Danh mục & Sản phẩm ẩm thực mở rộng
IF OBJECT_ID('Categories', 'U') IS NULL
BEGIN
    CREATE TABLE [Categories] (
        [Id] INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        [Name] NVARCHAR(150) NOT NULL,
        [Description] NVARCHAR(500) NULL
    );
    PRINT N'[+] Đã tạo bảng Categories';
END
GO

IF OBJECT_ID('Products', 'U') IS NULL
BEGIN
    CREATE TABLE [Products] (
        [Id] INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        [Name] NVARCHAR(250) NULL,
        [Price] DECIMAL(18,2) NOT NULL,
        [Image] NVARCHAR(MAX) NULL,
        [Description] NVARCHAR(MAX) NULL,
        [CategoryId] INT NULL,
        CONSTRAINT FK_Products_Categories FOREIGN KEY ([CategoryId]) REFERENCES [Categories]([Id]) ON DELETE SET NULL
    );
    PRINT N'[+] Đã tạo bảng Products';
END
GO

-- =============================================================================
-- PHẦN 2: BẢNG IDENTITY BẢO MẬT & PHÂN QUYỀN (ASP.NET CORE IDENTITY)
-- =============================================================================

IF OBJECT_ID('AspNetRoles', 'U') IS NULL
BEGIN
    CREATE TABLE [AspNetRoles] (
        [Id] NVARCHAR(450) NOT NULL PRIMARY KEY,
        [Name] NVARCHAR(256) NULL,
        [NormalizedName] NVARCHAR(256) NULL,
        [ConcurrencyStamp] NVARCHAR(MAX) NULL
    );
END
GO

IF OBJECT_ID('AspNetUsers', 'U') IS NULL
BEGIN
    CREATE TABLE [AspNetUsers] (
        [Id] NVARCHAR(450) NOT NULL PRIMARY KEY,
        [UserName] NVARCHAR(256) NULL,
        [NormalizedUserName] NVARCHAR(256) NULL,
        [Email] NVARCHAR(256) NULL,
        [NormalizedEmail] NVARCHAR(256) NULL,
        [EmailConfirmed] BIT NOT NULL DEFAULT 0,
        [PasswordHash] NVARCHAR(MAX) NULL,
        [SecurityStamp] NVARCHAR(MAX) NULL,
        [ConcurrencyStamp] NVARCHAR(MAX) NULL,
        [PhoneNumber] NVARCHAR(MAX) NULL,
        [PhoneNumberConfirmed] BIT NOT NULL DEFAULT 0,
        [TwoFactorEnabled] BIT NOT NULL DEFAULT 0,
        [LockoutEnd] DATETIMEOFFSET NULL,
        [LockoutEnabled] BIT NOT NULL DEFAULT 1,
        [AccessFailedCount] INT NOT NULL DEFAULT 0,
        [Initials] NVARCHAR(5) NULL
    );
END
GO

IF OBJECT_ID('AspNetUserRoles', 'U') IS NULL
BEGIN
    CREATE TABLE [AspNetUserRoles] (
        [UserId] NVARCHAR(450) NOT NULL,
        [RoleId] NVARCHAR(450) NOT NULL,
        PRIMARY KEY ([UserId], [RoleId]),
        FOREIGN KEY ([UserId]) REFERENCES [AspNetUsers]([Id]) ON DELETE CASCADE,
        FOREIGN KEY ([RoleId]) REFERENCES [AspNetRoles]([Id]) ON DELETE CASCADE
    );
END
GO

-- =============================================================================
-- PHẦN 3: LƯU TOÀN BỘ DỮ LIỆU MẪU ĐẦY ĐỦ VÀO CƠ SỞ DỮ LIỆU
-- =============================================================================

-- 1. Thêm Người dùng / Quản trị viên
IF NOT EXISTS (SELECT 1 FROM [restaurant_users])
BEGIN
    INSERT INTO [restaurant_users] ([Username], [Password], [FullName], [Phone], [Role]) VALUES
    (N'admin', N'123', N'Quản Trị Viên Nhà Hàng', N'0901234567', N'ADMIN'),
    (N'staff', N'123', N'Nhân Viên Thu Ngân', N'0909876543', N'USER'),
    (N'waiter', N'123', N'Nhân Viên Phục Vụ Bàn', N'0911223344', N'USER'),
    (N'chef', N'123', N'Bếp Trưởng Điều Hành', N'0988776655', N'USER');
    PRINT N'[+] Đã nạp dữ liệu restaurant_users';
END
GO

-- 2. Thêm Danh sách Bàn ăn
IF NOT EXISTS (SELECT 1 FROM [restaurant_tables])
BEGIN
    INSERT INTO [restaurant_tables] ([Name], [Floor], [Seats], [Status], [CurrentOrderId]) VALUES
    (N'Bàn 101', N'Tầng 1', 4, N'TRONG', NULL),
    (N'Bàn 102', N'Tầng 1', 4, N'DANG_PHUC_VU', 1),
    (N'Bàn 103', N'Tầng 1', 6, N'TRONG', NULL),
    (N'Bàn 104', N'Tầng 1', 2, N'TRONG', NULL),
    (N'Bàn 105', N'Tầng 1', 8, N'DA_DAT', NULL),
    (N'Bàn 106', N'Tầng 1', 4, N'TRONG', NULL),
    (N'Bàn 201', N'Tầng 2 (Sân Vườn)', 4, N'TRONG', NULL),
    (N'Bàn 202', N'Tầng 2 (Sân Vườn)', 6, N'DANG_PHUC_VU', NULL),
    (N'Bàn 203', N'Tầng 2 (Sân Vườn)', 10, N'TRONG', NULL),
    (N'Bàn 204', N'Tầng 2 (Sân Vườn)', 4, N'TRONG', NULL),
    (N'Phòng VIP 01', N'Khu VIP Hoàng Gia', 12, N'TRONG', NULL),
    (N'Phòng VIP 02', N'Khu VIP Hoàng Gia', 8, N'DA_DAT', NULL),
    (N'Phòng VIP 03', N'Khu VIP Hoàng Gia', 16, N'TRONG', NULL);
    PRINT N'[+] Đã nạp dữ liệu restaurant_tables';
END
GO

-- 3. Thêm Thực đơn món ăn phong phú
IF NOT EXISTS (SELECT 1 FROM [menu_items])
BEGIN
    INSERT INTO [menu_items] ([Name], [Category], [Price], [Description], [IsAvailable]) VALUES
    -- Khai vị
    (N'Gỏi ngó sen tôm thịt', N'Khai vị', 120000, N'Tôm sú tươi, thịt ba chỉ giòn rụm, ngó sen chua ngọt thơm lừng', 1),
    (N'Khoai tây múi cau chiên bơ tỏi', N'Khai vị', 65000, N'Khoai tây tươi chiên vàng giòn sốt bơ tỏi thơm ngậy', 1),
    (N'Salad cá ngừ đại dương sốt mè', N'Khai vị', 135000, N'Rau rocket hữu cơ, cá ngừ ngâm dầu mè rang Nhật Bản', 1),
    (N'Súp cua tuyết bào ngư nấm đông cô', N'Khai vị', 185000, N'Thịt cua tuyết đậm đà, nước dùng hầm gà xương ngọt thanh', 1),
    (N'Nem hải sản sốt Mayonnaise', N'Khai vị', 110000, N'Vỏ rế giòn tan cuộn mực tươi, tôm sú và sốt kem', 1),

    -- Món chính
    (N'Bò Wagyu A5 nướng đá muối Himalaya', N'Món chính', 890000, N'Thịt bò Wagyu nhập khẩu Nhật thượng hạng vân mỡ cẩm thạch', 1),
    (N'Cá hồi Na Uy áp chảo sốt chanh leo', N'Món chính', 320000, N'Filet cá hồi áp chảo béo ngậy kèm măng tây tươi sốt chua ngọt', 1),
    (N'Sườn cừu đút lò thảo mộc sốt tiêu đen', N'Món chính', 450000, N'Sườn cừu Úc mềm ngọt ướp hương thảo và sốt tiêu cay nhẹ', 1),
    (N'Lẩu hải sản Hoàng Kim chua cay', N'Món chính', 580000, N'Mực nhảy, tôm hùm baby, nghêu lụa, cá hồi và nấm tươi', 1),
    (N'Cơm chiên hải sản Hoàng Bào', N'Món chính', 150000, N'Cơm hạt ngọc dẻo thơm, tôm mực trứng muối vàng óng', 1),
    (N'Tôm hùm nướng phô mai Mozzarella', N'Món chính', 650000, N'Tôm hùm bông nửa ký đút lò phô mai kéo sợi thơm phức', 1),
    (N'Gà đồi ủ muối hoa tiêu', N'Món chính', 290000, N'Gà ta thịt săn chắc da vàng giòn thấm vị hoa tiêu cay nồng', 1),

    -- Đồ uống
    (N'Trà đào cam sả thanh nhiệt', N'Đồ uống', 45000, N'Đào ngâm giòn, tinh dầu sả tươi và nước cam nguyên chất', 1),
    (N'Trà sữa ô long nướng kem cheese', N'Đồ uống', 55000, N'Trà ô long chuẩn vị Đài Loan cùng lớp kem cheese béo ngậy', 1),
    (N'Mojito việt quất bạc hà mát lạnh', N'Đồ uống', 65000, N'Nước soda sủi bọt, chanh vàng, việt quất và lá bạc hà tươi', 1),
    (N'Bia Heineken Silver ướp lạnh', N'Đồ uống', 35000, N'Bia lon 330ml nhập khẩu chính hãng phục vụ ly đá tuyết', 1),
    (N'Rượu vang đỏ Bordeaux Chateau 2018', N'Đồ uống', 1250000, N'Chai 750ml hương gỗ sồi và quả mọng đỏ quyến rũ', 1),
    (N'Nước dừa tươi xiêm Bến Tre', N'Đồ uống', 38000, N'Trái dừa tươi ngọt lành làm mát tức thì', 1),

    -- Tráng miệng
    (N'Panna Cotta dâu tây tươi Đà Lạt', N'Tráng miệng', 50000, N'Kem sữa mềm mượt tan chảy kèm mứt dâu tây chua ngọt', 1),
    (N'Bánh Tiramisu Ý truyền thống', N'Tráng miệng', 65000, N'Vị đắng nhẹ cà phê Espresso hòa quyện phô mai Mascarpone', 1),
    (N'Chè hạt sen long nhãn táo đỏ', N'Tráng miệng', 45000, N'Hạt sen bùi béo hầm đường phèn thanh mát bổ dưỡng', 1);
    PRINT N'[+] Đã nạp dữ liệu menu_items';
END
GO

-- 4. Thêm Khách hàng thành viên
IF NOT EXISTS (SELECT 1 FROM [customers])
BEGIN
    INSERT INTO [customers] ([Name], [Phone], [Email], [TotalSpent], [LoyaltyPoints]) VALUES
    (N'Nguyễn Văn An', N'0911111111', N'an.nguyen@gmail.com', 2500000.0, 25),
    (N'Trần Thị Bích', N'0922222222', N'bich.tran@gmail.com', 8500000.0, 85),
    (N'Lê Văn Cường', N'0933333333', N'cuong.le@gmail.com', 19500000.0, 195),
    (N'Phạm Thị Diễm', N'0944444444', N'diem.pham@gmail.com', 38000000.0, 380),
    (N'Hoàng Văn Em', N'0955555555', N'em.hoang@gmail.com', 65000000.0, 650),
    (N'Đặng Minh Khang', N'0966666666', N'khang.dang@gmail.com', 4500000.0, 45),
    (N'Vũ Quỳnh Trang', N'0977777777', N'trang.vu@gmail.com', 12000000.0, 120);
    PRINT N'[+] Đã nạp dữ liệu customers';
END
GO

-- 5. Thêm Nhân viên & Quản lý lương
IF NOT EXISTS (SELECT 1 FROM [employees])
BEGIN
    INSERT INTO [employees] ([Name], [Phone], [Position], [Tier], [ContractType], [HourlyRate], [MonthlySalary], [WorkedHours], [WorkedDays], [Bonus], [Penalty]) VALUES
    (N'Nguyễn Văn Bình', N'098111222', N'Phục vụ bàn', N'THU_VIEC', N'HOURLY', 25000.0, 0.0, 130.0, 0.0, 250000.0, 0.0),
    (N'Trần Thị Hoa', N'098333444', N'Thu ngân chính', N'CHINH_THUC', N'MONTHLY', 0.0, 8500000.0, 0.0, 25.0, 500000.0, 100000.0),
    (N'Lê Hoàng Nam', N'098555666', N'Bếp trưởng điều hành', N'QUAN_LY', N'MONTHLY', 0.0, 16000000.0, 0.0, 26.0, 2000000.0, 0.0),
    (N'Phạm Minh Đức', N'098777888', N'Giám đốc vận hành F&B', N'GIAM_DOC', N'MONTHLY', 0.0, 32000000.0, 0.0, 26.0, 5000000.0, 0.0),
    (N'Đỗ Thu Thảo', N'098999000', N'Tiếp tân / Lễ tân', N'CHINH_THUC', N'HOURLY', 30000.0, 0.0, 160.0, 0.0, 300000.0, 50000.0),
    (N'Võ Thành Long', N'098222333', N'Phụ bếp', N'CHINH_THUC', N'MONTHLY', 0.0, 7500000.0, 0.0, 24.0, 400000.0, 0.0);
    PRINT N'[+] Đã nạp dữ liệu employees';
END
GO

-- 6. Thêm Lịch sử Chấm công mẫu (NFC / QR)
IF NOT EXISTS (SELECT 1 FROM [attendance_records])
BEGIN
    INSERT INTO [attendance_records] ([EmployeeId], [EmployeeName], [Type], [Method], [Timestamp], [DurationHours], [Note]) VALUES
    (1, N'Nguyễn Văn Bình', N'CHECK_IN', N'NFC', 1730682000000, 0.0, N'Điểm danh vào ca sáng qua thẻ NFC'),
    (1, N'Nguyễn Văn Bình', N'CHECK_OUT', N'NFC', 1730703600000, 6.0, N'Tan ca đúng giờ lúc 14:00'),
    (2, N'Trần Thị Hoa', N'CHECK_IN', N'QR_CODE', 1730680200000, 0.0, N'Quét mã QR quầy thu ngân vào ca'),
    (2, N'Trần Thị Hoa', N'CHECK_OUT', N'QR_CODE', 1730709000000, 8.0, N'Tan ca quầy thu ngân'),
    (3, N'Lê Hoàng Nam', N'CHECK_IN', N'FACE_ID', 1730678400000, 0.0, N'Bếp trưởng vào ca kiểm tra nguyên liệu');
    PRINT N'[+] Đã nạp dữ liệu attendance_records';
END
GO

-- 7. Thêm Hóa đơn mẫu & Chi tiết món
IF NOT EXISTS (SELECT 1 FROM [orders])
BEGIN
    -- Đơn hàng 1
    INSERT INTO [orders] ([TableId], [TableName], [CustomerId], [CustomerName], [CustomerTierName], [EmployeeId], [EmployeeName], [Subtotal], [DiscountPercent], [PaymentMethod], [Status], [Timestamp])
    VALUES (1, N'Bàn 101', 1, N'Nguyễn Văn An', N'Bậc Đồng', 2, N'Trần Thị Hoa', 1210000.0, 0.0, N'Tiền mặt', N'COMPLETED', 1730691600000);

    DECLARE @Ord1Id BIGINT = SCOPE_IDENTITY();
    INSERT INTO [order_items] ([OrderId], [MenuItemId], [MenuItemName], [Price], [Quantity], [Note]) VALUES
    (@Ord1Id, 6, N'Bò Wagyu A5 nướng đá muối Himalaya', 890000, 1, N'Nướng vừa chín tới (Medium)'),
    (@Ord1Id, 1, N'Gỏi ngó sen tôm thịt', 120000, 1, N'Ít cay'),
    (@Ord1Id, 13, N'Trà đào cam sả thanh nhiệt', 45000, 2, N'Ít đá 70% ngọt');

    -- Đơn hàng 2
    INSERT INTO [orders] ([TableId], [TableName], [CustomerId], [CustomerName], [CustomerTierName], [EmployeeId], [EmployeeName], [Subtotal], [DiscountPercent], [PaymentMethod], [Status], [Timestamp])
    VALUES (11, N'Phòng VIP 01', 5, N'Hoàng Văn Em', N'Bậc Kim Cương', 2, N'Trần Thị Hoa', 2340000.0, 20.0, N'VietQR', N'COMPLETED', 1730698800000);

    DECLARE @Ord2Id BIGINT = SCOPE_IDENTITY();
    INSERT INTO [order_items] ([OrderId], [MenuItemId], [MenuItemName], [Price], [Quantity], [Note]) VALUES
    (@Ord2Id, 11, N'Tôm hùm nướng phô mai Mozzarella', 650000, 2, N'Phô mai gấp đôi'),
    (@Ord2Id, 4, N'Súp cua tuyết bào ngư nấm đông cô', 185000, 4, N'Ăn nóng'),
    (@Ord2Id, 17, N'Rượu vang đỏ Bordeaux Chateau 2018', 1250000, 1, N'Ướp xô đá');

    PRINT N'[+] Đã nạp dữ liệu orders & order_items';
END
GO

-- 8. Thêm Món đang phục vụ tại Bàn 102
IF NOT EXISTS (SELECT 1 FROM [active_table_orders])
BEGIN
    INSERT INTO [active_table_orders] ([TableId], [MenuItemId], [MenuItemName], [Price], [Quantity], [Note]) VALUES
    (2, 2, N'Khoai tây múi cau chiên bơ tỏi', 65000, 1, N'Ăn kèm tương cà'),
    (2, 7, N'Cá hồi Na Uy áp chảo sốt chanh leo', 320000, 1, N'Chín kỹ'),
    (2, 14, N'Trà sữa ô long nướng kem cheese', 55000, 2, N'Trân châu trắng');
    PRINT N'[+] Đã nạp dữ liệu active_table_orders';
END
GO

-- 9. Thêm Nhật ký hoạt động mẫu
IF NOT EXISTS (SELECT 1 FROM [app_activities])
BEGIN
    INSERT INTO [app_activities] ([Timestamp], [FormattedTime], [Source], [ClientIp], [Action], [Description], [Category], [Status]) VALUES
    (1730691600000, N'10:20:00 04/11/2026', N'📱 App Thu Ngân', N'192.168.100.219', N'THANH TOÁN ĐƠN HÀNG', N'Bàn 101 đã thanh toán đơn #1 - Tổng: 1.210.000 đ (Hình thức: Tiền mặt)', N'Order', N'SUCCESS'),
    (1730682000000, N'07:40:00 04/11/2026', N'📱 App Nhân Viên', N'192.168.100.219', N'CHẤM CÔNG VÀO CA', N'Nhân viên Nguyễn Văn Bình đã điểm danh vào ca qua NFC', N'Attendance', N'SUCCESS'),
    (1730679000000, N'06:50:00 04/11/2026', N'🖥️ Web Quản Trị', N'127.0.0.1', N'ĐĂNG NHẬP HỆ THỐNG', N'Quản trị viên admin đã đăng nhập vào hệ thống', N'Auth', N'SUCCESS');
    PRINT N'[+] Đã nạp dữ liệu app_activities';
END
GO

-- 10. Thêm Danh mục & Sản phẩm mở rộng
IF NOT EXISTS (SELECT 1 FROM [Categories])
BEGIN
    INSERT INTO [Categories] ([Name], [Description]) VALUES
    (N'Món Ăn Cao Cấp', N'Các món ăn thượng hạng chế biến từ nguyên liệu tuyển chọn'),
    (N'Đồ Uống & Rượu Vang', N'Nước ép tươi, bia, cocktail và rượu vang nhập khẩu'),
    (N'Tráng Miệng Tinh Tế', N'Bánh ngọt và chè thanh mát');
    PRINT N'[+] Đã nạp dữ liệu Categories';
END
GO

IF NOT EXISTS (SELECT 1 FROM [Products])
BEGIN
    INSERT INTO [Products] ([Name], [Price], [Image], [Description], [CategoryId]) VALUES
    (N'Bò Wagyu A5 nướng đá muối', 890000.00, N'/images/wagyu.jpg', N'Thịt bò Wagyu nhập khẩu Nhật thượng hạng', 1),
    (N'Cá hồi Na Uy áp chảo sốt chanh leo', 320000.00, N'/images/cahoi.jpg', N'Filet cá hồi áp chảo béo ngậy', 1),
    (N'Rượu vang đỏ Bordeaux Chateau 2018', 1250000.00, N'/images/wine.jpg', N'Chai 750ml nhập khẩu Pháp', 2),
    (N'Bánh Tiramisu Ý truyền thống', 65000.00, N'/images/tiramisu.jpg', N'Phô mai Mascarpone và cà phê Espresso', 3);
    PRINT N'[+] Đã nạp dữ liệu Products';
END
GO

PRINT N'=============================================================================';
PRINT N'🎉 HOÀN TẤT KHỞI TẠO VÀ LƯU TOÀN BỘ CƠ SỞ DỮ LIỆU RESTAURANTPOS_DB THÀNH CÔNG!';
PRINT N'=============================================================================';
