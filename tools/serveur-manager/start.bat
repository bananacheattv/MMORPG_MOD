@echo off
chcp 65001 >nul
title Aeloria Panel
cd /d "%~dp0"
:loop
node server.js
echo.
echo [start.bat] Le panel s'est arrete, relance dans 3 secondes... (Ctrl+C pour quitter)
timeout /t 3 /nobreak >nul
goto loop
