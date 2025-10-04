@echo off
REM Load Test Runner for SHACL Validation Cache (Windows)

setlocal enabledelayedexpansion

echo.
echo ╔════════════════════════════════════════════════════════════════════════╗
echo ║  SHACL Validation Cache - Load Test Runner                            ║
echo ╚════════════════════════════════════════════════════════════════════════╝
echo.

REM Parse arguments
set MODE=%1
set COUNT=%2
set PAYLOAD=%3
set DELAY=%4
set THREADS=%5

REM Set defaults
if "%COUNT%"=="" set COUNT=10
if "%PAYLOAD%"=="" set PAYLOAD=1
if "%DELAY%"=="" set DELAY=100
if "%THREADS%"=="" set THREADS=1

if "%MODE%"=="" (
    call :show_usage
    exit /b 0
)

REM Check if BigchainDB is running
echo [INFO] Checking if BigchainDB is running...
curl -s http://localhost:9984/ >nul 2>&1
if errorlevel 1 (
    echo [ERROR] BigchainDB is not accessible at http://localhost:9984/
    echo [INFO] Run: cd ..\smartchaindb ^&^& docker-compose up -d
    exit /b 1
)
echo [OK] BigchainDB is running
echo.

REM Compile code
echo [INFO] Compiling Java code...
call mvn clean compile -q
if errorlevel 1 (
    echo [ERROR] Compilation failed
    exit /b 1
)
echo [OK] Compilation successful
echo.

REM Run test
if "%MODE%"=="all" (
    call :run_all_tests
) else (
    call :run_test %MODE% %COUNT% %PAYLOAD% %DELAY% %THREADS%
)

echo.
echo [INFO] To view server metrics, run:
echo   curl http://localhost:9984/api/v1/metrics/validation
echo.

exit /b 0

:run_test
    set TEST_MODE=%1
    set TEST_COUNT=%2
    set TEST_PAYLOAD=%3
    set TEST_DELAY=%4
    set TEST_THREADS=%5
    
    echo [INFO] Running %TEST_MODE% test...
    echo.
    
    call mvn exec:java ^
        -Dexec.mainClass="com.bigchaindb.smartchaindb.driver.LoadTestDriver" ^
        -Dexec.args="%TEST_MODE% %TEST_COUNT% %TEST_PAYLOAD% %TEST_DELAY% %TEST_THREADS%" ^
        -q
    
    echo.
    echo [OK] Test completed
    echo.
    
    exit /b 0

:run_all_tests
    echo [INFO] Running all test scenarios...
    echo.
    
    echo === Test 1: Single Transaction ===
    call :run_test single 1 1 0 1
    timeout /t 2 /nobreak >nul
    
    echo === Test 2: Small Batch (10 TXs) ===
    call :run_test batch 10 1 100 1
    timeout /t 2 /nobreak >nul
    
    echo === Test 3: Cache Effectiveness ===
    call :run_test cache 5 1 100 1
    timeout /t 2 /nobreak >nul
    
    echo === Test 4: Marketplace Flow (3 flows) ===
    call :run_test marketplace 3 2 1000 1
    timeout /t 2 /nobreak >nul
    
    echo === Test 5: Larger Batch (20 TXs, 5KB each) ===
    call :run_test batch 20 5 200 1
    timeout /t 2 /nobreak >nul
    
    echo === All Tests Complete ===
    
    echo.
    echo [INFO] Fetching server-side metrics...
    curl -s http://localhost:9984/api/v1/metrics/validation
    echo.
    
    exit /b 0

:show_usage
    echo Usage: %0 ^<mode^> [options]
    echo.
    echo Modes:
    echo   single       - Single transaction test
    echo   batch        - Batch of unique transactions
    echo   marketplace  - Full marketplace flow
    echo   stress       - Multi-threaded stress test
    echo   cache        - Cache effectiveness test
    echo   all          - Run all tests
    echo.
    echo Options (mode-specific):
    echo   count        - Number of transactions (default: 10)
    echo   payloadKB    - Payload size in KB (default: 1)
    echo   delayMs      - Delay between TXs in ms (default: 100)
    echo   threads      - Number of threads for stress test (default: 1)
    echo.
    echo Examples:
    echo   %0 single
    echo   %0 batch 50 2
    echo   %0 marketplace 5 5 1000
    echo   %0 stress 100 1 0 4
    echo   %0 cache 10
    echo   %0 all
    echo.
    exit /b 0


