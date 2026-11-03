using _0859_NguyenQuocHuy_WebAPI.Models;
using _0859_NguyenQuocHuy_WebAPI.Services;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;

namespace _0859_NguyenQuocHuy_WebAPI.Controllers;

[Route("api/orders")]
[ApiController]
public class RestaurantOrdersController : ControllerBase
{
    private readonly ApplicationDbContext _context;
    private readonly ILogger<RestaurantOrdersController> _logger;
    private readonly IAppNotificationService _notificationService;

    public RestaurantOrdersController(
        ApplicationDbContext context,
        ILogger<RestaurantOrdersController> logger,
        IAppNotificationService notificationService)
    {
        _context = context;
        _logger = logger;
        _notificationService = notificationService;
    }

    [HttpGet]
    public async Task<IActionResult> GetAll([FromQuery] int limit = 50)
    {
        var orders = await _context.Orders
            .Include(o => o.Items)
            .OrderByDescending(o => o.Timestamp)
            .Take(limit)
            .ToListAsync();

        var result = orders.Select(o => new
        {
            o.Id,
            o.TableId,
            o.TableName,
            o.CustomerId,
            o.CustomerName,
            o.CustomerTierName,
            o.EmployeeId,
            o.EmployeeName,
            o.Subtotal,
            o.DiscountPercent,
            discountAmount = o.DiscountAmount,
            finalTotal = o.FinalTotal,
            o.PaymentMethod,
            o.Status,
            o.Timestamp,
            formattedDate = DateTimeOffset.FromUnixTimeMilliseconds(o.Timestamp).ToLocalTime().ToString("dd/MM/yyyy HH:mm"),
            itemsCount = o.Items.Count,
            items = o.Items.Select(i => new
            {
                i.Id,
                i.MenuItemId,
                i.MenuItemName,
                i.Price,
                i.Quantity,
                totalPrice = i.TotalPrice,
                i.Note
            })
        });

        return Ok(new { success = true, data = result });
    }

    [HttpGet("{id}")]
    public async Task<IActionResult> GetById(long id)
    {
        var order = await _context.Orders
            .Include(o => o.Items)
            .FirstOrDefaultAsync(o => o.Id == id);

        if (order == null)
        {
            return NotFound(new { success = false, message = "Không tìm thấy hóa đơn." });
        }

        return Ok(new
        {
            success = true,
            data = new
            {
                order.Id,
                order.TableId,
                order.TableName,
                order.CustomerId,
                order.CustomerName,
                order.CustomerTierName,
                order.EmployeeId,
                order.EmployeeName,
                order.Subtotal,
                order.DiscountPercent,
                discountAmount = order.DiscountAmount,
                finalTotal = order.FinalTotal,
                order.PaymentMethod,
                order.Status,
                order.Timestamp,
                formattedDate = DateTimeOffset.FromUnixTimeMilliseconds(order.Timestamp).ToLocalTime().ToString("dd/MM/yyyy HH:mm"),
                items = order.Items.Select(i => new
                {
                    i.Id,
                    i.MenuItemId,
                    i.MenuItemName,
                    i.Price,
                    i.Quantity,
                    totalPrice = i.TotalPrice,
                    i.Note
                })
            }
        });
    }

    [HttpPost("checkout")]
    public async Task<IActionResult> Checkout([FromBody] CheckoutRequest request)
    {
        if (request.Items == null || !request.Items.Any())
        {
            return BadRequest(new { success = false, message = "Đơn hàng chưa có món ăn." });
        }

        using var transaction = await _context.Database.BeginTransactionAsync();
        try
        {
            var now = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();

            var order = new Order
            {
                TableId = request.TableId,
                TableName = request.TableName,
                CustomerId = request.CustomerId,
                CustomerName = string.IsNullOrWhiteSpace(request.CustomerName) ? "Khách vãng lai" : request.CustomerName,
                CustomerTierName = string.IsNullOrWhiteSpace(request.CustomerTierName) ? "Bậc Đồng" : request.CustomerTierName,
                EmployeeId = request.EmployeeId,
                EmployeeName = string.IsNullOrWhiteSpace(request.EmployeeName) ? "Nhân viên" : request.EmployeeName,
                Subtotal = request.Subtotal,
                DiscountPercent = request.DiscountPercent,
                PaymentMethod = string.IsNullOrWhiteSpace(request.PaymentMethod) ? "Tiền mặt" : request.PaymentMethod,
                Status = "COMPLETED",
                Timestamp = now
            };

            foreach (var itemDto in request.Items)
            {
                order.Items.Add(new OrderItem
                {
                    MenuItemId = itemDto.MenuItemId,
                    MenuItemName = itemDto.MenuItemName,
                    Price = itemDto.Price,
                    Quantity = itemDto.Quantity,
                    Note = itemDto.Note
                });
            }

            _context.Orders.Add(order);
            await _context.SaveChangesAsync();

            // Clear table active orders
            var activeItems = _context.ActiveTableOrders.Where(a => a.TableId == request.TableId);
            _context.ActiveTableOrders.RemoveRange(activeItems);

            // Update table status
            var table = await _context.RestaurantTables.FindAsync(request.TableId);
            if (table != null)
            {
                table.Status = "TRONG";
                table.CurrentOrderId = null;
            }

            // Update Customer points & spent if customerId was provided
            if (request.CustomerId.HasValue && request.CustomerId.Value > 0)
            {
                var customer = await _context.Customers.FindAsync(request.CustomerId.Value);
                if (customer != null)
                {
                    customer.TotalSpent += order.FinalTotal;
                    var earnedPoints = (int)(order.FinalTotal / 100000.0);
                    customer.LoyaltyPoints += earnedPoints;
                }
            }

            await _context.SaveChangesAsync();
            await transaction.CommitAsync();

            _logger.LogInformation("Đơn hàng #{OrderId} thanh toán thành công cho {TableName}: {FinalTotal:N0} đ",
                order.Id, order.TableName, order.FinalTotal);

            // Gửi thông báo nổi bật lên Web API
            await _notificationService.RecordActivityAsync(new AppActivity
            {
                Source = "📱 App Thu Ngân/Phục Vụ",
                ClientIp = HttpContext.Connection.RemoteIpAddress?.ToString() ?? "127.0.0.1",
                Action = "THANH TOÁN ĐƠN HÀNG",
                Description = $"{order.TableName} đã thanh toán đơn #{order.Id} - Tổng cộng: {order.FinalTotal:N0} đ (Hình thức: {order.PaymentMethod})",
                Category = "Order",
                Status = "SUCCESS"
            });

            return Ok(new
            {
                success = true,
                message = "Thanh toán đơn hàng thành công!",
                orderId = order.Id,
                finalTotal = order.FinalTotal,
                data = order
            });
        }
        catch (Exception ex)
        {
            await transaction.RollbackAsync();
            _logger.LogError(ex, "Lỗi khi xử lý thanh toán đơn hàng");
            return StatusCode(500, new { success = false, message = "Thanh toán thất bại: " + ex.Message });
        }
    }
}
