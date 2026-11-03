@echo off
chcp 65001 > nul
title DA Restaurant Api - He Thong Quan Ly Nha Hang & POS Realtime
color 0B
cls
echo ===============================================================================
echo                DA RESTAURANT API - HỆ THỐNG QUẢN LÝ NHÀ HÀNG & POS
echo                  ĐỒNG BỘ ĐA THIẾT BỊ & REAL-TIME NOTIFICATION
echo ===============================================================================
echo.
echo [+] Database: SQLite (RestaurantPOS.db - San sang su dung khong can SQL Server)
echo [+] Cong ket noi: http://0.0.0.0:5276 (Mo cho tat ca dien thoai & thiet bi khac)
echo [+] He thong thong bao Real-time qua SignalR: BAT
echo.
echo [*] Dang khoi dong Web API...
echo.

cd /d "%~dp0\0859_NguyenQuocHuy_WebAPI"

dotnet run --project "DA Restaurant Api.csproj"

pause
