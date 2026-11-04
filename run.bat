@echo off
chcp 65001 >nul
title DA Restaurant Api - He Thong Quan Ly Nha Hang & POS
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

REM === Buoc 1: Giai phong port 5276 va tat tien trinh cu ===
echo [*] Kiem tra va giai phong port 5276...
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":5276 " ^| findstr "LISTENING" 2^>nul') do (
    echo     [!] Phat hien PID=%%a dang chiem port 5276 - Dang giai phong...
    taskkill /PID %%a /F /T >nul 2>&1
)

REM Tat cac process dotnet/server chay ngam
taskkill /IM "DA_Restaurant_Api.exe" /F >nul 2>&1
taskkill /IM "DA Restaurant Api.exe" /F >nul 2>&1

timeout /t 1 /nobreak >nul

REM === Buoc 2: Kiem tra xac nhan port da san sang ===
netstat -ano | findstr ":5276 " | findstr "LISTENING" >nul 2>&1
if %errorlevel%==0 (
    echo [!] Canh bao: Port 5276 van bi giu, dang thu cuong che giai phong lan 2...
    for /f "tokens=5" %%b in ('netstat -ano ^| findstr ":5276 " ^| findstr "LISTENING" 2^>nul') do (
        taskkill /PID %%b /F /T >nul 2>&1
    )
    timeout /t 1 /nobreak >nul
)

echo [*] Port 5276 da san sang!
echo.
echo ===============================================================================
echo   Web Admin Dashboard : http://localhost:5276/
echo   Tai lieu Swagger UI  : http://localhost:5276/swagger
echo   SignalR Realtime Hub : http://localhost:5276/hubs/notifications
echo ===============================================================================
echo.
echo   DUONG DAN KET NOI TU DIEN THOAI / APP (CUNG MANG WI-FI):
echo     http://10.168.222.213:5276/api/
echo     http://26.38.230.253:5276/api/
echo     http://10.0.2.2:5276/api/ (Dung tren Android Emulator)
echo ===============================================================================
echo.
echo [*] Dang mo trinh duyet Web Admin Dashboard...
start "" http://localhost:5276/
echo [*] Dang khoi dong Web API server...
echo.

cd /d "%~dp0\0859_NguyenQuocHuy_WebAPI"

REM Chay server (su dung no-build de khoi dong cuc nhanh vi da duoc build san)
dotnet run --project "DA Restaurant Api.csproj" --no-restore

if %errorlevel% neq 0 (
    echo.
    echo [!] Server bi dung hoac xay ra loi khi chay.
)

pause
