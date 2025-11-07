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
            hideLoading();

            if (response.passages && response.passages.length > 0) {
                container.innerHTML = response.passages.map((passage, idx) => `
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
                            <button class="btn-primary" onclick="generateFromPdf(${idx}, \`${passage.replace(/`/g, '\\`').replace(/\\/g, '\\\\')}\`)">문제 생성</button>
                        </div>
                    </div>
                `).join('');
            } else {
                container.innerHTML = '<div class="message error">PDF에서 지문을 추출할 수 없습니다.</div>';
            }
        } catch (error) {
            hideLoading();
            container.innerHTML = `<div class="message error">오류: ${error.message}</div>`;
        }
    });
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

async function generateFromPdf(idx, passageText) {
    const title = document.getElementById(`pdfTitle${idx}`).value;
    if (!title) {
        alert('제목을 입력해주세요.');
        return;
    }

    showLoading('문제를 생성하고 있습니다...');
    try {
        await api.post('/questions/generate', { text: passageText, title });
        hideLoading();
        alert('문제 생성이 완료되었습니다!');
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

        showLoading('문제를 생성하고 있습니다...');
        try {
            await api.post('/questions/generate', { text, title });
            hideLoading();
            alert('문제 생성이 완료되었습니다!');
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
