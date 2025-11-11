# Leareng - 영어 문제 생성기

Spring Boot + Vanilla JavaScript로 구현된 한국 수능 영어 문제 생성 및 학습 플랫폼입니다.

## 기능

- ✅ 사용자 인증 (이메일 인증 포함)
- ✅ 구독 관리 (무료/프리미엄)
- ✅ PDF 업로드 및 지문 추출
- ✅ Cerebras GPT 기반 문제 생성 (10가지 유형)
- ✅ 지문 및 문제 저장/조회
- ✅ 학습 모드 (문제 풀이, 정답 확인, 해설)
- ✅ 한국어 번역 제공
- ✅ 관리자 구독 승인 시스템

## 기술 스택

### Backend
- Spring Boot 3.2.0
- Spring Security + JWT
- Spring Data JPA
- MySQL 8.0
- Cerebras API
- Apache PDFBox

### Frontend
- Vanilla JavaScript
- HTML5 / CSS3
- Fetch API

## 프로젝트 구조

```
leareng/
├── backend/              # Spring Boot REST API
│   ├── src/main/java/com/leareng/
│   │   ├── controller/   # REST 컨트롤러
│   │   ├── service/      # 비즈니스 로직
│   │   ├── entity/       # JPA 엔티티
│   │   ├── repository/   # JPA 레포지토리
│   │   ├── security/     # JWT, Spring Security
│   │   └── dto/          # 데이터 전송 객체
│   └── pom.xml
├── frontend/             # Vanilla JS SPA
│   ├── index.html
│   ├── login.html
│   ├── css/
│   └── js/
└── docker-compose.yml
```

## 환경 설정

### 1. 환경 변수 설정

`.env` 파일을 생성하고 다음 변수들을 설정하세요:

```env
DB_USERNAME=root
DB_PASSWORD=your_password
EMAIL_ADDRESS=your_email@gmail.com
EMAIL_PASSWORD=your_app_password
JWT_SECRET_KEY=your-secret-key-min-32-characters
CEREBRAS_API_KEY=your-cerebras-api-key
```

### 2. MySQL 데이터베이스 생성

```sql
CREATE DATABASE leareng CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

## 실행 방법

### Docker를 사용한 실행 (권장)

```bash
docker-compose up -d
```

백엔드: http://localhost:8080
프론트엔드: http://localhost

### 로컬 개발 환경

#### Backend 실행

```bash
cd backend
mvn clean install
mvn spring-boot:run
```

#### Frontend 실행

```bash
cd frontend
python3 -m http.server 8080
# 또는
npx http-server -p 8080
```

프론트엔드에서 API 호출 시 CORS 설정을 확인하세요.

## API 엔드포인트

### 인증
- `POST /api/auth/register` - 회원가입
- `POST /api/auth/login` - 로그인
- `POST /api/auth/verify-email` - 이메일 인증
- `POST /api/auth/reset-password-request` - 비밀번호 재설정 요청
- `POST /api/auth/reset-password` - 비밀번호 재설정

### 지문
- `GET /api/passages` - 지문 목록
- `POST /api/passages` - 지문 저장
- `GET /api/passages/{id}` - 지문 상세

### 문제
- `POST /api/questions/generate` - 문제 생성
- `GET /api/questions/passage/{passageId}` - 지문의 문제 목록

### 구독
- `POST /api/subscription/request` - 구독 신청
- `GET /api/subscription/status` - 구독 상태 확인
- `GET /api/subscription/pending` - 관리자: 대기 중인 구독 요청
- `POST /api/subscription/approve/{requestId}` - 관리자: 승인
- `POST /api/subscription/deny/{requestId}` - 관리자: 거절

### PDF
- `POST /api/pdf/upload` - PDF 업로드 및 텍스트 추출

## 문제 유형

1. Title/Theme Inference (제목/주제 추론)
2. Blank Inference (빈칸 추론)
3. Sentence Insertion (문장 삽입)
4. Paragraph Order (문단 순서)
5. Vocabulary in Context (어휘)
6. Main Idea/Argument (주제/논지)
7. Implied Meaning (함축적 의미)
8. Irrelevant Sentence (관련 없는 문장)
9. Paragraph Summary (문단 요약)
10. Reading Comprehension (종합 독해)

## 배포

무료로 배포하는 방법은 [DEPLOYMENT.md](./DEPLOYMENT.md)를 참조하세요.

### 빠른 배포 (Railway 추천)

1. https://railway.app 접속 및 GitHub 로그인
2. "New Project" → "Deploy from GitHub repo" 선택
3. 환경 변수 설정 (env.example 참고)
4. 자동 배포 완료!

자세한 배포 가이드는 [DEPLOYMENT.md](./DEPLOYMENT.md)를 확인하세요.

## 라이선스

이 프로젝트는 오픈소스 라이선스를 따릅니다. 자세한 내용은 LICENSES.md를 참조하세요.

## 문의

- 문의전화: 010-9493-6576
