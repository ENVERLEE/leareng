// API URL 자동 감지 (배포 환경에 따라 자동 설정)
const getApiBaseUrl = () => {
    // 프로덕션 환경에서 백엔드 URL이 설정되어 있는지 확인
    const hostname = window.location.hostname;
    
    // 로컬 개발 환경
    if (hostname === 'localhost' || hostname === '127.0.0.1') {
        return 'http://localhost:8080/api';
    }
    
    // 프로덕션 환경 - 같은 도메인 사용 (프론트엔드와 백엔드가 같은 서버에 있을 때)
    // 또는 환경 변수로 설정된 백엔드 URL 사용
    const backendUrl = window.BACKEND_URL || '';
    if (backendUrl) {
        return `${backendUrl}/api`;
    }
    
    // 기본값: 같은 origin 사용 (프론트엔드와 백엔드가 같은 도메인)
    // Railway나 Render에서 같은 서비스로 배포할 때
    return '/api';
    
    // 프론트엔드와 백엔드가 다른 도메인에 있을 때는
    // index.html의 <script> 태그에서 window.BACKEND_URL을 설정하세요
    // 예: <script>window.BACKEND_URL = 'https://your-backend.railway.app';</script>
};

const API_BASE_URL = getApiBaseUrl();

// 디버깅용: API Base URL 확인
console.log('API Base URL:', API_BASE_URL);

class ApiClient {
    constructor() {
        this.baseUrl = API_BASE_URL;
    }

    getToken() {
        return localStorage.getItem('token');
    }

    async request(endpoint, options = {}) {
        const url = `${this.baseUrl}${endpoint}`;
        const token = this.getToken();
        
        const headers = {
            'Content-Type': 'application/json',
            ...options.headers
        };

        if (token) {
            headers['Authorization'] = `Bearer ${token}`;
        }

        const config = {
            ...options,
            headers
        };

        try {
            const response = await fetch(url, config);
            
            // Handle 401 Unauthorized
            if (response.status === 401) {
                localStorage.removeItem('token');
                localStorage.removeItem('userEmail');
                localStorage.removeItem('isAdmin');
                window.location.href = 'login.html';
                throw new Error('인증이 만료되었습니다. 다시 로그인해주세요.');
            }

            // JSON 파싱 시도
            let data;
            const contentType = response.headers.get('content-type');
            if (contentType && contentType.includes('application/json')) {
                try {
                    data = await response.json();
                } catch (jsonError) {
                    console.error('JSON 파싱 오류:', jsonError);
                    const text = await response.text();
                    console.error('응답 본문:', text);
                    throw new Error(`서버 응답을 파싱할 수 없습니다. (${response.status})`);
                }
            } else {
                // JSON이 아닌 경우 텍스트로 읽기
                const text = await response.text();
                console.error('예상하지 못한 응답 형식:', text);
                throw new Error(`서버가 예상하지 못한 형식으로 응답했습니다. (${response.status})`);
            }
            
            if (!response.ok) {
                const errorMessage = data?.error || data?.message || `요청 실패 (${response.status})`;
                console.error('API 오류 응답:', {
                    status: response.status,
                    statusText: response.statusText,
                    url: url,
                    data: data
                });
                throw new Error(errorMessage);
            }
            
            return data;
        } catch (error) {
            // 네트워크 오류 처리
            if (error instanceof TypeError && error.message.includes('fetch')) {
                console.error('네트워크 오류:', {
                    url: url,
                    error: error.message
                });
                throw new Error('서버에 연결할 수 없습니다. 네트워크 연결을 확인해주세요.');
            }
            
            // 이미 Error 객체인 경우 그대로 전달
            if (error instanceof Error) {
                console.error('API 오류:', {
                    message: error.message,
                    url: url,
                    stack: error.stack
                });
                throw error;
            }
            
            // 기타 오류
            console.error('알 수 없는 오류:', error);
            throw new Error('알 수 없는 오류가 발생했습니다.');
        }
    }

    async get(endpoint) {
        return this.request(endpoint, { method: 'GET' });
    }

    async post(endpoint, body) {
        return this.request(endpoint, {
            method: 'POST',
            body: JSON.stringify(body)
        });
    }

    async postFormData(endpoint, formData) {
        const url = `${this.baseUrl}${endpoint}`;
        const token = this.getToken();
        
        const headers = {};
        if (token) {
            headers['Authorization'] = `Bearer ${token}`;
        }

        try {
            const response = await fetch(url, {
                method: 'POST',
                headers,
                body: formData
            });

            // Handle 401 Unauthorized
            if (response.status === 401) {
                localStorage.removeItem('token');
                localStorage.removeItem('userEmail');
                localStorage.removeItem('isAdmin');
                window.location.href = 'login.html';
                throw new Error('인증이 만료되었습니다. 다시 로그인해주세요.');
            }

            // JSON 파싱 시도
            let data;
            const contentType = response.headers.get('content-type');
            if (contentType && contentType.includes('application/json')) {
                try {
                    data = await response.json();
                } catch (jsonError) {
                    console.error('JSON 파싱 오류:', jsonError);
                    const text = await response.text();
                    console.error('응답 본문:', text);
                    throw new Error(`서버 응답을 파싱할 수 없습니다. (${response.status})`);
                }
            } else {
                // JSON이 아닌 경우 텍스트로 읽기
                const text = await response.text();
                console.error('예상하지 못한 응답 형식:', text);
                throw new Error(`서버가 예상하지 못한 형식으로 응답했습니다. (${response.status})`);
            }
            
            if (!response.ok) {
                const errorMessage = data?.error || data?.message || `요청 실패 (${response.status})`;
                console.error('API 오류 응답:', {
                    status: response.status,
                    statusText: response.statusText,
                    url: url,
                    data: data
                });
                throw new Error(errorMessage);
            }
            
            return data;
        } catch (error) {
            // 네트워크 오류 처리
            if (error instanceof TypeError && error.message.includes('fetch')) {
                console.error('네트워크 오류:', {
                    url: url,
                    error: error.message
                });
                throw new Error('서버에 연결할 수 없습니다. 네트워크 연결을 확인해주세요.');
            }
            
            // 이미 Error 객체인 경우 그대로 전달
            if (error instanceof Error) {
                console.error('API 오류:', {
                    message: error.message,
                    url: url,
                    stack: error.stack
                });
                throw error;
            }
            
            // 기타 오류
            console.error('알 수 없는 오류:', error);
            throw new Error('알 수 없는 오류가 발생했습니다.');
        }
    }
}

const api = new ApiClient();

