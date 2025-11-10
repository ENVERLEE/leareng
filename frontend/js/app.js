// Main app initialization
document.addEventListener('DOMContentLoaded', () => {
    if (!authManager.isAuthenticated()) {
        window.location.href = 'login.html';
        return;
    }
    
    // Show user info
    const userEmail = authManager.userEmail;
    document.getElementById('userEmail').textContent = userEmail;
    document.getElementById('userInfo').style.display = 'flex';

    // Show admin link if admin
    if (authManager.isAdmin) {
        document.getElementById('adminLink').style.display = 'inline-block';
    }

    // Logout handler
    document.getElementById('logoutBtn').addEventListener('click', () => {
        authManager.logout();
    });

    // Mode switching
    document.querySelectorAll('.mode-btn').forEach(btn => {
        btn.addEventListener('click', () => {
            const mode = btn.dataset.mode;
            switchMode(mode);
        });
    });

    // Create tab switching
    document.querySelectorAll('.create-tab').forEach(btn => {
        btn.addEventListener('click', () => {
            const tab = btn.dataset.tab;
            switchCreateTab(tab);
        });
    });

    // Load initial data
    loadSubscriptionStatus();
    loadSavedPassages();
    updateStats();
});

function switchMode(mode) {
    // Update mode buttons
    document.querySelectorAll('.mode-btn').forEach(btn => {
        btn.classList.remove('active');
    });
    document.querySelector(`[data-mode="${mode}"]`).classList.add('active');

    // Update mode panels
    document.querySelectorAll('.mode-panel').forEach(panel => {
        panel.classList.remove('active');
    });
    
    const panelId = mode === 'study' ? 'studyMode' : 
                   mode === 'create' ? 'createMode' : 'libraryMode';
    document.getElementById(panelId).classList.add('active');

    // Load content based on mode
    if (mode === 'library') {
        loadSavedPassages();
    } else if (mode === 'study') {
        // Load study content if needed
        loadStudyPassages();
    }
}

function switchCreateTab(tab) {
    // Update tab buttons
    document.querySelectorAll('.create-tab').forEach(btn => {
        btn.classList.remove('active');
    });
    document.querySelector(`[data-tab="${tab}"]`).classList.add('active');

    // Update tab content
    document.querySelectorAll('.create-content').forEach(content => {
        content.classList.remove('active');
    });
    document.getElementById(`${tab}Tab`).classList.add('active');
}

async function loadSubscriptionStatus() {
    try {
        const status = await api.get('/subscription/status');
        const statusEl = document.getElementById('subscriptionStatus');
        
        if (status.isSubscribed) {
            statusEl.innerHTML = `
                <div class="status-badge success">
                    <span class="badge-icon">✓</span>
                    <div class="badge-content">
                        <div class="badge-title">프리미엄 활성화</div>
                        <div class="badge-subtitle">만료일: ${new Date(status.expiryDate).toLocaleDateString()}</div>
                    </div>
                </div>
            `;
        } else {
            statusEl.innerHTML = `
                <div class="status-badge info">
                    <span class="badge-icon">ℹ</span>
                    <div class="badge-content">
                        <div class="badge-title">무료 버전</div>
                        <div class="badge-subtitle">남은 문제: ${status.remainingQuestions}개</div>
                    </div>
                </div>
            `;
        }
    } catch (error) {
        console.error('Failed to load subscription status:', error);
    }
}

async function loadSavedPassages() {
    try {
        const passages = await api.get('/passages');
        const container = document.getElementById('savedPassages');
        
        if (passages.length === 0) {
            container.innerHTML = `
                <div class="empty-state">
                    <svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                        <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"></path>
                        <path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"></path>
                    </svg>
                    <h3>저장된 지문이 없습니다</h3>
                    <p>지문 생성 모드에서 새로운 지문을 추가해보세요</p>
                </div>
            `;
            return;
        }

        container.innerHTML = passages.map(passage => `
            <div class="passage-card">
                <div class="card-header">
                    <h4>${escapeHtml(passage.title)}</h4>
                    <span class="card-date">${new Date(passage.createdAt).toLocaleDateString()}</span>
                </div>
                <div class="card-body">
                    <p>${escapeHtml(passage.text.substring(0, 150))}${passage.text.length > 150 ? '...' : ''}</p>
                </div>
                <div class="card-actions">
                    <button class="btn-secondary" onclick="startStudy(${passage.id})">학습하기</button>
                    <button class="btn-primary" id="generateBtn-${passage.id}" onclick="generateQuestionsForPassage(${passage.id})">문제 생성</button>
                </div>
            </div>
        `).join('');
        
        updateStats();
    } catch (error) {
        console.error('Failed to load passages:', error);
    }
}

async function loadStudyPassages() {
    try {
        const passages = await api.get('/passages');
        const container = document.getElementById('studyContent');
        
        if (passages.length === 0) {
            container.innerHTML = `
                <div class="empty-state">
                    <svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                        <path d="M2 3h6a4 4 0 0 1 4 4v14a3 3 0 0 0-3-3H2z"></path>
                        <path d="M22 3h-6a4 4 0 0 0-4 4v14a3 3 0 0 1 3-3h7z"></path>
                    </svg>
                    <h3>학습할 지문을 선택하세요</h3>
                    <p>지문 라이브러리에서 지문을 선택하여 학습을 시작하세요</p>
                </div>
            `;
            return;
        }

        container.innerHTML = `
            <div class="study-passages-grid">
                ${passages.map(passage => `
                    <div class="study-passage-card" onclick="startStudy(${passage.id})">
                        <div class="card-icon">
                            <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                <path d="M2 3h6a4 4 0 0 1 4 4v14a3 3 0 0 0-3-3H2z"></path>
                                <path d="M22 3h-6a4 4 0 0 0-4 4v14a3 3 0 0 1 3-3h7z"></path>
                            </svg>
                        </div>
                        <h4>${escapeHtml(passage.title)}</h4>
                        <p>${escapeHtml(passage.text.substring(0, 100))}${passage.text.length > 100 ? '...' : ''}</p>
                    </div>
                `).join('')}
            </div>
        `;
    } catch (error) {
        console.error('Failed to load study passages:', error);
    }
}

function startStudy(passageId) {
    // Switch to study mode and load questions
    switchMode('study');
    // This will be handled by study-mode.js
    if (typeof loadQuestions === 'function') {
        loadQuestions(passageId);
    }
}

// 문제 생성 중 상태 관리
let isGeneratingQuestions = false;

async function generateQuestionsForPassage(passageId) {
    // 이미 생성 중이면 중복 요청 방지
    if (isGeneratingQuestions) {
        alert('문제 생성이 이미 진행 중입니다. 잠시만 기다려주세요.');
        return;
    }
    
    // 버튼 비활성화
    const generateBtn = document.getElementById(`generateBtn-${passageId}`);
    if (generateBtn) {
        generateBtn.disabled = true;
        generateBtn.textContent = '생성 중...';
        generateBtn.style.opacity = '0.6';
        generateBtn.style.cursor = 'not-allowed';
    }
    
    isGeneratingQuestions = true;
    showLoading('문제 생성을 시작했습니다. 백그라운드에서 생성 중입니다...');
    
    try {
        const passages = await api.get('/passages');
        const passage = passages.find(p => p.id === passageId);
        
        if (!passage) {
            throw new Error('지문을 찾을 수 없습니다.');
        }

        const response = await api.post('/questions/generate', {
            passageId: passageId,
            text: passage.text,
            title: passage.title
        });

        hideLoading();
        
        // 즉시 응답을 받았으므로 백그라운드에서 생성 중임을 알림
        if (response.status === 'processing') {
            alert('문제 생성이 시작되었습니다. 잠시 후 자동으로 업데이트됩니다.');
            
            // 주기적으로 문제 생성 완료 여부 확인
            checkQuestionGenerationStatus(passageId, generateBtn);
        } else {
            alert('문제 생성이 완료되었습니다!');
            loadSavedPassages();
            updateStats();
            isGeneratingQuestions = false;
            if (generateBtn) {
                generateBtn.disabled = false;
                generateBtn.textContent = '문제 생성';
                generateBtn.style.opacity = '1';
                generateBtn.style.cursor = 'pointer';
            }
        }
    } catch (error) {
        hideLoading();
        alert('오류: ' + error.message);
        isGeneratingQuestions = false;
        // 버튼 다시 활성화
        if (generateBtn) {
            generateBtn.disabled = false;
            generateBtn.textContent = '문제 생성';
            generateBtn.style.opacity = '1';
            generateBtn.style.cursor = 'pointer';
        }
    }
}

// 문제 생성 상태 확인 함수
async function checkQuestionGenerationStatus(passageId, generateBtn, attemptCount = 0) {
    const maxAttempts = 60; // 최대 5분 (5초 * 60)
    const checkInterval = 5000; // 5초마다 확인
    
    if (attemptCount >= maxAttempts) {
        isGeneratingQuestions = false;
        if (generateBtn) {
            generateBtn.disabled = false;
            generateBtn.textContent = '문제 생성';
            generateBtn.style.opacity = '1';
            generateBtn.style.cursor = 'pointer';
        }
        alert('문제 생성이 시간 초과되었습니다. 새로고침 후 확인해주세요.');
        return;
    }
    
    setTimeout(async () => {
        try {
            const questions = await api.get(`/questions/passage/${passageId}`);
            
            if (questions && questions.length > 0) {
                // 문제가 생성되었음
                isGeneratingQuestions = false;
                if (generateBtn) {
                    generateBtn.disabled = false;
                    generateBtn.textContent = '문제 생성';
                    generateBtn.style.opacity = '1';
                    generateBtn.style.cursor = 'pointer';
                }
                loadSavedPassages();
                updateStats();
                alert('문제 생성이 완료되었습니다!');
            } else {
                // 아직 생성 중, 계속 확인
                checkQuestionGenerationStatus(passageId, generateBtn, attemptCount + 1);
            }
        } catch (error) {
            // 에러가 발생해도 계속 확인 시도
            console.log('Checking question status...', error);
            checkQuestionGenerationStatus(passageId, generateBtn, attemptCount + 1);
        }
    }, checkInterval);
}

async function updateStats() {
    try {
        const passages = await api.get('/passages');
        document.getElementById('totalPassages').textContent = passages.length;
        
        // TODO: Get total questions count from API
        document.getElementById('totalQuestions').textContent = '-';
    } catch (error) {
        console.error('Failed to update stats:', error);
    }
}

function showLoading(text = '처리 중...') {
    const overlay = document.getElementById('loadingOverlay');
    const textEl = overlay.querySelector('.loading-text');
    if (textEl) {
        textEl.textContent = text;
    }
    overlay.style.display = 'flex';
}

function hideLoading() {
    document.getElementById('loadingOverlay').style.display = 'none';
}

function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}
