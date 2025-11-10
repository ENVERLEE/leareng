# 무료 배포 가이드

이 프로젝트를 무료로 배포하는 방법을 안내합니다.

## 추천 배포 플랫폼

### 1. Railway (추천) ⭐
- **무료 티어**: 월 $5 크레딧 (충분함)
- **장점**: Docker 지원, 자동 배포, 환경 변수 관리 쉬움
- **URL**: https://railway.app

### 2. Render
- **무료 티어**: 제한적이지만 사용 가능
- **장점**: 무료 SSL, 자동 배포
- **URL**: https://render.com

### 3. Fly.io
- **무료 티어**: 제한적
- **장점**: 전 세계 CDN, 빠른 속도
- **URL**: https://fly.io

---

## Railway로 배포하기 (추천)

### 1단계: Railway 계정 생성
1. https://railway.app 접속
2. GitHub로 로그인
3. "New Project" 클릭
4. "Deploy from GitHub repo" 선택

### 2단계: 백엔드 배포
1. 프로젝트에서 "New Service" 클릭
2. "GitHub Repo" 선택
3. 저장소 선택 후 "Deploy" 클릭
4. Railway가 자동으로 Dockerfile을 감지

### 3단계: 환경 변수 설정
Railway 대시보드에서 "Variables" 탭에서 다음 환경 변수 추가:

```bash
# 데이터베이스
SPRING_DATASOURCE_URL=your_oracle_connection_string
DB_USERNAME=your_db_username
DB_PASSWORD=your_db_password

# 이메일 (Gmail 사용 시)
EMAIL_ADDRESS=your_email@gmail.com
EMAIL_PASSWORD=your_app_password
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587

# JWT
JWT_SECRET_KEY=your-very-secret-key-change-this

# OpenAI
OPENAI_API_KEY=your_openai_api_key

# 프론트엔드 URL (Railway가 제공하는 도메인)
FRONTEND_URL=https://your-frontend-domain.railway.app
```

### 4단계: 프론트엔드 배포
1. 같은 프로젝트에서 "New Service" 클릭
2. "Static Website" 선택
3. `frontend` 폴더를 선택하거나 GitHub에서 배포
4. 또는 별도 서비스로 nginx 이미지 사용

### 5단계: 프론트엔드 API URL 수정
`frontend/js/api.js` 파일에서 백엔드 URL을 Railway 도메인으로 변경:

```javascript
const API_BASE_URL = 'https://your-backend.railway.app/api';
```

### 6단계: 도메인 설정 (선택사항)
1. Railway 대시보드에서 각 서비스의 "Settings" 클릭
2. "Generate Domain" 클릭하여 무료 도메인 생성
3. 또는 커스텀 도메인 연결 가능

---

## Render로 배포하기

### 1단계: Render 계정 생성
1. https://render.com 접속
2. GitHub로 로그인

### 2단계: 백엔드 배포
1. "New +" → "Web Service" 클릭
2. GitHub 저장소 연결
3. 설정:
   - **Name**: leareng-backend
   - **Environment**: Docker
   - **Dockerfile Path**: `backend/Dockerfile`
   - **Root Directory**: `backend`
4. 환경 변수 추가 (위와 동일)
5. "Create Web Service" 클릭

### 3단계: 프론트엔드 배포
1. "New +" → "Static Site" 클릭
2. GitHub 저장소 연결
3. 설정:
   - **Name**: leareng-frontend
   - **Root Directory**: `frontend`
   - **Build Command**: (비워두기)
   - **Publish Directory**: `frontend`
4. "Create Static Site" 클릭

---

## Fly.io로 배포하기

### 1단계: Fly.io CLI 설치
```bash
# macOS
curl -L https://fly.io/install.sh | sh

# 또는 Homebrew
brew install flyctl
```

### 2단계: 로그인
```bash
fly auth login
```

### 3단계: 백엔드 배포
```bash
cd backend
fly launch
# 프롬프트에 따라 설정
fly secrets set SPRING_DATASOURCE_URL="your_url"
fly secrets set DB_USERNAME="your_username"
fly secrets set DB_PASSWORD="your_password"
fly secrets set EMAIL_ADDRESS="your_email"
fly secrets set EMAIL_PASSWORD="your_password"
fly secrets set JWT_SECRET_KEY="your_secret"
fly secrets set OPENAI_API_KEY="your_key"
fly secrets set FRONTEND_URL="https://your-frontend.fly.dev"
fly deploy
```

### 4단계: 프론트엔드 배포
```bash
cd frontend
# fly.toml 생성 필요
fly launch
fly deploy
```

---

## 프론트엔드와 백엔드 분리 배포

### 옵션 1: Vercel (프론트엔드) + Railway (백엔드)
1. **프론트엔드**: Vercel에 배포 (무료, 매우 빠름)
   - https://vercel.com
   - GitHub 저장소 연결
   - Root Directory: `frontend`
   - Build Command: (없음)
   - Output Directory: `frontend`

2. **백엔드**: Railway에 배포
   - 위의 Railway 가이드 참고

3. **API URL 수정**
   - Vercel 배포 후 도메인 확인
   - `frontend/js/api.js`에서 백엔드 URL 설정
   - `application.properties`에서 `FRONTEND_URL`을 Vercel 도메인으로 설정

### 옵션 2: Netlify (프론트엔드) + Render (백엔드)
1. **프론트엔드**: Netlify에 배포
   - https://netlify.com
   - GitHub 저장소 연결
   - Base directory: `frontend`
   - Publish directory: `frontend`

2. **백엔드**: Render에 배포
   - 위의 Render 가이드 참고

---

## 배포 전 체크리스트

- [ ] 환경 변수 모두 설정
- [ ] `application.properties`에서 `FRONTEND_URL` 설정
- [ ] `frontend/js/api.js`에서 백엔드 URL 설정
- [ ] Oracle 데이터베이스 접근 가능한지 확인
- [ ] 이메일 SMTP 설정 확인
- [ ] OpenAI API 키 설정
- [ ] JWT Secret Key를 강력한 값으로 변경

---

## 무료 티어 제한사항

### Railway
- 월 $5 크레딧
- 사용량에 따라 제한
- 슬리프 모드 (비활성 시 자동 중지)

### Render
- 무료 서비스는 15분 비활성 후 슬리프
- 첫 요청 시 느릴 수 있음
- 월간 제한 있음

### Fly.io
- 무료 티어 제한적
- 사용량 초과 시 유료 전환 필요

---

## 트러블슈팅

### 백엔드가 시작되지 않을 때
1. Railway/Render 로그 확인
2. 환경 변수 확인
3. 포트 설정 확인 (Spring Boot는 8080 사용)

### CORS 오류
1. `SecurityConfig.java`에서 프론트엔드 도메인 추가
2. `application.properties`의 `cors.allowed-origins` 확인

### 데이터베이스 연결 실패
1. Oracle Cloud 방화벽 설정 확인
2. 연결 문자열 형식 확인
3. TLS/SSL 설정 확인

---

## 추천 구성

**가장 안정적인 무료 배포:**
- **프론트엔드**: Vercel (무료, 빠름, CDN)
- **백엔드**: Railway (무료 티어, 안정적)
- **데이터베이스**: Oracle Cloud Always Free (이미 사용 중)

이 구성이 가장 비용 효율적이고 안정적입니다!

