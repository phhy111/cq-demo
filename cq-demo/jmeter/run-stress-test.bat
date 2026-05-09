@echo off
chcp 65001 >nul
echo ============================================
echo   CQ-Demo 重庆旅游平台 JMeter 压测脚本
echo ============================================
echo.

set JMETER_HOME=
where jmeter >nul 2>nul
if %ERRORLEVEL% EQU 0 (
    set JMETER_CMD=jmeter
) else (
    if defined JMETER_HOME (
        set JMETER_CMD=%JMETER_HOME%\bin\jmeter.bat
    ) else (
        echo [错误] 未找到 JMeter！
        echo 请执行以下任一操作：
        echo   1. 将 JMeter 的 bin 目录添加到系统 PATH
        echo   2. 设置 JMETER_HOME 环境变量
        echo   3. 下载 JMeter: https://jmeter.apache.org/download_jmeter.cgi
        pause
        exit /b 1
    )
)

set SCRIPT_DIR=%~dp0
set JMX_FILE=%SCRIPT_DIR%cq-demo-stress-test.jmx
set RESULT_DIR=%SCRIPT_DIR%results

if not exist "%RESULT_DIR%" mkdir "%RESULT_DIR%"

echo [信息] 压测脚本: %JMX_FILE%
echo [信息] 结果目录: %RESULT_DIR%
echo.

if "%1"=="gui" (
    echo [模式] GUI 模式启动 JMeter...
    "%JMETER_CMD%" -t "%JMX_FILE%"
) else (
    set TIMESTAMP=%date:~0,4%%date:~5,2%%date:~8,2%_%time:~0,2%%time:~3,2%%time:~6,2%
    set TIMESTAMP=%TIMESTAMP: =0%
    set JTL_FILE=%RESULT_DIR%\result_%TIMESTAMP%.jtl
    set LOG_FILE=%RESULT_DIR%\jmeter_%TIMESTAMP%.log

    echo [模式] 非 GUI 模式（命令行压测）
    echo [输出] 结果文件: %JTL_FILE%
    echo [输出] 日志文件: %LOG_FILE%
    echo.
    echo [提示] 默认参数: 50线程, 10秒启动, 持续60秒
    echo [提示] 如需修改，请编辑 .jmx 文件中的用户定义变量
    echo.
    echo 开始压测...
    echo.

    "%JMETER_CMD%" -n -t "%JMX_FILE%" -l "%JTL_FILE%" -j "%LOG_FILE%" -e -o "%RESULT_DIR%\report_%TIMESTAMP%"

    echo.
    echo ============================================
    echo   压测完成！
    echo ============================================
    echo [结果] %JTL_FILE%
    echo [日志] %LOG_FILE%
    echo [报告] %RESULT_DIR%\report_%TIMESTAMP%
    echo.
    echo 用浏览器打开报告目录中的 index.html 查看详细报告
)

pause
