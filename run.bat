@echo off
chcp 65001 > nul
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

REM === Tat server cu neu dang chay ===
echo [*] Kiem tra va tat server cu tren port 5276...
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":5276" ^| findstr "LISTENING" 2^>nul') do (
    echo     [!] Tim thay tien trinh cu PID=%%a - Dang tat...
    taskkill /PID %%a /F >nul 2>&1
)
timeout /t 2 /nopr >nul

echo [*] Dang khoi dong Web API...
echo.

cd /d "%~dp0\0859_NguyenQuocHuy_WebAPI"

dotnet run --project "DA Restaurant Api.csproj"

pause
