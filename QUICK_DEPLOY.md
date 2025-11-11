# 빠른 배포 가이드 (5분 안에 배포하기)

## Railway로 배포하기 (가장 쉬움) ⭐

### 1단계: Railway 계정 생성 (1분)
1. https://railway.app 접속
2. "Start a New Project" 클릭
3. GitHub로 로그인
4. 저장소 선택

### 2단계: 백엔드 배포 (2분)
1. "New Service" → "GitHub Repo" 선택
2. 저장소 선택 후 "Deploy" 클릭
3. Railway가 자동으로 Dockerfile 감지
4. "Settings" → "Root Directory"를 `backend`로 설정

### 3단계: 환경 변수 설정 (2분)
"Variables" 탭에서 다음 변수 추가:

```bash
SPRING_DATASOURCE_URL=your_oracle_url
DB_USERNAME=your_username
DB_PASSWORD=your_password
EMAIL_ADDRESS=your_email@gmail.com
EMAIL_PASSWORD=your_app_password
JWT_SECRET_KEY=your-secret-key-32-chars-min
CEREBRAS_API_KEY=your-cerebras-api-key
FRONTEND_URL=https://your-frontend.railway.app
CORS_ALLOWED_ORIGINS=https://your-frontend.railway.app
```

### 4단계: 프론트엔드 배포
1. 같은 프로젝트에서 "New Service" 클릭
2. "Static Website" 선택
3. Root Directory: `frontend`
4. 또는 nginx 사용 시 Dockerfile 생성

### 5단계: 도메인 확인
1. 각 서비스의 "Settings" → "Generate Domain" 클릭
2. 백엔드 도메인을 프론트엔드 `index.html`에 설정
3. 프론트엔드 도메인을 백엔드 환경 변수에 설정

**완료!** 🎉

---

## Render로 배포하기

### 1단계: Render 계정 생성
1. https://render.com 접속
2. GitHub로 로그인

### 2단계: 백엔드 배포
1. "New +" → "Web Service"
2. 저장소 연결
3. 설정:
   - Name: `leareng-backend`
   - Environment: `Docker`
   - Dockerfile Path: `backend/Dockerfile`
   - Root Directory: `backend`
4. 환경 변수 추가 (위와 동일)
5. "Create Web Service"

### 3단계: 프론트엔드 배포
1. "New +" → "Static Site"
2. 저장소 연결
3. 설정:
   - Root Directory: `frontend`
   - Publish Directory: `frontend`

**완료!** 🎉

---

## 배포 후 확인사항

- [ ] 백엔드가 정상 시작되었는지 로그 확인
- [ ] 프론트엔드에서 백엔드 API 호출 테스트
- [ ] 이메일 인증 링크가 정상 작동하는지 확인
- [ ] CORS 오류가 없는지 확인

---

## 문제 해결

### 백엔드가 시작되지 않을 때
- 로그 확인: Railway/Render 대시보드에서 "Logs" 탭
- 환경 변수 확인: 모든 필수 변수가 설정되었는지 확인
- 포트 확인: `PORT` 환경 변수가 설정되어 있는지 확인

### CORS 오류
- `CORS_ALLOWED_ORIGINS`에 프론트엔드 도메인 추가
- `FRONTEND_URL`이 올바르게 설정되었는지 확인

### 이메일이 발송되지 않을 때
- Gmail 앱 비밀번호 사용 확인
- `MAIL_HOST`, `MAIL_PORT` 확인
- Railway/Render의 방화벽 설정 확인

