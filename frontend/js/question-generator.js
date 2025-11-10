// PDF upload handler
if (document.getElementById('pdfUpload')) {
    document.getElementById('pdfUpload').addEventListener('change', async (e) => {
        const file = e.target.files[0];
        if (!file) return;

        showLoading('PDF 파일을 분석하고 있습니다...');
        const container = document.getElementById('pdfPassages');

        try {
            const formData = new FormData();
            formData.append('file', file);

            const response = await api.postFormData('/pdf/upload', formData);
            
            if (response.status === 'processing' && response.jobId) {
                hideLoading();
                container.innerHTML = '<div class="message info">PDF 파일 분석이 시작되었습니다. 잠시만 기다려주세요...</div>';
                
                // 주기적으로 PDF 처리 상태 확인
                checkPdfProcessingStatus(response.jobId, container);
            } else if (response.passages && response.passages.length > 0) {
                hideLoading();
                displayPdfPassages(response.passages, container);
            } else {
                hideLoading();
                container.innerHTML = '<div class="message error">PDF에서 지문을 추출할 수 없습니다.</div>';
            }
        } catch (error) {
            hideLoading();
            container.innerHTML = `<div class="message error">오류: ${error.message}</div>`;
        }
    });
}

// PDF 처리 상태 확인 함수
async function checkPdfProcessingStatus(jobId, container, attemptCount = 0) {
    const maxAttempts = 60; // 최대 5분 (5초 * 60)
    const checkInterval = 5000; // 5초마다 확인
    
    if (attemptCount >= maxAttempts) {
        container.innerHTML = '<div class="message error">PDF 분석이 시간 초과되었습니다. 다시 시도해주세요.</div>';
        return;
    }
    
    setTimeout(async () => {
        try {
            const response = await api.get(`/pdf/status/${jobId}`);
            
            if (response.status === 'completed' && response.passages && response.passages.length > 0) {
                displayPdfPassages(response.passages, container);
            } else if (response.status === 'failed') {
                container.innerHTML = `<div class="message error">PDF 분석 실패: ${response.error || '알 수 없는 오류'}</div>`;
            } else {
                // 아직 처리 중, 계속 확인
                checkPdfProcessingStatus(jobId, container, attemptCount + 1);
            }
        } catch (error) {
            // 에러가 발생해도 계속 확인 시도
            console.log('Checking PDF processing status...', error);
            checkPdfProcessingStatus(jobId, container, attemptCount + 1);
        }
    }, checkInterval);
}

// PDF 지문 표시 함수
function displayPdfPassages(passages, container) {
    if (passages && passages.length > 0) {
        container.innerHTML = passages.map((passage, idx) => `
            <div class="input-card" style="margin-top: var(--spacing-lg);">
                <div class="input-group">
                    <label>지문 ${idx + 1}</label>
                    <textarea id="pdfPassage${idx}" rows="10" placeholder="지문 내용">${escapeHtml(passage)}</textarea>
                </div>
                <div class="input-group">
                    <label>지문 제목</label>
                    <input type="text" id="pdfTitle${idx}" placeholder="지문 제목을 입력하세요">
                </div>
                <div class="action-buttons">
                    <button class="btn-secondary" onclick="savePdfPassage(${idx}, \`${passage.replace(/`/g, '\\`').replace(/\\/g, '\\\\')}\`)">저장</button>
                    <button class="btn-primary" id="pdfGenerateBtn-${idx}" onclick="generateFromPdf(${idx}, \`${passage.replace(/`/g, '\\`').replace(/\\/g, '\\\\')}\`)">문제 생성</button>
                </div>
            </div>
        `).join('');
    } else {
        container.innerHTML = '<div class="message error">PDF에서 지문을 추출할 수 없습니다.</div>';
    }
}

async function savePdfPassage(idx, passageText) {
    const title = document.getElementById(`pdfTitle${idx}`).value;
    if (!title) {
        alert('제목을 입력해주세요.');
        return;
    }

    showLoading('지문을 저장하고 있습니다...');
    try {
        await api.post('/passages', { title, text: passageText });
        hideLoading();
        alert('저장되었습니다!');
        if (typeof loadSavedPassages === 'function') {
            loadSavedPassages();
        }
        if (typeof updateStats === 'function') {
            updateStats();
        }
    } catch (error) {
        hideLoading();
        alert('오류: ' + error.message);
    }
}

// 문제 생성 중 상태 관리 (question-generator.js)
if (typeof window.isGeneratingQuestions === 'undefined') {
    window.isGeneratingQuestions = false;
}

async function generateFromPdf(idx, passageText) {
    // 이미 생성 중이면 중복 요청 방지
    if (window.isGeneratingQuestions) {
        alert('문제 생성이 이미 진행 중입니다. 잠시만 기다려주세요.');
        return;
    }
    
    const title = document.getElementById(`pdfTitle${idx}`).value;
    if (!title) {
        alert('제목을 입력해주세요.');
        return;
    }

    // 버튼 비활성화
    const generateBtn = document.getElementById(`pdfGenerateBtn-${idx}`);
    if (generateBtn) {
        generateBtn.disabled = true;
        generateBtn.textContent = '생성 중...';
        generateBtn.style.opacity = '0.6';
        generateBtn.style.cursor = 'not-allowed';
    }

    window.isGeneratingQuestions = true;
    showLoading('문제 생성을 시작했습니다. 백그라운드에서 생성 중입니다...');
    
    try {
        const response = await api.post('/questions/generate', { text: passageText, title });
        hideLoading();
        
        // 즉시 응답을 받았으므로 백그라운드에서 생성 중임을 알림
        if (response.status === 'processing' && response.passageId) {
            alert('문제 생성이 시작되었습니다. 잠시 후 자동으로 업데이트됩니다.');
            
            // 주기적으로 문제 생성 완료 여부 확인
            checkQuestionGenerationStatusFromPdf(response.passageId, generateBtn);
        } else {
            alert('문제 생성이 완료되었습니다!');
            if (typeof loadSavedPassages === 'function') {
                loadSavedPassages();
            }
            if (typeof updateStats === 'function') {
                updateStats();
            }
            window.isGeneratingQuestions = false;
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
        window.isGeneratingQuestions = false;
        // 버튼 다시 활성화
        if (generateBtn) {
            generateBtn.disabled = false;
            generateBtn.textContent = '문제 생성';
            generateBtn.style.opacity = '1';
            generateBtn.style.cursor = 'pointer';
        }
    }
}

// PDF에서 문제 생성 상태 확인 함수
async function checkQuestionGenerationStatusFromPdf(passageId, generateBtn, attemptCount = 0) {
    const maxAttempts = 60; // 최대 5분 (5초 * 60)
    const checkInterval = 5000; // 5초마다 확인
    
    if (attemptCount >= maxAttempts) {
        window.isGeneratingQuestions = false;
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
                window.isGeneratingQuestions = false;
                if (generateBtn) {
                    generateBtn.disabled = false;
                    generateBtn.textContent = '문제 생성';
                    generateBtn.style.opacity = '1';
                    generateBtn.style.cursor = 'pointer';
                }
                if (typeof loadSavedPassages === 'function') {
                    loadSavedPassages();
                }
                if (typeof updateStats === 'function') {
                    updateStats();
                }
                alert('문제 생성이 완료되었습니다!');
            } else {
                // 아직 생성 중, 계속 확인
                checkQuestionGenerationStatusFromPdf(passageId, generateBtn, attemptCount + 1);
            }
        } catch (error) {
            // 에러가 발생해도 계속 확인 시도
            console.log('Checking question status...', error);
            checkQuestionGenerationStatusFromPdf(passageId, generateBtn, attemptCount + 1);
        }
    }, checkInterval);
}

// Manual passage handlers
if (document.getElementById('savePassageBtn')) {
    document.getElementById('savePassageBtn').addEventListener('click', async () => {
        const text = document.getElementById('manualPassage').value;
        const title = document.getElementById('manualTitle').value;

        if (!title) {
            alert('제목을 입력해주세요.');
            return;
        }

        if (!text) {
            alert('지문 내용을 입력해주세요.');
            return;
        }

        showLoading('지문을 저장하고 있습니다...');
        try {
            await api.post('/passages', { title, text });
            hideLoading();
            alert('저장되었습니다!');
            document.getElementById('manualPassage').value = '';
            document.getElementById('manualTitle').value = '';
            if (typeof loadSavedPassages === 'function') {
                loadSavedPassages();
            }
            if (typeof updateStats === 'function') {
                updateStats();
            }
        } catch (error) {
            hideLoading();
            alert('오류: ' + error.message);
        }
    });
}

if (document.getElementById('generateQuestionsBtn')) {
    document.getElementById('generateQuestionsBtn').addEventListener('click', async () => {
        // 이미 생성 중이면 중복 요청 방지
        if (window.isGeneratingQuestions) {
            alert('문제 생성이 이미 진행 중입니다. 잠시만 기다려주세요.');
            return;
        }
        
        const text = document.getElementById('manualPassage').value;
        const title = document.getElementById('manualTitle').value;

        if (!title) {
            alert('제목을 입력해주세요.');
            return;
        }

        if (!text) {
            alert('지문 내용을 입력해주세요.');
            return;
        }

        // 버튼 비활성화
        const generateBtn = document.getElementById('generateQuestionsBtn');
        if (generateBtn) {
            generateBtn.disabled = true;
            generateBtn.innerHTML = '<i class="fa-solid fa-spinner fa-spin"></i> <span>생성 중...</span>';
            generateBtn.style.opacity = '0.6';
            generateBtn.style.cursor = 'not-allowed';
        }

        window.isGeneratingQuestions = true;
        showLoading('문제 생성을 시작했습니다. 백그라운드에서 생성 중입니다...');
        
        try {
            const response = await api.post('/questions/generate', { text, title });
            hideLoading();
            
            // 즉시 응답을 받았으므로 백그라운드에서 생성 중임을 알림
            if (response.status === 'processing' && response.passageId) {
                alert('문제 생성이 시작되었습니다. 잠시 후 자동으로 업데이트됩니다.');
                
                // 주기적으로 문제 생성 완료 여부 확인
                checkQuestionGenerationStatusManual(response.passageId, generateBtn);
            } else {
                alert('문제 생성이 완료되었습니다!');
                if (typeof loadSavedPassages === 'function') {
                    loadSavedPassages();
                }
                if (typeof updateStats === 'function') {
                    updateStats();
                }
                window.isGeneratingQuestions = false;
                if (generateBtn) {
                    generateBtn.disabled = false;
                    generateBtn.innerHTML = '<i class="fa-solid fa-wand-sparkles"></i> <span>문제 생성</span>';
                    generateBtn.style.opacity = '1';
                    generateBtn.style.cursor = 'pointer';
                }
            }
        } catch (error) {
            hideLoading();
            alert('오류: ' + error.message);
            window.isGeneratingQuestions = false;
            // 버튼 다시 활성화
            if (generateBtn) {
                generateBtn.disabled = false;
                generateBtn.innerHTML = '<i class="fa-solid fa-wand-sparkles"></i> <span>문제 생성</span>';
                generateBtn.style.opacity = '1';
                generateBtn.style.cursor = 'pointer';
            }
        }
    });
}

// 수동 입력에서 문제 생성 상태 확인 함수
async function checkQuestionGenerationStatusManual(passageId, generateBtn, attemptCount = 0) {
    const maxAttempts = 60; // 최대 5분 (5초 * 60)
    const checkInterval = 5000; // 5초마다 확인
    
    if (attemptCount >= maxAttempts) {
        window.isGeneratingQuestions = false;
        if (generateBtn) {
            generateBtn.disabled = false;
            generateBtn.innerHTML = '<i class="fa-solid fa-wand-sparkles"></i> <span>문제 생성</span>';
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
                window.isGeneratingQuestions = false;
                if (generateBtn) {
                    generateBtn.disabled = false;
                    generateBtn.innerHTML = '<i class="fa-solid fa-wand-sparkles"></i> <span>문제 생성</span>';
                    generateBtn.style.opacity = '1';
                    generateBtn.style.cursor = 'pointer';
                }
                if (typeof loadSavedPassages === 'function') {
                    loadSavedPassages();
                }
                if (typeof updateStats === 'function') {
                    updateStats();
                }
                alert('문제 생성이 완료되었습니다!');
            } else {
                // 아직 생성 중, 계속 확인
                checkQuestionGenerationStatusManual(passageId, generateBtn, attemptCount + 1);
            }
        } catch (error) {
            // 에러가 발생해도 계속 확인 시도
            console.log('Checking question status...', error);
            checkQuestionGenerationStatusManual(passageId, generateBtn, attemptCount + 1);
        }
    }, checkInterval);
}

// Subscription Modal Functions
function showSubscriptionModal() {
    const modal = document.getElementById('subscriptionModal');
    if (modal) {
        modal.style.display = 'flex';
        document.body.style.overflow = 'hidden';
    }
}

function hideSubscriptionModal() {
    const modal = document.getElementById('subscriptionModal');
    if (modal) {
        modal.style.display = 'none';
        document.body.style.overflow = '';
    }
}

// Subscription request handler
if (document.getElementById('subscribeBtn')) {
    document.getElementById('subscribeBtn').addEventListener('click', () => {
        showSubscriptionModal();
    });
}

// Modal close handlers
if (document.getElementById('closeModalBtn')) {
    document.getElementById('closeModalBtn').addEventListener('click', () => {
        hideSubscriptionModal();
    });
}

if (document.getElementById('cancelSubscriptionBtn')) {
    document.getElementById('cancelSubscriptionBtn').addEventListener('click', () => {
        hideSubscriptionModal();
    });
}

// Close modal when clicking outside
const subscriptionModal = document.getElementById('subscriptionModal');
if (subscriptionModal) {
    subscriptionModal.addEventListener('click', (e) => {
        if (e.target === subscriptionModal) {
            hideSubscriptionModal();
        }
    });
}

// Confirm subscription
if (document.getElementById('confirmSubscriptionBtn')) {
    document.getElementById('confirmSubscriptionBtn').addEventListener('click', async () => {
        if (!confirm('입금을 완료하셨나요? 구독 신청을 진행하시겠습니까?')) {
            return;
        }

        try {
            showLoading('구독 신청을 처리하고 있습니다...');
            await api.post('/subscription/request', {});
            hideLoading();
            hideSubscriptionModal();
            alert('구독 신청이 완료되었습니다. 입금 확인 후 승인됩니다.');
            if (typeof loadSubscriptionStatus === 'function') {
                loadSubscriptionStatus();
            }
        } catch (error) {
            hideLoading();
            alert('오류: ' + error.message);
        }
    });
}

// Close modal with Escape key
document.addEventListener('keydown', (e) => {
    if (e.key === 'Escape') {
        hideSubscriptionModal();
    }
});

function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}
