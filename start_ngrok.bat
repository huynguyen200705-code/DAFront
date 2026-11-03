@echo off
title NGROK TUNNEL - Web API Port 5276
color 0A
cls
echo ============================================================
echo   NGROK TUNNEL - MO API RA INTERNET
echo   Port: 5276  |  MSSV: 0859 - Nguyen Quoc Huy
echo ============================================================
echo.

REM Kiem tra ngrok co khong
where ngrok >nul 2>nul
if errorlevel 1 (
    echo [LOI] Chua tim thay ngrok!
    echo.
    echo Vui long tai ngrok tai: https://ngrok.com/download
    echo Sau do:
    echo   1. Giai nen ngrok.exe vao C:\ngrok\
    echo   2. Them C:\ngrok vao PATH
    echo   3. Chay: ngrok config add-authtoken ^<TOKEN^>
    echo   4. Chay lai file nay
    echo.
    pause
    exit /b 1
)

echo [OK] Da tim thay ngrok!
echo.
echo [!] Dam bao Web API dang chay tai http://localhost:5276
echo     Neu chua chay, mo them 1 cua so CMD va chay: run.bat
echo.
echo [*] Dang mo tunnel...
echo.

ngrok http 5276 --log=stdout

pause

