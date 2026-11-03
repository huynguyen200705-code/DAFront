namespace _0859_NguyenQuocHuy_WebAPI.Models;

public class RestaurantLoginRequest
{
    public string Username { get; set; } = string.Empty;
    public string Password { get; set; } = string.Empty;
}

public class RestaurantRegisterRequest
{
    public string Username { get; set; } = string.Empty;
    public string Password { get; set; } = string.Empty;
    public string FullName { get; set; } = string.Empty;
    public string Phone { get; set; } = string.Empty;
    public string Role { get; set; } = "USER"; // ADMIN, USER
}

public class AttendanceCheckInRequest
{
    public long EmployeeId { get; set; }
    public string Method { get; set; } = "NFC"; // NFC, QR_CODE, FACE_ID
    public string? Note { get; set; }
}

public class AttendanceCheckOutRequest
{
    public long EmployeeId { get; set; }
    public string Method { get; set; } = "NFC";
    public string? Note { get; set; }
}

public class UpdateTableStatusRequest
{
    public string Status { get; set; } = "TRONG"; // TRONG, DANG_PHUC_VU, DA_DAT
}

public class SaveActiveOrderRequest
{
    public List<ActiveOrderItemDto> Items { get; set; } = new();
}

public class ActiveOrderItemDto
{
    public long MenuItemId { get; set; }
    public string MenuItemName { get; set; } = string.Empty;
    public double Price { get; set; }
    public int Quantity { get; set; } = 1;
    public string? Note { get; set; }
}

public class CheckoutRequest
{
    public long TableId { get; set; }
    public string TableName { get; set; } = string.Empty;
    public long? CustomerId { get; set; }
    public string CustomerName { get; set; } = "Khách vãng lai";
    public string CustomerTierName { get; set; } = "Bậc Đồng";
    public long EmployeeId { get; set; }
    public string EmployeeName { get; set; } = "Nhân viên";
    public double Subtotal { get; set; }
    public double DiscountPercent { get; set; }
    public string PaymentMethod { get; set; } = "Tiền mặt";
    public List<ActiveOrderItemDto> Items { get; set; } = new();
}

public class DashboardSummaryDto
{
    public double TodayRevenue { get; set; }
    public int TodayOrdersCount { get; set; }
    public int ActiveTablesCount { get; set; }
    public int TotalTablesCount { get; set; }
    public int TotalEmployeesCount { get; set; }
    public int ActiveStaffOnDutyCount { get; set; }
    public int TotalCustomersCount { get; set; }
}
