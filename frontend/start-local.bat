@echo off
REM 로컬 프론트엔드 개발 서버 시작 스크립트 (Windows)

echo 🚀 로컬 프론트엔드 개발 서버 시작...
echo.
echo 📝 Railway 백엔드 URL 확인:
echo    index.html의 window.BACKEND_URL을 확인하세요
echo.
echo 🌐 서버 시작 중...
echo    브라우저에서 http://localhost:3000 접속
echo.
echo ⚠️  종료하려면 Ctrl+C를 누르세요
echo.

REM Python이 있는지 확인
python --version >nul 2>&1
if %errorlevel% == 0 (
    python -m http.server 3000
) else (
    echo ❌ Python이 설치되어 있지 않습니다.
    echo 다음 명령어 중 하나를 사용하세요:
    echo   - npx http-server -p 3000
    echo   - VS Code Live Server 확장 사용
    pause
    exit /b 1
)

