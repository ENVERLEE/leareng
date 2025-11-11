let currentPassageId = null;
let questions = [];
let currentQuestionIndex = 0;
let showAnswer = false;
let showTranslation = false;

// Load study mode content
if (document.getElementById('studyMode')) {
    document.getElementById('studyMode').addEventListener('display', () => {
        loadStudyMode();
    });
}

async function loadStudyMode() {
    const container = document.getElementById('studyContent');
    
    try {
        const passages = await api.get('/passages');
        
        if (passages.length === 0) {
            container.innerHTML = '<p class="message info">저장된 지문이 없습니다. 편집 모드에서 지문을 추가해주세요.</p>';
            return;
        }

        // Create passage selector with better styling
        const selectorWrapper = document.createElement('div');
        selectorWrapper.className = 'passage-selector-wrapper';
        const selector = document.createElement('select');
        selector.id = 'passageSelector';
        selector.className = 'passage-selector';
        selector.innerHTML = '<option value="">학습할 지문을 선택하세요</option>' +
            passages.map(p => `<option value="${p.id}">${p.title}</option>`).join('');
        
        selector.addEventListener('change', async (e) => {
            const passageId = e.target.value;
            if (passageId) {
                await loadQuestions(passageId);
            }
        });

        selectorWrapper.appendChild(selector);
        container.innerHTML = '';
        container.appendChild(selectorWrapper);
        const questionContainer = document.createElement('div');
        questionContainer.id = 'questionContainer';
        questionContainer.className = 'question-container';
        container.appendChild(questionContainer);
    } catch (error) {
        container.innerHTML = `<div class="message error">오류: ${error.message}</div>`;
    }
}

async function loadQuestions(passageId) {
    currentPassageId = passageId;
    currentQuestionIndex = 0;
    showAnswer = false;
    showTranslation = false;

    // Ensure questionContainer exists
    const container = document.getElementById('studyContent');
    let questionContainer = document.getElementById('questionContainer');
    
    if (!questionContainer) {
        questionContainer = document.createElement('div');
        questionContainer.id = 'questionContainer';
        questionContainer.className = 'question-container';
        container.appendChild(questionContainer);
    }

    try {
        questions = await api.get(`/questions/passage/${passageId}`);
        
        if (questions.length === 0) {
            questionContainer.innerHTML = 
                '<p class="message info">이 지문에 대한 문제가 아직 생성되지 않았습니다.</p>';
            return;
        }

        displayQuestion();
    } catch (error) {
        console.error('Failed to load questions:', error);
        questionContainer.innerHTML = 
            `<div class="message error">오류: ${error.message}</div>`;
    }
}

function displayQuestion() {
    const questionContainer = document.getElementById('questionContainer');
    
    if (!questionContainer) {
        console.error('questionContainer not found');
        return;
    }
    
    // 사이드바 번역 업데이트
    updateSidebarTranslation();
    
    if (currentQuestionIndex >= questions.length) {
        questionContainer.innerHTML = 
            '<div class="completion-message"><div class="completion-icon">✓</div><h2>모든 문제를 완료했습니다!</h2><p>훌륭합니다! 모든 문제를 풀었습니다.</p></div>';
        return;
    }

    const question = questions[currentQuestionIndex];
    const container = questionContainer;
    
    let html = `
        <div class="question-wrapper ${showTranslation ? 'with-translation' : ''}">
            <div class="question-header">
                <div class="question-progress">
                    <span class="progress-badge">문제 ${currentQuestionIndex + 1} / ${questions.length}</span>
                </div>
            </div>
            
            <div class="question-layout">
                ${question.originalText ? `
                <div class="passage-column">
                    <div class="passage-section">
                        <div class="section-label">
                            <i class="fa-solid fa-book-open"></i>
                            <span>지문</span>
                        </div>
                        <div class="passage-text">${escapeHtml(question.originalText).replace(/\n/g, '<br>')}</div>
                    </div>
                </div>
                ` : ''}
                
                <div class="question-main">
                    <div class="question-card">
                        <div class="question-section">
                            <div class="section-label">
                                <i class="fa-solid fa-circle-question"></i>
                                <span>문제</span>
                            </div>
                            <div class="question-text">${escapeHtml(question.question)}</div>
                        </div>
            
            <div class="choices-section">
                <div class="section-label">
                    <i class="fa-solid fa-list-ol"></i>
                    <span>선택지</span>
                </div>
                <div class="choices">
    `;

    // 정답 번호를 숫자로 변환 (null/undefined 처리)
    const correctAnswerNum = question.correctAnswer != null ? Number(question.correctAnswer) : null;
    
    question.choices.forEach((choice, idx) => {
        const choiceNum = idx + 1;
        let className = 'choice-item';
        let icon = '';
        let selectedClass = '';
        
        // 정답 표시 로직 (타입 안전성 보장)
        if (showAnswer && correctAnswerNum != null) {
            if (choiceNum === correctAnswerNum) {
                className += ' correct';
                icon = '<i class="fa-solid fa-check-circle"></i>';
                selectedClass = 'selected-correct';
            } else {
                className += ' incorrect';
                icon = '<i class="fa-solid fa-times-circle"></i>';
            }
        }
        
        html += `
            <label class="choice-wrapper">
                <input type="radio" name="question-choice" value="${choiceNum}" class="choice-radio" 
                    onclick="selectChoice(${choiceNum}, ${correctAnswerNum != null ? correctAnswerNum : 'null'}, ${question.id || 'null'})" 
                    ${showAnswer ? 'disabled' : ''}>
                <div class="choice-item ${className} ${selectedClass}">
                    <div class="choice-indicator">
                        <span class="choice-radio-custom"></span>
                    </div>
                    <span class="choice-text">${escapeHtml(choice)}</span>
                    ${icon ? `<span class="choice-icon">${icon}</span>` : ''}
                </div>
            </label>
        `;
    });

    html += `
                    </div>
                </div>
            </div>
    `;

    if (showAnswer) {
        html += `
                    <div class="answer-section">
                        <div class="section-label">
                            <i class="fa-solid fa-lightbulb"></i>
                            <span>정답 설명</span>
                        </div>
                        <div class="explanation-text">${escapeHtml(question.explanation || '설명이 없습니다.')}</div>
                    </div>
        `;
    }

    html += `
                    </div>
                </div>
                
                <div class="question-navigation">
                    <button class="nav-btn nav-btn-prev" onclick="previousQuestion()" ${currentQuestionIndex === 0 ? 'disabled' : ''}>
                        <i class="fa-solid fa-chevron-left"></i>
                        <span>이전</span>
                    </button>
                    <button class="nav-btn nav-btn-answer" onclick="showAnswerFunc()" ${showAnswer ? 'disabled' : ''}>
                        <i class="fa-solid fa-eye"></i>
                        <span>정답 보기</span>
                    </button>
                    <button class="nav-btn nav-btn-translation" onclick="toggleTranslation()" ${question.koreanTranslation ? '' : 'disabled'} title="${question.koreanTranslation ? '' : '번역이 없습니다'}">
                        <i class="fa-solid fa-language"></i>
                        <span>${showTranslation ? '번역 숨기기' : '번역 보기'}</span>
                    </button>
                    <button class="nav-btn nav-btn-next" onclick="nextQuestion()" ${currentQuestionIndex >= questions.length - 1 ? 'disabled' : ''}>
                        <span>다음</span>
                        <i class="fa-solid fa-chevron-right"></i>
                    </button>
                </div>
            </div>
        </div>
    `;

    if (container) {
        container.innerHTML = html;
    } else {
        console.error('Container not found for displaying question');
    }
}

function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

async function selectChoice(selected, correct, questionId) {
    if (showAnswer) return;

    const question = questions[currentQuestionIndex];
    // questionId 파라미터가 있으면 사용, 없으면 question 객체에서 가져오기
    const id = questionId || (question ? question.id : null);
    
    // 정답 번호를 숫자로 변환 (null/undefined 처리)
    const correctAnswerNum = correct != null ? Number(correct) : (question && question.correctAnswer != null ? Number(question.correctAnswer) : null);
    
    if (!id) {
        // question id가 없으면 기존 방식대로 처리
        showAnswer = true;
        displayQuestion();
        return;
    }

    try {
        // 답안 제출 API 호출
        const response = await api.post('/questions/submit-answer', {
            questionId: id,
            selectedAnswer: selected
        });

        // API 응답에서 받은 정답 정보로 question 객체 업데이트 (있는 경우)
        if (response.correctAnswer != null && question) {
            question.correctAnswer = Number(response.correctAnswer);
        }
        if (response.explanation && question) {
            question.explanation = response.explanation;
        }

        showAnswer = true;
        
        // 업그레이드 가능 여부 체크
        if (response.upgradeAvailable) {
            showUpgradeModal(response.upgradeableTier, response.tierName, response.totalQuestionsSolved);
        }
        
        displayQuestion();
    } catch (error) {
        console.error('Failed to submit answer:', error);
        // 에러가 발생해도 정답 표시는 진행
        showAnswer = true;
        displayQuestion();
    }
}

function showAnswerFunc() {
    showAnswer = true;
    displayQuestion();
}

function toggleTranslation() {
    showTranslation = !showTranslation;
    updateSidebarTranslation();
    displayQuestion();
}

function updateSidebarTranslation() {
    const sidebar = document.querySelector('.app-sidebar');
    if (!sidebar) return;
    
    // 기존 번역 카드 제거
    const existingTranslation = sidebar.querySelector('.translation-sidebar-card');
    if (existingTranslation) {
        existingTranslation.remove();
    }
    
    // 번역 보기가 활성화되고 번역이 있으면 사이드바에 추가
    if (showTranslation && questions.length > 0 && currentQuestionIndex < questions.length) {
        const question = questions[currentQuestionIndex];
        if (question.koreanTranslation) {
            const translationCard = document.createElement('div');
            translationCard.className = 'sidebar-card translation-sidebar-card glass-card';
            translationCard.innerHTML = `
                <div class="card-header">
                    <div class="section-label" style="margin-bottom: 0;">
                        <i class="fa-solid fa-language"></i>
                        <span>한국어 번역</span>
                    </div>
                    <button class="close-translation-btn" onclick="toggleTranslation()" title="번역 닫기" style="background: rgba(255, 255, 255, 0.8); border: 1px solid rgba(148, 163, 184, 0.28); width: 28px; height: 28px; border-radius: 50%; display: flex; align-items: center; justify-content: center; cursor: pointer; transition: all 0.2s;">
                        <i class="fa-solid fa-times" style="font-size: 0.85rem;"></i>
                    </button>
                </div>
                <div class="translation-sidebar-content" style="padding-top: 1rem; max-height: 400px; overflow-y: auto;">
                    <div class="translation-text" style="line-height: 1.85; color: var(--text-primary); font-size: 0.95rem; white-space: pre-wrap; word-break: keep-all; word-wrap: normal; overflow-wrap: break-word;">${escapeHtml(question.koreanTranslation)}</div>
                </div>
            `;
            
            // 학습 통계 카드 다음에 삽입
            const statsCard = sidebar.querySelector('.stats-card');
            if (statsCard && statsCard.nextSibling) {
                sidebar.insertBefore(translationCard, statsCard.nextSibling);
            } else {
                sidebar.appendChild(translationCard);
            }
        }
    }
}

function previousQuestion() {
    if (currentQuestionIndex > 0) {
        currentQuestionIndex--;
        showAnswer = false;
        showTranslation = false;
        displayQuestion();
    }
}

function nextQuestion() {
    if (currentQuestionIndex < questions.length - 1) {
        currentQuestionIndex++;
        showAnswer = false;
        showTranslation = false;
        displayQuestion();
    }
}

// 업그레이드 모달 표시
function showUpgradeModal(tier, tierName, totalSolved) {
    // 기존 모달이 있으면 제거
    const existingModal = document.getElementById('upgradeModal');
    if (existingModal) {
        existingModal.remove();
    }

    const modal = document.createElement('div');
    modal.id = 'upgradeModal';
    modal.className = 'upgrade-modal-overlay';
    modal.innerHTML = `
        <div class="upgrade-modal">
            <div class="upgrade-modal-header">
                <h2>🎉 축하합니다!</h2>
                <button class="upgrade-modal-close" onclick="closeUpgradeModal()">&times;</button>
            </div>
            <div class="upgrade-modal-body">
                <p class="upgrade-message">
                    문제를 <strong>${totalSolved}개</strong> 해결하셨습니다!
                </p>
                <p class="upgrade-info">
                    <strong>${tierName}</strong> 티어 <strong>3일 무료 체험</strong>을 받을 수 있습니다!
                </p>
                <div class="upgrade-benefits">
                    ${tier === 'BASIC' ? '<p>• 일일 문제 풀이 한도: 500개</p>' : ''}
                    ${tier === 'PREMIUM' ? '<p>• 일일 문제 풀이 한도: 1000개</p>' : ''}
                    ${tier === 'ACADEMY' ? '<p>• 무제한 문제 풀이</p><p>• 학원용 특별 혜택</p>' : ''}
                    <p style="margin-top: 1rem; padding-top: 1rem; border-top: 1px solid rgba(74, 101, 129, 0.2);">
                        <strong>⚠️ 중요:</strong> 체험 기간은 3일입니다. 체험 종료 후 계속 사용하려면 구독 신청이 필요합니다.
                    </p>
                </div>
            </div>
            <div class="upgrade-modal-footer">
                <button class="btn-secondary" onclick="closeUpgradeModal()">나중에</button>
                <button class="btn-primary" onclick="confirmUpgrade('${tier}')">3일 무료 체험 시작</button>
            </div>
        </div>
    `;
    
    document.body.appendChild(modal);
    modal.style.display = 'flex';
}

function closeUpgradeModal() {
    const modal = document.getElementById('upgradeModal');
    if (modal) {
        modal.remove();
    }
}

async function confirmUpgrade(tier) {
    try {
        const response = await api.post('/subscription/upgrade', {
            tier: tier
        });
        
        closeUpgradeModal();
        alert('3일 무료 체험이 시작되었습니다! 체험 종료 후 계속 사용하려면 구독 신청이 필요합니다.');
        
        // 구독 상태 새로고침
        if (typeof loadSubscriptionStatus === 'function') {
            loadSubscriptionStatus();
        }
    } catch (error) {
        console.error('Failed to upgrade tier:', error);
        alert('업그레이드 중 오류가 발생했습니다: ' + error.message);
    }
}

// Note: Initial loading is handled by app.js's switchMode function
// This file handles question display after a passage is selected

