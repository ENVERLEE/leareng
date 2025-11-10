#!/bin/bash

# 로컬 프론트엔드 개발 서버 시작 스크립트

echo "🚀 로컬 프론트엔드 개발 서버 시작..."
echo ""
echo "📝 Railway 백엔드 URL 확인:"
echo "   index.html의 window.BACKEND_URL을 확인하세요"
echo ""
echo "🌐 서버 시작 중..."
echo "   브라우저에서 http://localhost:3000 접속"
echo ""
echo "⚠️  종료하려면 Ctrl+C를 누르세요"
echo ""

# Python 3가 있는지 확인
if command -v python3 &> /dev/null; then
    python3 -m http.server 3000
elif command -v python &> /dev/null; then
    python -m http.server 3000
else
    echo "❌ Python이 설치되어 있지 않습니다."
    echo "다음 명령어 중 하나를 사용하세요:"
    echo "  - npx http-server -p 3000"
    echo "  - VS Code Live Server 확장 사용"
    exit 1
fi

