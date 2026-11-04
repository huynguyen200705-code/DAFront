@echo off
chcp 65001 >nul
title DA Restaurant Api - He Thong Quan Ly Nha Hang
color 0B
cls
echo ===============================================================================
echo         DA RESTAURANT API - HE THONG QUAN LY NHA HANG va POS
echo           DONG BO DA THIET BI va REAL-TIME NOTIFICATION
echo ===============================================================================
echo.
echo [+] Database: SQLite (RestaurantPOS.db)
echo [+] Cong ket noi: http://0.0.0.0:5276
echo [+] He thong thong bao Real-time qua SignalR: BAT
echo.

REM === Buoc 1: Tat tat ca process dotnet dang chay ===
echo [*] Dang tat tat ca tien trinh dotnet cu...
taskkill /IM "dotnet.exe" /F >nul 2>&1
taskkill /IM "DA Restaurant Api.exe" /F >nul 2>&1
taskkill /IM "DA_Restaurant_Api.exe" /F >nul 2>&1
timeout /t 1 /nobreak >nul

REM === Buoc 2: Kill process dang chiem port 5276 ===
echo [*] Kiem tra port 5276...
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":5276 " ^| findstr "LISTENING" 2^>nul') do (
    echo     [!] Tim thay PID=%%a tren port 5276 - Dang tat...
    taskkill /PID %%a /F >nul 2>&1
)
timeout /t 2 /nobreak >nul

REM === Buoc 3: Xac nhan port da trong ===
netstat -ano | findstr ":5276 " | findstr "LISTENING" >nul 2>&1
if %errorlevel%==0 (
    echo [!] CANH BAO: Port 5276 van bi chiem. Thu kill lan 2...
    for /f "tokens=5" %%b in ('netstat -ano ^| findstr ":5276 " ^| findstr "LISTENING" 2^>nul') do (
        taskkill /PID %%b /F /T >nul 2>&1
    )
    timeout /t 2 /nobreak >nul
)

echo [*] Dang khoi dong Web API...
echo.
echo ===============================================================================
echo   Web Admin Dashboard : http://localhost:5276/
echo   Tai lieu Swagger UI  : http://localhost:5276/swagger
echo   SignalR Realtime Hub : http://localhost:5276/hubs/notifications
echo ===============================================================================
echo.
echo   DUONG DAN KET NOI TU DIEN THOAI / APP CUNG MANG WI-FI:
echo     http://26.38.230.253:5276/api/
echo     http://10.168.222.213:5276/api/
echo ===============================================================================
echo.

cd /d "%~dp0\0859_NguyenQuocHuy_WebAPI"

dotnet run --project "DA Restaurant Api.csproj"

pause
