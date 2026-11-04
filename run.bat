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
echo [*] Dang khoi dong Web API...
echo.

cd /d "%~dp0\0859_NguyenQuocHuy_WebAPI"

dotnet run --project "DA Restaurant Api.csproj" --no-build --configuration Release

pause
