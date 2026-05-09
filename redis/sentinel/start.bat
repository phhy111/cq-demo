@echo off
chcp 65001 >nul
echo ==================== Redis Sentinel 启动脚本 ====================
echo.

REM 请根据实际 Redis 安装路径修改
set REDIS_SERVER=redis-server
set REDIS_CLI=redis-cli

echo [1/6] 启动 Redis Master (port 6379)...
start "Redis-Master" %REDIS_SERVER% redis-master.conf
timeout /t 2 >nul

echo [2/6] 启动 Redis Slave1 (port 6380)...
start "Redis-Slave1" %REDIS_SERVER% redis-slave1.conf
timeout /t 2 >nul

echo [3/6] 启动 Redis Slave2 (port 6381)...
start "Redis-Slave2" %REDIS_SERVER% redis-slave2.conf
timeout /t 2 >nul

echo [4/6] 启动 Sentinel1 (port 26379)...
start "Sentinel-1" %REDIS_SERVER% sentinel-1.conf --sentinel
timeout /t 2 >nul

echo [5/6] 启动 Sentinel2 (port 26380)...
start "Sentinel-2" %REDIS_SERVER% sentinel-2.conf --sentinel
timeout /t 2 >nul

echo [6/6] 启动 Sentinel3 (port 26381)...
start "Sentinel-3" %REDIS_SERVER% sentinel-3.conf --sentinel
timeout /t 2 >nul

echo.
echo ==================== 启动完成 ====================
echo Master:    127.0.0.1:6379
echo Slave1:    127.0.0.1:6380
echo Slave2:    127.0.0.1:6381
echo Sentinel1: 127.0.0.1:26379
echo Sentinel2: 127.0.0.1:26380
echo Sentinel3: 127.0.0.1:26381
echo.
echo 验证命令: redis-cli -p 26379 sentinel master mymaster
echo.
pause
