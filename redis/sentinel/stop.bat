@echo off
chcp 65001 >nul
echo ==================== Redis Sentinel 停止脚本 ====================
echo.

set REDIS_CLI=redis-cli

echo [1/6] 停止 Sentinel3 (port 26381)...
%REDIS_CLI% -p 26381 shutdown nosave 2>nul
timeout /t 1 >nul

echo [2/6] 停止 Sentinel2 (port 26380)...
%REDIS_CLI% -p 26380 shutdown nosave 2>nul
timeout /t 1 >nul

echo [3/6] 停止 Sentinel1 (port 26379)...
%REDIS_CLI% -p 26379 shutdown nosave 2>nul
timeout /t 1 >nul

echo [4/6] 停止 Redis Slave2 (port 6381)...
%REDIS_CLI% -p 6381 shutdown nosave 2>nul
timeout /t 1 >nul

echo [5/6] 停止 Redis Slave1 (port 6380)...
%REDIS_CLI% -p 6380 shutdown nosave 2>nul
timeout /t 1 >nul

echo [6/6] 停止 Redis Master (port 6379)...
%REDIS_CLI% -p 6379 shutdown nosave 2>nul
timeout /t 1 >nul

echo.
echo ==================== 全部已停止 ====================
pause
