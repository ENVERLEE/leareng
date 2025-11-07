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

        // Create passage selector
        const selector = document.createElement('select');
        selector.id = 'passageSelector';
        selector.innerHTML = '<option value="">학습할 지문을 선택하세요</option>' +
            passages.map(p => `<option value="${p.id}">${p.title}</option>`).join('');
        
        selector.addEventListener('change', async (e) => {
            const passageId = e.target.value;
            if (passageId) {
                await loadQuestions(passageId);
            }
        });

        container.innerHTML = '';
        container.appendChild(selector);
        const questionContainer = document.createElement('div');
        questionContainer.id = 'questionContainer';
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

    try {
        questions = await api.get(`/questions/passage/${passageId}`);
        
        if (questions.length === 0) {
            document.getElementById('questionContainer').innerHTML = 
                '<p class="message info">이 지문에 대한 문제가 아직 생성되지 않았습니다.</p>';
            return;
        }

        displayQuestion();
    } catch (error) {
        document.getElementById('questionContainer').innerHTML = 
            `<div class="message error">오류: ${error.message}</div>`;
    }
}

function displayQuestion() {
    if (currentQuestionIndex >= questions.length) {
        document.getElementById('questionContainer').innerHTML = 
            '<p class="message success">모든 문제를 완료했습니다!</p>';
        return;
    }

    const question = questions[currentQuestionIndex];
    const container = document.getElementById('questionContainer');
    
    let html = `
        <div class="question-card">
            <p>문제 ${currentQuestionIndex + 1}/${questions.length}</p>
    `;

    if (question.originalText) {
        html += `
            <h3>지문</h3>
            <p>${question.originalText}</p>
            <hr>
        `;
    }

    html += `
        <h3>Q. ${question.question}</h3>
        <div class="choices">
    `;

    question.choices.forEach((choice, idx) => {
        const choiceNum = idx + 1;
        let className = 'choice-btn';
        if (showAnswer && choiceNum === question.correctAnswer) {
            className += ' correct';
        } else if (showAnswer && choiceNum !== question.correctAnswer) {
            className += ' incorrect';
        }
        
        html += `
            <button class="${className}" onclick="selectChoice(${choiceNum}, ${question.correctAnswer})">
                ${choiceNum}. ${choice}
            </button>
        `;
    });

    html += '</div>';

    if (showAnswer) {
        html += `
            <div class="explanation">
                <h3>정답 설명</h3>
                <p>${question.explanation || ''}</p>
            </div>
        `;
    }

    if (showTranslation && question.koreanTranslation) {
        html += `
            <div class="translation">
                <h3>한국어 번역</h3>
                <p>${question.koreanTranslation}</p>
            </div>
        `;
    }

    html += `
        <div class="navigation">
            <button class="btn-secondary" onclick="previousQuestion()" ${currentQuestionIndex === 0 ? 'disabled' : ''}>
                ← 이전
            </button>
            <button class="btn-primary" onclick="showAnswerFunc()" ${showAnswer ? 'disabled' : ''}>
                정답 보기
            </button>
            <button class="btn-secondary" onclick="nextQuestion()" ${currentQuestionIndex >= questions.length - 1 ? 'disabled' : ''}>
                다음 →
            </button>
        </div>
    </div>
    `;

    container.innerHTML = html;
}

function selectChoice(selected, correct) {
    if (showAnswer) return;

    showAnswer = true;
    if (selected !== correct) {
        showTranslation = true;
    }
    displayQuestion();
}

function showAnswerFunc() {
    showAnswer = true;
    displayQuestion();
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

// Auto-load study mode when tab is clicked
document.addEventListener('DOMContentLoaded', () => {
    const studyTab = document.querySelector('[data-mode="study"]');
    if (studyTab) {
        studyTab.addEventListener('click', () => {
            setTimeout(loadStudyMode, 100);
        });
    }
});

