@echo off
echo Running Newman Tests...

docker run --rm -t ^
    -v "%cd%\docs":/etc/newman ^
    --network host ^
    postman/newman ^
    run "newman-v1.2.json" ^
    --verbose

if %ERRORLEVEL% NEQ 0 (
    echo Tests failed!
    exit /b %ERRORLEVEL%
)

echo Tests passed successfully!
