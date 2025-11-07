class AuthManager {
    constructor() {
        this.token = localStorage.getItem('token');
        this.userEmail = localStorage.getItem('userEmail');
        this.isAdmin = localStorage.getItem('isAdmin') === 'true';
    }

    isAuthenticated() {
        return !!this.token;
    }

    setAuth(token, email, isAdmin) {
        this.token = token;
        this.userEmail = email;
        this.isAdmin = isAdmin;
        localStorage.setItem('token', token);
        localStorage.setItem('userEmail', email);
        localStorage.setItem('isAdmin', isAdmin);
    }

    clearAuth() {
        this.token = null;
        this.userEmail = null;
        this.isAdmin = false;
        localStorage.removeItem('token');
        localStorage.removeItem('userEmail');
        localStorage.removeItem('isAdmin');
    }

    async login(email, password) {
        try {
            const response = await api.post('/auth/login', { email, password });
            this.setAuth(response.token, response.email, response.isAdmin || false);
            return response;
        } catch (error) {
            throw error;
        }
    }

    async register(email, password) {
        try {
            const response = await api.post('/auth/register', { email, password });
            return response;
        } catch (error) {
            throw error;
        }
    }

    async verifyEmail(email, token) {
        try {
            const response = await api.post('/auth/verify-email', { email, token });
            return response;
        } catch (error) {
            throw error;
        }
    }

    async requestPasswordReset(email) {
        try {
            const response = await api.post('/auth/reset-password-request', { email });
            return response;
        } catch (error) {
            throw error;
        }
    }

    async resetPassword(token, newPassword) {
        try {
            const response = await api.post('/auth/reset-password', { token, newPassword });
            return response;
        } catch (error) {
            throw error;
        }
    }

    logout() {
        this.clearAuth();
        window.location.href = 'login.html';
    }
}

const authManager = new AuthManager();

// Login page handlers
if (document.getElementById('loginForm')) {
    document.getElementById('loginForm').addEventListener('submit', async (e) => {
        e.preventDefault();
        const email = document.getElementById('loginEmail').value;
        const password = document.getElementById('loginPassword').value;
        const messageEl = document.getElementById('loginMessage');

        try {
            await authManager.login(email, password);
            messageEl.textContent = '로그인 성공!';
            messageEl.className = 'message success';
            setTimeout(() => {
                window.location.href = 'index.html';
            }, 1000);
        } catch (error) {
            messageEl.textContent = error.message;
            messageEl.className = 'message error';
        }
    });
}

// Register page handlers
if (document.getElementById('registerForm')) {
    let registrationEmail = null;

    document.getElementById('registerForm').addEventListener('submit', async (e) => {
        e.preventDefault();
        const email = document.getElementById('registerEmail').value;
        const password = document.getElementById('registerPassword').value;
        const passwordConfirm = document.getElementById('registerPasswordConfirm').value;
        const messageEl = document.getElementById('registerMessage');

        if (password !== passwordConfirm) {
            messageEl.textContent = '비밀번호가 일치하지 않습니다.';
            messageEl.className = 'message error';
            return;
        }

        try {
            await authManager.register(email, password);
            registrationEmail = email;
            document.getElementById('registerForm').style.display = 'none';
            document.getElementById('verificationSection').style.display = 'block';
            document.getElementById('verificationEmail').textContent = `이메일(${email})로 발송된 인증 코드를 입력해주세요.`;
            messageEl.textContent = '인증 이메일이 발송되었습니다.';
            messageEl.className = 'message success';
        } catch (error) {
            messageEl.textContent = error.message;
            messageEl.className = 'message error';
        }
    });

    if (document.getElementById('verifyBtn')) {
        document.getElementById('verifyBtn').addEventListener('click', async () => {
            const code = document.getElementById('verificationCode').value;
            const messageEl = document.getElementById('registerMessage');

            if (!registrationEmail) {
                messageEl.textContent = '회원가입을 먼저 진행해주세요.';
                messageEl.className = 'message error';
                return;
            }

            try {
                await authManager.verifyEmail(registrationEmail, code);
                messageEl.textContent = '이메일 인증이 완료되었습니다!';
                messageEl.className = 'message success';
                setTimeout(() => {
                    window.location.href = 'login.html';
                }, 2000);
            } catch (error) {
                messageEl.textContent = error.message;
                messageEl.className = 'message error';
            }
        });
    }

    if (document.getElementById('resendBtn')) {
        document.getElementById('resendBtn').addEventListener('click', async () => {
            const messageEl = document.getElementById('registerMessage');

            if (!registrationEmail) {
                messageEl.textContent = '회원가입을 먼저 진행해주세요.';
                messageEl.className = 'message error';
                return;
            }

            try {
                const email = registrationEmail;
                const password = 'temp'; // 비밀번호는 서버에서 검증하지 않고 재발송만 함
                await api.post('/auth/resend-verification', { email });
                messageEl.textContent = '인증 코드가 재발송되었습니다.';
                messageEl.className = 'message success';
            } catch (error) {
                messageEl.textContent = error.message;
                messageEl.className = 'message error';
            }
        });
    }
}

// Password reset handlers
if (document.getElementById('resetForm')) {
    let resetEmail = null;

    document.getElementById('resetForm').addEventListener('submit', async (e) => {
        e.preventDefault();
        const email = document.getElementById('resetEmailInput').value;
        const messageEl = document.getElementById('resetMessage');

        try {
            await authManager.requestPasswordReset(email);
            resetEmail = email;
            document.getElementById('resetForm').style.display = 'none';
            document.getElementById('resetCodeSection').style.display = 'block';
            document.getElementById('resetEmail').textContent = `이메일(${email})로 발송된 인증 코드를 입력해주세요.`;
            messageEl.textContent = '비밀번호 재설정 이메일이 발송되었습니다.';
            messageEl.className = 'message success';
        } catch (error) {
            messageEl.textContent = error.message;
            messageEl.className = 'message error';
        }
    });

    if (document.getElementById('resetPasswordBtn')) {
        document.getElementById('resetPasswordBtn').addEventListener('click', async () => {
            const code = document.getElementById('resetCode').value;
            const newPassword = document.getElementById('newPassword').value;
            const newPasswordConfirm = document.getElementById('newPasswordConfirm').value;
            const messageEl = document.getElementById('resetMessage');

            if (newPassword !== newPasswordConfirm) {
                messageEl.textContent = '비밀번호가 일치하지 않습니다.';
                messageEl.className = 'message error';
                return;
            }

            try {
                await authManager.resetPassword(code, newPassword);
                messageEl.textContent = '비밀번호가 성공적으로 변경되었습니다!';
                messageEl.className = 'message success';
                setTimeout(() => {
                    window.location.href = 'login.html';
                }, 2000);
            } catch (error) {
                messageEl.textContent = error.message;
                messageEl.className = 'message error';
            }
        });
    }
}

