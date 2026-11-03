@echo off
chcp 65001 > nul
echo ===============================================================================
echo   HƯỚNG DẪN DEPLOY & PUBLIC WEB API RA INTERNET ĐỂ GỌI TỪ FLUTTER
echo ===============================================================================
echo.
echo [CÁCH 1 - DÙNG CONVEYOR TRONG VISUAL STUDIO (Cách Thầy Hùng Hướng Dẫn)]:
echo   1. Mở Solution bằng Visual Studio.
echo   2. Cài Extension: "Conveyor by Keyoti" (nếu chưa có).
echo   3. Bấm Run (F5). Conveyor sẽ cấp ngay link Internet HTTPS:
echo      Ví dụ: https://xxxx.conveyor.cloud/api/
echo   4. Dán link này vào file .env bên Flutter!
echo.
echo [CÁCH 2 - DÙNG NGROK (Nhanh Chóng & Miễn Phí)]:
echo   1. Tải ngrok từ https://ngrok.com/download
echo   2. Chạy lệnh:
echo        ngrok http 5276
echo   3. Copy đường link Forwarding HTTPS (vd: https://abc-xyz.ngrok-free.app)
echo   4. Đường dẫn API cho Flutter: https://abc-xyz.ngrok-free.app/api/
echo.
echo [CÁCH 3 - DÙNG CLOUDFLARED (Miễn phí 100%% không cần tài khoản)]:
echo   1. Chạy lệnh:
echo        npx cloudflared tunnel --url http://localhost:5276
echo   2. Copy link trycloudflare.com và gắn vào Flutter!
echo.
echo [CÁCH 4 - DÙNG LOCALTUNNEL]:
echo   1. Chạy lệnh:
echo        npx localtunnel --port 5276
echo.
echo [CÁCH 5 - DEPLOY DOCKER LÊN CLOUD (Render / Railway / MonsterASP)]:
echo   - Dự án đã tích hợp sẵn Dockerfile và docker-compose.yml.
echo   - Đẩy code lên GitHub, kết nối Render.com hoặc Railway.app để deploy tự động!
echo.
echo ===============================================================================
pause

