@echo off
title DEPLOY - Tao Web API len GitHub
color 0B
cls
echo ============================================================
echo   DEPLOY WEB API - 0859 NguyenQuocHuy
echo   Buoc 1: Publish  |  Buoc 2: Git push
echo ============================================================
echo.

cd /d "%~dp0\0859_NguyenQuocHuy_WebAPI"

echo [1/3] Dang publish project...
dotnet publish -c Release -o ../publish --nologo
if errorlevel 1 (
    echo [LOI] Publish that bai! Kiem tra lai code.
    pause & exit /b 1
)
echo [OK] Publish thanh cong!
echo.

cd /d "%~dp0"

echo [2/3] Kiem tra Git...
git status 2>nul
if errorlevel 1 (
    echo Chua co Git repo. Dang khoi tao...
    git init
    git branch -M main
)

echo [3/3] Commit va push len GitHub...
git add -A
git commit -m "Web API Restaurant POS - Deploy v%DATE:~0,10%"

echo.
echo ============================================================
echo [TIEP THEO] Push len GitHub:
echo   git remote add origin https://github.com/USERNAME/REPO.git
echo   git push -u origin main
echo.
echo [RENDER] Vao https://render.com -> New Web Service -> Docker
echo [RAILWAY] Vao https://railway.app -> Deploy from GitHub
echo ============================================================
pause

