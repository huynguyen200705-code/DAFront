@echo off
chcp 65001 > nul
title PUBLIC TUNNEL CHO DA RESTAURANT API - DONG BO MOI THIET BI
color 0E
cls
echo ===============================================================================
echo       DA RESTAURANT API - CÔNG CỤ TẠO LINK INTERNET (PUBLIC TUNNEL)
echo   CHO PHÉP MỌI MÁY / ĐIỆN THOẠI TẢI APP VỀ ĐỀU CHẠY ĐỒNG BỘ TRÊN WEB API NÀY!
echo ===============================================================================
echo.
echo  LƯU Ý: Đảm bảo Web API đang chạy trước (chạy run.bat ở 1 cửa sổ khác).
echo.
echo  Chọn giải pháp tunnel bên dưới để mở API ra Internet miễn phí:
echo.
echo  [1] Cloudflare Tunnel (Khuyên Dùng - Hoàn Toàn Miễn Phí, Không Cần Tài Khoản)
echo  [2] Ngrok Tunnel (Yêu cầu đã cài ngrok)
echo  [3] Localtunnel (Dùng npx)
echo.
set /p choice="Nhập lựa chọn của bạn (1, 2 hoặc 3 - Mặc định là 1): "

if "%choice%"=="2" goto run_ngrok
if "%choice%"=="3" goto run_localtunnel

:run_cloudflare
cls
echo ===============================================================================
echo   Đang khởi tạo Cloudflare Tunnel cho http://localhost:5276 ...
echo   Khi đường link HTTPS hiện ra (vd: https://xxxx.trycloudflare.com)
echo   Hãy dán link đó vào file cấu hình .env của ứng dụng Mobile:
echo   BASE_URL=https://xxxx.trycloudflare.com/api/
echo ===============================================================================
echo.
npx cloudflared tunnel --url http://localhost:5276
goto end

:run_ngrok
cls
echo ===============================================================================
echo   Đang chạy Ngrok cho Port 5276 ...
echo ===============================================================================
ngrok http 5276
goto end

:run_localtunnel
cls
echo ===============================================================================
echo   Đang chạy Localtunnel cho Port 5276 ...
echo ===============================================================================
npx localtunnel --port 5276
goto end

:end
pause

