# DA Restaurant Api — Hệ Thống Quản Lý Toàn Bộ Nhà Hàng & POS

Hệ thống Backend ASP.NET Core Web API hoàn chỉnh phục vụ quản lý toàn diện Nhà Hàng & POS, hỗ trợ **đồng bộ đa thiết bị (mọi máy/điện thoại tải app về đều kết nối chung)** và **hệ thống thông báo Real-time tức thì qua SignalR mỗi khi có người hoạt động trên app**.

---

## 🌟 Tính Năng Chính Của Hệ Thống

### 1. Đồng Bộ Đa Thiết Bị (Multi-Device Sync)
- Mọi điện thoại (Android, iOS) hoặc máy tính khi tải ứng dụng về đều kết nối và đồng bộ trực tiếp với cùng 1 Web API.
- Dữ liệu bàn ăn, gọi món, thanh toán, chấm công và thực đơn được lưu tập trung vào SQLite (`RestaurantPOS.db`).
- Hỗ trợ kết nối cả trong mạng nội bộ (Wi-Fi/LAN) và ra ngoài Internet (qua Cloudflare Tunnel / Ngrok với `start_public_tunnel.bat`).

### 2. Thông Báo Real-time Mọi Hoạt Động Từ App (SignalR Live Notification)
- Bất kỳ khi nào có người thao tác trên app (Đăng nhập, Gọi món, Thanh toán, Đổi trạng thái bàn, Điểm danh chấm công NFC/QR, Thêm/Sửa thực đơn...):
  - **Cửa sổ dòng lệnh (Console) của Web API**: Lập tức xuất hiện dòng thông báo nổi bật màu sắc kèm thời gian, IP thiết bị và nội dung thao tác.
  - **Trang Quản Trị Web (`http://localhost:5276/`)**: Bắn popup Toast thông báo tức thời ở góc phải kèm chuông báo âm thanh, tự động cập nhật số liệu không cần tải lại trang.
  - **Tab chuyên dụng "Hoạt Động App & Đồng Bộ"**: Lưu lại toàn bộ lịch sử các hoạt động từ app theo thời gian thực.
- Tích hợp sẵn endpoint kiểm tra và gửi thông báo tùy biến:
  - `GET /api/notifications/recent`: Xem 50 hoạt động gần nhất.
  - `POST /api/notifications/test`: Thử nghiệm bắn thông báo mẫu.
  - `POST /api/notifications/log`: Cho phép app chủ động gửi thông báo tùy chỉnh.

### 3. Quản lý Bàn Ăn (Restaurant Tables)
- Sơ đồ bàn theo tầng/khu vực (Tầng 1, Tầng 2, Phòng VIP).
- Trạng thái bàn: `TRONG` (Trống), `DANG_PHUC_VU` (Đang phục vụ), `DA_DAT` (Đã đặt trước).
- Tự động cập nhật trạng thái thời gian thực giữa các thiết bị.

### 4. POS & Quản Lý Đơn Hàng (Orders & Checkout)
- Gọi món tại bàn, chi tiết món ăn, giảm giá theo hạng thành viên khách hàng.
- Đa dạng phương thức thanh toán: Tiền mặt, Chuyển khoản VietQR, Thẻ POS.
- Tự động tích điểm và tích lũy chi tiêu khách hàng.

### 5. Quản lý Nhân Sự & Chấm Công (Employees & Attendance)
- Quản lý nhân viên, hợp đồng (`HOURLY`, `MONTHLY`), bậc lương.
- Tích hợp chấm công không chạm qua NFC hoặc quét mã QR.
- Lịch sử chấm công `CHECK_IN` / `CHECK_OUT` kèm thời lượng ca làm việc.

### 6. Thực Đơn Món Ăn, Danh Mục & Sản Phẩm (Menu & Categories)
- Quản lý đầy đủ danh mục, sản phẩm, hình ảnh và trạng thái còn món.

### 7. Xác Thực, Phân Quyền & Smart Auth (Identity & JWT)
- Đăng nhập/Đăng ký cấp phát JWT Bearer Token.
- Phân quyền theo vai trò `Admin` và `User`.
- Xác thực bằng số điện thoại không cần mật khẩu (**Smart Auth OTP**).

---

## 🚀 Hướng Dẫn Khởi Chạy Web API

### Cách 1: Chạy bằng file `run.bat` (Khuyên dùng)
Click đúp chuột vào file **`run.bat`** tại thư mục gốc. Hệ thống sẽ tự động khởi động server và hiển thị địa chỉ IP để kết nối từ điện thoại.

### Cách 2: Chạy bằng lệnh Terminal
```bash
dotnet run --project "0859_NguyenQuocHuy_WebAPI/DA Restaurant Api.csproj"
```

Sau khi khởi động thành công:
- **Giao diện Web Admin & Thông Báo Realtime**: [http://localhost:5276/](http://localhost:5276/)
- **Tài liệu Swagger API**: [http://localhost:5276/swagger](http://localhost:5276/swagger)
- **SignalR Hub Realtime**: [http://localhost:5276/hubs/notifications](http://localhost:5276/hubs/notifications)

---

## 📱 Hướng Dẫn Đồng Bộ Cho Mọi Máy Tải App Về

### Trường hợp 1: Các máy cùng kết nối chung mạng Wi-Fi (Nhanh nhất)
1. Mở file `run.bat` lên, xem địa chỉ IP mạng LAN của máy bạn (ví dụ: `192.168.1.15`).
2. Mở file cấu hình `.env` của ứng dụng Mobile và đổi thành:
```env
BASE_URL=http://192.168.1.15:5276/api/
```
*(Thay `192.168.1.15` bằng IP hiển thị trên màn hình Web Admin của bạn)*

### Trường hợp 2: Các máy dùng 4G/Internet khác mạng (Gửi app cho bất kỳ ai)
1. Click đúp chạy file **`start_public_tunnel.bat`** ở thư mục gốc.
2. Chọn `1` để dùng Cloudflare Tunnel (miễn phí 100%, không cần đăng ký tài khoản).
3. Đợi vài giây, màn hình sẽ hiện link HTTPS dạng: `https://xxxx.trycloudflare.com`
4. Dán link đó vào file `.env` của app:
```env
BASE_URL=https://xxxx.trycloudflare.com/api/
```
Bất kỳ ai ở bất kỳ đâu tải app về máy đều sẽ kết nối chung và đồng bộ dữ liệu về máy tính của bạn!
