import streamlit as st
import streamlit.components.v1 as components
import os
import re
from typing import List, Dict, Optional, Union, Any
from langchain_anthropic import ChatAnthropic
import traceback
from langchain.prompts import ChatPromptTemplate
from langchain.schema import HumanMessage, SystemMessage
from langchain_core.exceptions import OutputParserException
from langchain_core.output_parsers import PydanticOutputParser
from langchain.chains import LLMChain
from langchain_core.runnables import RunnableSerializable
from langchain.output_parsers import OutputFixingParser
from pydantic import BaseModel, Field
import pymupdf
import json
import ssl
from pymongo.mongo_client import MongoClient
from pymongo.server_api import ServerApi
import pymongo
from datetime import datetime
import random   
import warnings
import tempfile
import os
from langchain.document_loaders import PyPDFLoader, PDFMinerLoader, PyMuPDFLoader
from langchain.text_splitter import RecursiveCharacterTextSplitter
import time
from dotenv import load_dotenv
import streamlit as st
import time
import hashlib
from datetime import datetime, timedelta
import smtplib
from email.mime.text import MIMEText
from email.mime.multipart import MIMEMultipart
from jose import jwt
import secrets
import string

load_dotenv()

st.set_page_config(
    page_title="내신쌀먹-영어",
    layout="wide"
)

def apply_custom_css():
    return """
        <style>
        /* 전체 앱 스타일 */
        .stApp {
            background-color: #f2f2f7;
        }

        /* 카드 스타일 */
        .css-1r6slb0 {
            background-color: white;
            border-radius: 10px;
            padding: 20px;
            box-shadow: 0 1px 3px rgba(0,0,0,0.1);
            margin: 10px 0;
        }

        /* 버튼 스타일 */
        .stButton>button {
            background-color: #007AFF;
            color: white;
            border-radius: 8px;
            border: none;
            padding: 8px 16px;
            font-weight: 500;
            width: 100%;
            transition: all 0.2s;
        }
        .stButton>button:hover {
            background-color: #0051FF;
        }

        /* 입력 필드 스타일 */
        .stTextInput>div>div>input {
            border-radius: 8px;
            border: 1px solid #E5E5EA;
            padding: 8px 12px;
        }

        /* 탭 스타일 */
        .auth-tab {
            display: flex;
            justify-content: center;
            gap: 10px;
            padding: 10px;
            background-color: white;
            border-radius: 10px;
            margin-bottom: 20px;
        }
        .auth-tab-item {
            padding: 8px 16px;
            border-radius: 8px;
            cursor: pointer;
            transition: all 0.2s;
        }
        .auth-tab-item.active {
            background-color: #007AFF;
            color: white;
        }

        /* 알림 메시지 스타일 */
        .stAlert {
            border-radius: 10px;
            border: none;
        }

        /* 사이드바 스타일 */
        .css-1d391kg {
            background-color: #f2f2f7;
        }

        /* 컨테이너 스타일 */
        .auth-container {
            background-color: white;
            padding: 20px;
            border-radius: 10px;
            box-shadow: 0 1px 3px rgba(0,0,0,0.1);
            margin-bottom: 20px;
        }
        </style>
    """

st.markdown(apply_custom_css(), unsafe_allow_html=True)

# HTML 컴포넌트로 IMA SDK 및 광고 코드 추가
ad_container = st.empty()


# HTML 헤더에 메타 태그 추가
st.markdown("""
    <head>
        <meta name="google-adsense-account" content="ca-pub-2146306428640171">
    </head>
""", unsafe_allow_html=True)
ad_container.markdown("""
    <script async src="https://imasdk.googleapis.com/js/sdkloader/ima3.js"></script>
    <div id="adContainer">
        <video id="contentElement">
            <source src="광고영상.mp4" type="video/mp4">
        </video>
    </div>
    <script>
        var adDisplayContainer = new google.ima.AdDisplayContainer(
            document.getElementById('adContainer'));
        var adsLoader = new google.ima.AdsLoader(adDisplayContainer);
        var adsRequest = new google.ima.AdsRequest();
        adsRequest.adTagUrl = '광고태그URL';
        adsLoader.requestAds(adsRequest);
    </script>
""", unsafe_allow_html=True)
# Ignore PyTorch related warnings

def initialize_spacy():
    """Initialize spaCy with minimal pipeline"""
    try:
        return en_core_web_sm.load(disable=['ner', 'parser', 'textcat'])
    except Exception as e:
        st.error(f"spaCy 모델 로드 중 오류 발생: {str(e)}")
        raise

def initialize_environment():
    """Initialize all required components and handle dependencies"""
    try:
        # Initialize database
        db = DBManager()

    except Exception as e:
        st.error(f"환경 초기화 중 오류 발생: {str(e)}")
        raise

class QuestionOutput(BaseModel):
    question: str = Field(description="Question content in Korean")
    choices: List[str] = Field(description="Four answer choices in Korean")
    correct_answer: int = Field(description="Correct answer number (1-4)")
    explanation: Optional[str] = Field(default=None, description="Detailed explanation in Korean")
    difficulty: Optional[int] = Field(default=None, description="Difficulty level (1-5)")
    key_points: Optional[List[str]] = Field(default=None, description="Key points and evaluation elements")
    wrong_answer_analysis: Optional[Dict[str, str]] = Field(default=None, description="Analysis of wrong answers")
    type: Optional[str] = None
    type_id: Optional[str] = None
    question_number: Optional[int] = None
    original_text: Optional[str] = None
    korean_translation: Optional[str] = None

    class Config:
        extra = "allow"  # 추가 필드 허용

class QuestionGenerator:
    def __init__(self):
        self.llm = ChatAnthropic(
            model="claude-3-5-haiku-20241022",  # Using the correct model name
            temperature=0.6,
            max_tokens=2000
        )

        # Use Pydantic Output Parser for more robust parsing
        parser =  PydanticOutputParser(pydantic_object=QuestionOutput)
        self.output_parser = parser

        self.question_types = {
            "title_theme": {
                "name": "Title/Theme Inference",
                "system_message": """
                You are an expert in creating Korean SAT (SUNEUNG) English reading comprehension questions.
                For Title/Theme Inference questions, strictly adhere to these principles:

                [Core Principles]
                1. Theme Selection
                - Must encompass the entire passage's unity and coherence
                - Should reflect the main argument or central idea
                - Must avoid overly abstract or broad generalizations
                - Should capture the author's primary intention

                2. Question Construction
                - Evaluate understanding of overall text flow
                - Test ability to connect ideas across paragraphs
                - Assess comprehension of author's perspective
                - Check recognition of recurring key concepts

                3. Answer Choice Design
                - Create plausible but clearly incorrect alternatives
                - Include partially correct but incomplete options
                - Add choices that confuse topic with theme
                - Present subtly different interpretations

                4. Difficulty Calibration
                - Consider explicit vs implicit themes
                - Adjust complexity of thematic integration
                - Vary similarity between choices
                - Control passage structural complexity
                """
            },
            "blank_inference": {
                "name": "Blank Inference",
                "system_message": """
                You are an expert in creating Korean SAT (SUNEUNG) English reading comprehension questions.
                For Blank Inference questions, strictly adhere to these principles:

                [Core Principles]
                1. Logical Flow
                - Ensure clear logical progression
                - Place contextual clues strategically
                - Maintain consistency with overall argument
                - Create clear path to correct answer

                2. Question Design
                - Strategic blank placement at key points
                - Clear logical connection requirements
                - Multiple supporting context clues
                - Connection to main argument

                3. Answer Choice Creation
                - Logically plausible alternatives
                - Contextually related but incorrect options
                - Varying degrees of partial correctness
                - Common misinterpretation traps

                4. Assessment Criteria
                - Logical reasoning ability
                - Contextual comprehension
                - Understanding of text structure
                - Recognition of argument patterns
                """
            },
            "sentence_insertion": {
                "name": "Sentence Insertion",
                "system_message": """
                You are an expert in creating Korean SAT (SUNEUNG) English reading comprehension questions.
                For Sentence Insertion questions, focus on these elements:

                [Core Principles]
                1. Coherence Assessment
                - Logical flow maintenance
                - Transitional effectiveness
                - Reference consistency
                - Theme continuity

                2. Connection Analysis
                - Forward and backward linking
                - Pronoun references
                - Logical bridges
                - Thematic ties

                3. Position Justification
                - Clear reasoning for placement
                - Multiple connecting points
                - Flow disruption tests
                - Context compatibility

                4. Error Analysis
                - Identification of flow breaks
                - Reference mismatches
                - Logical discontinuities
                - Thematic interruptions
                """
            },
            "order_arrangement": {
                "name": "Paragraph Order",
                "system_message": """
                You are an expert in creating Korean SAT (SUNEUNG) English reading comprehension questions.
                For Paragraph Order questions, focus on these elements:

                [Core Principles]
                1. Logical Sequence
                - Clear progression of ideas
                - Cause-effect relationships
                - Chronological order where applicable
                - Conceptual development

                2. Transitional Elements
                - Connective devices
                - Reference words
                - Topic progression
                - Idea development

                3. Structural Analysis
                - Opening paragraph identification
                - Concluding paragraph placement
                - Supporting detail arrangement
                - Argument construction

                4. Coherence Markers
                - Signal words and phrases
                - Referential links
                - Thematic consistency
                - Logical flow indicators
                """
            },
            "vocabulary": {
                "name": "Vocabulary in Context",
                "system_message": """
                You are an expert in creating Korean SAT (SUNEUNG) English reading comprehension questions.
                For Vocabulary in Context questions, focus on these elements:

                [Core Principles]
                1. Contextual Meaning
                - Word usage in specific context
                - Connotative vs denotative meanings
                - Register appropriateness
                - Semantic precision

                2. Distractor Design
                - Common misinterpretations
                - Related but incorrect meanings
                - Context-specific confusion
                - Secondary definitions

                3. Context Analysis
                - Surrounding phrase analysis
                - Sentence-level meaning
                - Paragraph context
                - Overall passage tone

                4. Usage Evaluation
                - Collocational appropriateness
                - Stylistic consistency
                - Author's intention
                - Meaning precision
                """
            },
            "main_idea": {
                "name": "Main Idea/Argument",
                "system_message": """
                You are an expert in creating Korean SAT (SUNEUNG) English reading comprehension questions.
                For Main Idea/Argument questions, focus on these elements:

                [Core Principles]
                1. Central Concept Identification
                - Primary argument recognition
                - Supporting evidence analysis
                - Author's perspective
                - Key message extraction

                2. Supporting Detail Analysis
                - Evidence evaluation
                - Example relevance
                - Argument structure
                - Logical development

                3. Author's Purpose
                - Intention analysis
                - Tone evaluation
                - Persuasive elements
                - Communication goals

                4. Argument Evaluation
                - Logical consistency
                - Evidence sufficiency
                - Reasoning patterns
                - Conclusion validity
                """
            },
            "implied_meaning": {
                "name": "Implied Meaning",
                "system_message": """
                You are an expert in creating Korean SAT (SUNEUNG) English reading comprehension questions.
                For Implied Meaning questions, focus on these elements:

                [Core Principles]
                1. Inference Skills
                - Reading between lines
                - Context clue analysis
                - Tone interpretation
                - Underlying message detection

                2. Evidence Collection
                - Textual support identification
                - Context consideration
                - Pattern recognition
                - Tone analysis

                3. Logical Deduction
                - Reasonable conclusion drawing
                - Supporting evidence linking
                - Context integration
                - Assumption evaluation

                4. Alternative Interpretation
                - Multiple perspective consideration
                - Contextual validity
                - Logical consistency
                - Evidence alignment
                """
            },
            "irrelevant_sentence": {
                "name": "Irrelevant Sentence",
                "system_message": """
                You are an expert in creating Korean SAT (SUNEUNG) English reading comprehension questions.
                For Irrelevant Sentence questions, focus on these elements:

                [Core Principles]
                1. Unity Analysis
                - Topic consistency
                - Logical flow disruption
                - Thematic relevance
                - Coherence evaluation

                2. Context Evaluation
                - Local coherence
                - Global coherence
                - Transition appropriateness
                - Information relevance

                3. Disruption Identification
                - Topic drift detection
                - Logical break points
                - Style inconsistencies
                - Focus shifts

                4. Relevance Assessment
                - Main idea alignment
                - Supporting role analysis
                - Information necessity
                - Flow contribution
                """
            },
            "paragraph_summary": {
                "name": "Paragraph Summary",
                "system_message": """
                You are an expert in creating Korean SAT (SUNEUNG) English reading comprehension questions.
                For Paragraph Summary questions, focus on these elements:

                [Core Principles]
                1. Main Point Extraction
                - Central idea identification
                - Key detail selection
                - Supporting information evaluation
                - Theme recognition

                2. Conciseness
                - Essential information selection
                - Redundancy elimination
                - Clarity maintenance
                - Precision in expression

                3. Accuracy
                - Factual correctness
                - Interpretation accuracy
                - Detail relevance
                - Context maintenance

                4. Comprehensiveness
                - Key point inclusion
                - Balanced representation
                - Proper emphasis
                - Complete coverage
                """
            },
            "reading_comprehension": {
                "name": "Reading Comprehension",
                "system_message": """
                You are an expert in creating Korean SAT (SUNEUNG) English reading comprehension questions.
                For Overall Reading Comprehension questions, focus on these elements:

                [Core Principles]
                1. Comprehensive Understanding
                - Main idea grasp
                - Detail recognition
                - Argument structure
                - Author's purpose

                2. Critical Analysis
                - Evidence evaluation
                - Reasoning assessment
                - Assumption identification
                - Conclusion validity

                3. Detail Integration
                - Information synthesis
                - Cross-reference ability
                - Pattern recognition
                - Relationship identification

                4. Application Skills
                - Inference making
                - Principle application
                - Context consideration
                - Perspective evaluation
                """
            }
        }    # [All the question type definitions remain exactly the same]

    def create_chat_prompt(self, text: str, q_type: str):
        """Create a chat prompt for question generation"""
        if q_type not in self.question_types:
            raise ValueError(f"Unknown question type: {q_type}")

        system_message = self.question_types[q_type]["system_message"]
        type_info = self.question_types[q_type]

        human_message = f"""
        Create a {type_info['name']} question based on the following passage for Korean SAT (SUNEUNG) English test.

        [Passage]
        {text}

        Generate response in EXACTLY this JSON format:
        {{
            "question": "문제 내용 (한글)",
            "choices": [
                "1번 선택지 (한글)",
                "2번 선택지 (한글)",
                "3번 선택지 (한글)",
                "4번 선택지 (한글)",
                "5번 선택지 (한글)"
            ],
            "correct_answer": 1,
            "explanation": "자세한 설명 (한글)",
            "difficulty": 3,
            "key_points": [
                "핵심 포인트 1",
                "핵심 포인트 2"
            ],
            "wrong_answer_analysis": {{
                "2": "2번 오답 분석",
                "3": "3번 오답 분석",
                "4": "4번 오답 분석",  
                "5": "5번 오답 분석"
            }}
        }}

        Requirements:
        - All text must be in Korean
        - correct_answer must be a number between 1 and 5
        - difficulty must be a number between 1 and 5
        - key_points must be an array of strings
        - wrong_answer_analysis must be an object with numbers as keys
        - Strictly follow the JSON format above
        """

        messages = [
            SystemMessage(content=system_message),
            HumanMessage(content=human_message)
        ]

        return messages

    def generate_question(self, text: str, q_type: str):
        """Generate a single question of the specified type"""
        messages = self.create_chat_prompt(text, q_type)

        try:
            response = self.llm.invoke(messages)

            try:
                # JSON 파싱을 먼저 시도
                json_response = json.loads(response.content)

                # key_points가 문자열이면 리스트로 변환
                if isinstance(json_response.get('key_points'), str):
                    json_response['key_points'] = [json_response['key_points']]

                # wrong_answer_analysis가 없으면 빈 딕셔너리 추가
                if 'wrong_answer_analysis' not in json_response:
                    json_response['wrong_answer_analysis'] = {}

                # parse 메서드 사용
                result = self.output_parser.parse(json.dumps(json_response))
                result.type = q_type
                result.type_id = self.question_types[q_type]["name"]
                return result

            except json.JSONDecodeError as e:
                st.error(f"JSON 파싱 오류: {str(e)}")
                return None
            except Exception as e:
                st.error(f"응답 파싱 중 오류 발생: {str(e)}")
                st.error(f"원본 응답: {response.content}")
                return None

        except Exception as e:
            st.error(f"질문 생성 중 오류 발생: {str(e)}")
            return None

    def generate_single_question(self, text: str, q_type: str = None) -> Optional[QuestionOutput]:
        """Generate a single question of a specific type or random type"""
        if q_type is None:
            # Randomly select a question type
            q_type = random.choice(list(self.question_types.keys()))

        return self.generate_question(text, q_type)

    def generate_all_questions(self, text: str) -> List[QuestionOutput]:
        all_questions = []
        # Randomly select question types if we want more variety
        selected_types = list(self.question_types.keys())[:10]  # Limit to 10 types

        for i, q_type in enumerate(selected_types):
            question = self.generate_question(text, q_type)
            if question:
                question.question_number = i + 1
                all_questions.append(question)
                st.write(f"Generated question {i+1}/10")

        return all_questions

class PDFParser:
    def __init__(self):
        pass

    def process_pdf(self, uploaded_file):
        try:
            # Save the uploaded file temporarily
            with tempfile.NamedTemporaryFile(delete=False, suffix='.pdf') as tmp_file:
                tmp_file.write(uploaded_file.getvalue())
                tmp_path = tmp_file.name

            # Extract raw text using PyMuPDF
            try:
                doc = pymupdf.open(tmp_path)
                raw_text = ""
                for page in doc:
                    raw_text += page.get_text()
                doc.close()
            except Exception as e:
                st.error(f"PDF 텍스트 추출 중 오류 발생: {str(e)}")
                return None
            finally:
                # Clean up temporary file
                os.unlink(tmp_path)

            if not raw_text.strip():
                st.error("PDF에서 텍스트를 추출할 수 없습니다.")
                return None

            # Use ChatGPT to clean and extract English passages
            messages = [
                SystemMessage(content="""You are a helpful assistant that extracts and cleans English passages from PDFs.
                Your task is to:
                1. Identify and separate MULTIPLE English passages/articles from the input
                2. For each passage:
                   - Remove any unnecessary whitespace, headers, footers, and page numbers
                   - Ensure proper paragraph breaks
                3. Return the passages in this exact JSON format:
                   {
                     "passages": [
                       {
                         "text": "First passage text...",
                         "word_count": number_of_words
                       },
                       {
                         "text": "Second passage text...",
                         "word_count": number_of_words
                       }
                     ]
                   }
                4. Each passage should be complete and make sense on its own
                5. If there's no meaningful English text, return {"passages": []}"""),
                HumanMessage(content=f"Please extract and clean the English passages from this PDF content:\n\n{raw_text}")
            ]

            chat = ChatAnthropic(
                temperature=0,
                model="claude-3-5-haiku-20241022"
            )

            response = chat.invoke(messages)

            try:
                result = json.loads(response.content)
                if not result.get('passages'):
                    st.error("PDF에서 영어 지문을 찾을 수 없습니다.")
                    return None

                passages = []
                for idx, passage in enumerate(result['passages'], 1):
                    if len(passage['text'].split()) >= 10:  # Minimum 10 words
                        passages.append(passage['text'])
                        st.success(f"지문 {idx} 추출 완료 ({passage['word_count']} 단어)")

                if not passages:
                    st.error("추출된 지문이 너무 짧습니다.")
                    return None

                return passages

            except json.JSONDecodeError:
                st.error("지문 추출 중 오류가 발생했습니다.")
                return None

        except Exception as e:
            st.error(f"PDF 처리 중 오류 발생: {str(e)}")
            return None

    def parse_passages(self, text: str) -> List[Dict[str, str]]:
        """Parse text into passages of appropriate length"""
        try:
            # Clean the text first
            text = self._clean_text(text)

            # Split text into paragraphs
            paragraphs = [p.strip() for p in text.split('\n\n') if p.strip()]

            # Initialize passages list
            passages = []
            current_passage = []
            current_word_count = 0
            target_size = 250  # Target words per passage

            for paragraph in paragraphs:
                # Remove leading/trailing commas and question marks for each paragraph
                paragraph = re.sub(r'^[,\s?]+', '', paragraph)
                paragraph = re.sub(r'[,\s?]+$', '', paragraph)

                # Split paragraph into sentences
                sentences = nltk.sent_tokenize(paragraph)

                for sentence in sentences:
                    # Clean each sentence
                    sentence = re.sub(r'^[,\s?]+', '', sentence)
                    sentence = re.sub(r'[,\s?]+$', '', sentence)

                    words = sentence.split()
                    word_count = len(words)

                    # If adding this sentence would exceed target size and we already have content
                    if current_word_count + word_count > target_size and current_passage:
                        # Save current passage
                        passage_text = ' '.join(current_passage)
                        passages.append({
                            'text': passage_text,
                            'word_count': current_word_count,
                            'sentence_count': len(current_passage)
                        })
                        # Start new passage
                        current_passage = [sentence]
                        current_word_count = word_count
                    else:
                        # Add sentence to current passage
                        current_passage.append(sentence)
                        current_word_count += word_count

            # Add the last passage if it exists
            if current_passage:
                passage_text = ' '.join(current_passage)
                passages.append({
                    'text': passage_text,
                    'word_count': current_word_count,
                    'sentence_count': len(current_passage)
                })

            if not passages:
                raise ValueError("지문을 생성할 수 없습니다.")

            return passages

        except Exception as e:
            raise Exception(f"지문 파싱 중 오류 발생: {str(e)}")

    def _clean_text(self, text: str) -> str:
        """Clean and normalize the extracted text"""
        if not text:
            return ""

        # Basic cleaning
        text = text.replace('\x00', '')  # Remove null bytes
        text = re.sub(r'\s*\n\s*', '\n', text)  # Normalize line endings
        text = re.sub(r'\n{3,}', '\n\n', text)  # Reduce multiple newlines
        text = re.sub(r'\s+', ' ', text)  # Normalize whitespace

        # Remove leading/trailing whitespace, commas, and question marks
        text = text.strip()
        text = re.sub(r'^[,\s?]+', '', text)  # Remove leading commas, spaces, and question marks
        text = re.sub(r'[,\s?]+$', '', text)  # Remove trailing commas, spaces, and question marks

        # Remove non-English characters if english_only is True
        if self.english_only:
            # Keep basic punctuation and formatting
            text = re.sub(r'[^a-zA-Z0-9.,!?\'\"()\s\-:;\n]', '', text)

        # Clean up multiple spaces again after all replacements
        text = re.sub(r'\s+', ' ', text).strip()

        return text


class DBManager:
    def __init__(self):
        """Initialize the MongoDB connection."""
        try:
            # Load environment variables
            load_dotenv()

            # Get MongoDB password from environment variable
            mongodb_password = os.getenv('MONGODB_PASSWORD')
            if not mongodb_password:
                raise ValueError("MONGODB_PASSWORD environment variable is not set")

            # MongoDB Atlas connection string with password from environment variable
            uri = f"mongodb+srv://junwoo0755:{mongodb_password}@gonzalolkor.d5lrr.mongodb.net/?retryWrites=true&w=majority&appName=GonzaloLKor"

            # Connect to MongoDB
            self.client = MongoClient(uri)

            # Test connection
            self.client.admin.command('ping')

            # Initialize database and collections
            self.db = self.client['questions_db']
            self.passages = self.db['passages']
            self.questions = self.db['questions']
            self.users = self.db['users']

            # Create indexes
            self.passages.create_index('title')
            self.questions.create_index('passage_id')
            self.users.create_index('email', unique=True)

            st.success("MongoDB에 성공적으로 연결되었습니다!")

        except ValueError as ve:
            st.error(str(ve))
            raise
        except Exception as e:
            st.error(f"MongoDB 연결 중 오류 발생: {str(e)}")
            raise

    def create_tables(self):
        """No need to create tables in MongoDB as collections are created automatically"""
        pass

    def save_question(self, passage_id, question):
        """Save a question to MongoDB."""
        try:
            question_data = {
                'passage_id': passage_id,
                'question': question.dict(),  # 모든 질문 데이터를 question 필드 아래에 저장
                'created_at': datetime.now()
            }
            self.questions.insert_one(question_data)
        except Exception as e:
            st.error(f"질문 저장 중 오류 발생: {str(e)}")
            raise

    def get_questions_for_passage(self, passage_id):
        """Get all questions for a specific passage."""
        try:
            questions = list(self.questions.find({'passage_id': passage_id}, {'_id': 0}))
            return [QuestionOutput(
                question=q['question']['question'],
                choices=q['question']['choices'],
                correct_answer=q['question']['correct_answer'],
                explanation=q['question']['explanation'],
                difficulty=q['question']['difficulty'],
                type=q['question']['type'],
                type_id=q['question']['type_id'],
                question_number=q['question']['question_number'],
                original_text=q['question'].get('original_text', ''),
                korean_translation=q['question'].get('korean_translation', '')
            ) for q in questions]
        except Exception as e:
            st.error(f"질문 조회 중 오류 발생: {str(e)}")
            return []

    def get_passages(self):
        """Get all passages."""
        try:
            passages = list(self.passages.find({}))
            # MongoDB의 _id를 id로 변환
            return [{
                'id': str(p['_id']),
                'title': p['title'],
                'text': p['text']
            } for p in passages]
        except Exception as e:
            st.error(f"지문 조회 중 오류 발생: {str(e)}")
            return []

    def get_passage_id(self, passage_text):
        """Find passage_id by text."""
        try:
            passage = self.passages.find_one({'text': passage_text})
            return str(passage['_id']) if passage else None
        except Exception as e:
            st.error(f"지문 ID 조회 중 오류 발생: {str(e)}")
            return None

    def save_passage(self, title: str, text: str):
        """Save a passage and return its ID."""
        try:
            passage_data = {
                'title': title,
                'text': text,
                'created_at': datetime.now()
            }
            result = self.passages.insert_one(passage_data)
            return str(result.inserted_id)
        except Exception as e:
            st.error(f"지문 저장 중 오류 발생: {str(e)}")
            raise

    def get_questions_by_passage_id(self, passage_id):
        """Get all questions for a specific passage."""
        try:
            questions = list(self.questions.find({'passage_id': passage_id}, {'_id': 0}))
            return [QuestionOutput(
                question=q['question']['question'],
                choices=q['question']['choices'],
                correct_answer=q['question']['correct_answer'],
                explanation=q['question']['explanation'],
                difficulty=q['question']['difficulty'],
                type=q['question']['type'],
                type_id=q['question']['type_id'],
                question_number=q['question']['question_number'],
                original_text=q['question'].get('original_text', ''),
                korean_translation=q['question'].get('korean_translation', '')
            ) for q in questions]
        except Exception as e:
            st.error(f"질문 조회 중 오류 발생: {str(e)}")
            return []

    def has_questions(self, passage_id):
        """Check if a passage has questions."""
        try:
            return self.questions.count_documents({'passage_id': passage_id}) > 0
        except Exception as e:
            st.error(f"질문 확인 중 오류 발생: {str(e)}")
            return False

    def register_user(self, email, password):
        """새 사용자 등록"""
        try:
            hashed_pw = hashlib.sha256(password.encode()).hexdigest()
            user_data = {
                'email': email,
                'password': hashed_pw,
                'created_at': datetime.now(),
                'is_verified': False,
                'verification_token': None,
                'subscription': {
                    'is_active': False,
                    'expiry_date': None
                }
            }
            self.users.insert_one(user_data)
            return True
        except pymongo.errors.DuplicateKeyError:
            st.error('이미 등록된 이메일입니다.')
            return False
        except Exception as e:
            st.error(f'사용자 등록 중 오류가 발생했습니다: {str(e)}')
            return False

    def login(self, email, password):
        """사용자 로그인"""
        try:
            hashed_pw = hashlib.sha256(password.encode()).hexdigest()
            user = self.users.find_one({'email': email, 'password': hashed_pw})
            if user:
                st.session_state.user = {
                    'email': user['email'],
                    'subscription': user['subscription']
                }
                return True
            return False
        except Exception as e:
            st.error(f'로그인 중 오류가 발생했습니다: {str(e)}')
            return False

    def logout(self):
        """사용자 로그아웃"""
        if 'user' in st.session_state:
            del st.session_state.user

    def check_subscription_status(self):
        """구독 상태 확인"""
        if 'user' not in st.session_state:
            return False

        user = self.users.find_one({'email': st.session_state.user['email']})
        if not user:
            return False

        subscription = user['subscription']
        if not subscription['is_active']:
            return False

        # 구독 만료 확인
        if subscription['expiry_date'] and datetime.now() > subscription['expiry_date']:
            self.unsubscribe()  # 만료된 구독 처리
            return False

        return True

    def get_subscription_expiry(self):
        """구독 만료일 조회"""
        if 'user' not in st.session_state:
            return None

        user = self.users.find_one({'email': st.session_state.user['email']})
        if not user or not user['subscription']['is_active']:
            return None

        return user['subscription']['expiry_date']

    def subscribe(self):
        """구독 활성화"""
        if 'user' not in st.session_state:
            st.error('로그인이 필요합니다.')
            return False

        expiry_date = datetime.now() + timedelta(days=30)  # 30일 구독

        try:
            self.users.update_one(
                {'email': st.session_state.user['email']},
                {
                    '$set': {
                        'subscription': {
                            'is_active': True,
                            'expiry_date': expiry_date
                        }
                    }
                }
            )
            st.session_state.user['subscription'] = {
                'is_active': True,
                'expiry_date': expiry_date
            }
            return True
        except Exception as e:
            st.error(f'구독 활성화 중 오류가 발생했습니다: {str(e)}')
            return False

    def unsubscribe(self):
        """구독 비활성화"""
        if 'user' not in st.session_state:
            return

        try:
            self.users.update_one(
                {'email': st.session_state.user['email']},
                {
                    '$set': {
                        'subscription': {
                            'is_active': False,
                            'expiry_date': None
                        }
                    }
                }
            )
            st.session_state.user['subscription'] = {
                'is_active': False,
                'expiry_date': None
            }
        except Exception as e:
            st.error(f'구독 비활성화 중 오류가 발생했습니다: {str(e)}')

db = DBManager()

class SubscriptionManager:
    def __init__(self, db_manager):
        """Initialize the subscription manager."""
        self.db = db_manager
        self.FREE_QUESTION_LIMIT = 20  # 무료 버전의 문제 제한 수

    def check_subscription_status(self):
        """Check if the user has an active subscription."""
        if 'user' not in st.session_state:
            return False

        user = self.db.users.find_one({'email': st.session_state.user['email']})
        if not user:
            return False

        subscription = user['subscription']
        if not subscription['is_active']:
            return False

        # 구독 만료 확인
        if subscription['expiry_date'] and datetime.now() > subscription['expiry_date']:
            self.unsubscribe()  # 만료된 구독 처리
            return False

        return True

    def get_subscription_expiry(self):
        """Get the subscription expiry date."""
        if 'user' not in st.session_state:
            return None

        user = self.db.users.find_one({'email': st.session_state.user['email']})
        if not user or not user['subscription']['is_active']:
            return None

        return user['subscription']['expiry_date']

    def reset_daily_questions_if_needed(self):
        """Reset the daily question count if it's a new day."""
        if 'user' not in st.session_state:
            return

        user = self.db.users.find_one({'email': st.session_state.user['email']})
        if not user:
            return

        last_reset = user.get('last_question_reset', None)
        now = datetime.now()

        # Convert last_reset to datetime if it's a string
        if isinstance(last_reset, str):
            last_reset = datetime.fromisoformat(last_reset.replace('Z', '+00:00'))

        # Reset if it's the first time or if it's a new day
        if not last_reset or (now.date() > last_reset.date()):
            self.db.users.update_one(
                {'email': st.session_state.user['email']},
                {
                    '$set': {
                        'questions_used': 0,
                        'last_question_reset': now
                    }
                }
            )

    def get_remaining_questions(self):
        """Get the number of remaining questions for free users."""
        if 'user' not in st.session_state:
            return 0

        # Reset questions if needed before checking remaining count
        self.reset_daily_questions_if_needed()

        user = self.db.users.find_one({'email': st.session_state.user['email']})
        if not user:
            return 0

        questions_used = user.get('questions_used', 0)
        return self.FREE_QUESTION_LIMIT - questions_used

    def increment_question_count(self):
        """Increment the number of questions used."""
        if 'user' not in st.session_state:
            return

        # Reset questions if needed before incrementing
        self.reset_daily_questions_if_needed()

        self.db.users.update_one(
            {'email': st.session_state.user['email']},
            {'$inc': {'questions_used': 1}}
        )

    def can_generate_question(self):
        """Check if the user can generate more questions."""
        if self.check_subscription_status():
            return True
        return self.get_remaining_questions() > 0

    def subscribe(self):
        """Activate subscription for the user."""
        if 'user' not in st.session_state:
            st.error('로그인이 필요합니다.')
            return False

        expiry_date = datetime.now() + timedelta(days=30)  # 30일 구독

        try:
            self.db.users.update_one(
                {'email': st.session_state.user['email']},
                {
                    '$set': {
                        'subscription': {
                            'is_active': True,
                            'expiry_date': expiry_date
                        }
                    }
                }
            )
            st.session_state.user['subscription'] = {
                'is_active': True,
                'expiry_date': expiry_date
            }
            return True
        except Exception as e:
            st.error(f'구독 활성화 중 오류가 발생했습니다: {str(e)}')
            return False

    def unsubscribe(self):
        """Deactivate subscription for the user."""
        if 'user' not in st.session_state:
            return

        try:
            self.db.users.update_one(
                {'email': st.session_state.user['email']},
                {
                    '$set': {
                        'subscription': {
                            'is_active': False,
                            'expiry_date': None
                        }
                    }
                }
            )
            st.session_state.user['subscription'] = {
                'is_active': False,
                'expiry_date': None
            }
        except Exception as e:
            st.error(f'구독 비활성화 중 오류가 발생했습니다: {str(e)}')

subscription_manager = SubscriptionManager(db)

class EmailManager:
    def __init__(self, email_address, email_password):
        self.email_address = email_address
        self.email_password = email_password
        self.jwt_secret = os.getenv('JWT_SECRET_KEY')

    def generate_verification_token(self, email):
        """이메일 인증 토큰 생성"""
        try:
            expiry = datetime.now() + timedelta(hours=24)
            token_data = {
                'email': email,
                'exp': expiry.timestamp()
            }
            return jwt.encode(token_data, self.jwt_secret, algorithm='HS256')
        except Exception as e:
            st.error(f'토큰 생성 중 오류 발생: {str(e)}')
            return None

    def verify_token(self, token):
        """이메일 인증 토큰 검증"""
        try:
            payload = jwt.decode(token, self.jwt_secret, algorithms=['HS256'])
            if datetime.fromtimestamp(payload['exp']) < datetime.now():
                return None
            return payload['email']
        except Exception:
            return None

    def send_email(self, to_email, subject, body):
        """HTML 형식의 이메일 발송"""
        try:
            msg = MIMEMultipart('alternative')
            msg['Subject'] = subject
            msg['From'] = self.email_address
            msg['To'] = to_email

            html_part = MIMEText(body, 'html')
            msg.attach(html_part)

            with smtplib.SMTP_SSL('smtp.gmail.com', 465) as server:
                server.login(self.email_address, self.email_password)
                server.send_message(msg)
            return True
        except Exception as e:
            st.error(f'이메일 발송 중 오류 발생: {str(e)}')
            return False

class UserAuth:
    def __init__(self, db_manager, email_manager):
        self.db = db_manager
        self.email_manager = email_manager
        self.db.users = self.db.db['users']
        self.db.users.create_index('email', unique=True)
        self.db.reset_tokens = self.db.db['reset_tokens']
        self.db.reset_tokens.create_index('expires_at', expireAfterSeconds=0)
        self.db.subscription_requests = self.db.db['subscription_requests']

        # 관리자 계정이 없으면 생성
        if not self.db.users.find_one({'is_admin': True}):
            admin_email = os.getenv('ADMIN_EMAIL', 'admin@ssalmuk.com')
            admin_password = os.getenv('ADMIN_PASSWORD', 'adminpassword')
            self.register_admin(admin_email, admin_password)

    def hash_password(self, password):
        """비밀번호 해시화"""
        return hashlib.sha256(password.encode()).hexdigest()

    def login(self, email, password):
        """사용자 로그인"""
        try:
            hashed_pw = self.hash_password(password)
            user = self.db.users.find_one({
                'email': email,
                'password': hashed_pw
            })

            if not user:
                st.error('이메일 또는 비밀번호가 올바르지 않습니다.')
                return False

            if not user.get('is_verified', False) and not user.get('is_admin', False):
                st.error('이메일 인증이 필요합니다. 이메일을 확인해주세요.')
                return False

            # 세션에 사용자 정보 저장
            st.session_state.user = {
                'email': user['email'],
                'is_admin': user.get('is_admin', False),
                'subscription': user.get('subscription', {
                    'is_active': False,
                    'expiry_date': None
                })
            }
            return True

        except Exception as e:
            st.error(f'로그인 중 오류가 발생했습니다: {str(e)}')
            return False

    def logout(self):
        """사용자 로그아웃"""
        if 'user' in st.session_state:
            del st.session_state.user

    def register_admin(self, email, password):
        """관리자 계정 등록"""
        try:
            hashed_pw = self.hash_password(password)
            admin_data = {
                'email': email,
                'password': hashed_pw,
                'created_at': datetime.now(),
                'is_verified': True,
                'is_admin': True,
                'subscription': {
                    'is_active': True,
                    'expiry_date': None
                }
            }
            self.db.users.insert_one(admin_data)
            return True
        except Exception as e:
            st.error(f'관리자 계정 생성 중 오류 발생: {str(e)}')
            return False

    def register_user(self, email, password):
        """새 사용자 등록"""
        try:
            # 이미 존재하는 이메일인지 확인
            if self.db.users.find_one({'email': email}):
                st.error('이미 등록된 이메일입니다.')
                return False

            # 인증 토큰 생성
            token = self.email_manager.generate_verification_token(email)

            # 인증 이메일 발송
            email_body = f"""
            <h2>내신쌀먹 이메일 인증</h2>
            <p>아래 인증 코드를 입력하여 이메일을 인증해주세요:</p>
            <h3 style="background-color: #f0f0f0; padding: 10px; font-family: monospace;">{token}</h3>
            <p>이 인증 코드는 24시간 동안 유효합니다.</p>
            """

            if not self.email_manager.send_email(email, "이메일 인증", email_body):
                st.error('인증 이메일 발송에 실패했습니다.')
                return False

            # 사용자 정보 저장
            hashed_pw = self.hash_password(password)
            user_data = {
                'email': email,
                'password': hashed_pw,
                'created_at': datetime.now(),
                'is_verified': False,
                'verification_token': token,
                'subscription': {
                    'is_active': False,
                    'expiry_date': None
                }
            }
            self.db.users.insert_one(user_data)

            # 세션에 이메일 저장
            if 'registration_email' not in st.session_state:
                st.session_state.registration_email = email
                st.session_state.verification_sent = True

            st.success('인증 이메일이 발송되었습니다. 이메일을 확인해주세요.')
            return True

        except Exception as e:
            st.error(f'사용자 등록 중 오류가 발생했습니다: {str(e)}')
            return False

    def verify_email(self, email, token):
        """이메일 인증"""
        try:
            user = self.db.users.find_one({
                'email': email,
                'verification_token': token,
                'is_verified': False
            })

            if not user:
                st.error('유효하지 않은 인증 코드입니다.')
                return False

            # 토큰 유효성 검사
            if not self.email_manager.verify_token(token):
                st.error('만료된 인증 코드입니다.')
                return False

            # 사용자 인증 상태 업데이트
            self.db.users.update_one(
                {'email': email},
                {
                    '$set': {
                        'is_verified': True,
                        'verification_token': None
                    }
                }
            )

            st.success('이메일이 성공적으로 인증되었습니다!')
            return True

        except Exception as e:
            st.error(f'이메일 인증 중 오류가 발생했습니다: {str(e)}')
            return False

    def request_password_reset(self, email):
        """비밀번호 재설정 요청"""
        try:
            user = self.db.users.find_one({'email': email})
            if not user:
                st.error('등록되지 않은 이메일입니다.')
                return False

            # 재설정 토큰 생성
            token = self.email_manager.generate_verification_token(email)

            # 재설정 이메일 발송
            email_body = f"""
            <h2>내신쌀먹 비밀번호 재설정</h2>
            <p>아래 인증 코드를 입력하여 비밀번호를 재설정하세요:</p>
            <h3 style="background-color: #f0f0f0; padding: 10px; font-family: monospace;">{token}</h3>
            <p>이 인증 코드는 24시간 동안 유효합니다.</p>
            """

            if not self.email_manager.send_email(email, "비밀번호 재설정", email_body):
                st.error('재설정 이메일 발송에 실패했습니다.')
                return False

            st.success('비밀번호 재설정 이메일이 발송되었습니다.')
            return True

        except Exception as e:
            st.error(f'비밀번호 재설정 요청 중 오류가 발생했습니다: {str(e)}')
            return False

    def reset_password(self, token, new_password):
        """비밀번호 재설정"""
        try:
            email = self.email_manager.verify_token(token)
            if not email:
                st.error('유효하지 않거나 만료된 토큰입니다.')
                return False

            hashed_pw = self.hash_password(new_password)
            result = self.db.users.update_one(
                {'email': email},
                {'$set': {'password': hashed_pw}}
            )

            if result.modified_count == 0:
                st.error('비밀번호 재설정에 실패했습니다.')
                return False

            st.success('비밀번호가 성공적으로 재설정되었습니다!')
            return True

        except Exception as e:
            st.error(f'비밀번호 재설정 중 오류가 발생했습니다: {str(e)}')
            return False

    def is_admin(self, email):
        """사용자가 관리자인지 확인"""
        try:
            user = self.db.users.find_one({'email': email})
            return user and user.get('is_admin', False)
        except Exception as e:
            st.error(f'관리자 확인 중 오류가 발생했습니다: {str(e)}')
            return False

    def get_pending_subscriptions(self):
        """대기 중인 구독 요청 목록 조회"""
        try:
            return list(self.db.subscription_requests.find({'status': 'pending'}))
        except Exception as e:
            st.error(f'구독 요청 조회 중 오류가 발생했습니다: {str(e)}')
            return []

    def approve_subscription(self, request_id):
        """구독 요청 승인"""
        try:
            request = self.db.subscription_requests.find_one({'_id': request_id})
            if not request:
                st.error('구독 요청을 찾을 수 없습니다.')
                return False

            # 구독 상태 업데이트
            self.db.users.update_one(
                {'email': request['email']},
                {
                    '$set': {
                        'subscription': {
                            'is_active': True,
                            'expiry_date': datetime.now() + timedelta(days=30)
                        }
                    }
                }
            )

            # 요청 상태 업데이트
            self.db.subscription_requests.update_one(
                {'_id': request_id},
                {'$set': {'status': 'approved'}}
            )

            # 승인 이메일 발송
            email_body = """
            <h2>내신쌀먹 구독 승인</h2>
            <p>회원님의 구독 요청이 승인되었습니다.</p>
            <p>이제 프리미엄 기능을 이용하실 수 있습니다.</p>
            """
            self.email_manager.send_email(request['email'], '구독 승인 알림', email_body)

            st.success('구독 요청이 승인되었습니다.')
            return True

        except Exception as e:
            st.error(f'구독 승인 중 오류가 발생했습니다: {str(e)}')
            return False

    def deny_subscription(self, request_id):
        """구독 요청 거절"""
        try:
            request = self.db.subscription_requests.find_one({'_id': request_id})
            if not request:
                st.error('구독 요청을 찾을 수 없습니다.')
                return False

            # 요청 상태 업데이트
            self.db.subscription_requests.update_one(
                {'_id': request_id},
                {'$set': {'status': 'denied'}}
            )

            # 거절 이메일 발송
            email_body = """
            <h2>내신쌀먹 구독 거절 알림</h2>
            <p>죄송합니다. 회원님의 구독 요청이 거절되었습니다.</p>
            <p>자세한 사항은 고객센터로 문의해주세요.</p>
            """
            self.email_manager.send_email(request['email'], '구독 거절 알림', email_body)

            st.success('구독 요청이 거절되었습니다.')
            return True

        except Exception as e:
            st.error(f'구독 거절 중 오류가 발생했습니다: {str(e)}')
            return False

    def request_subscription(self, email):
        """구독 신청"""
        try:
            # 이미 신청한 구독이 있는지 확인
            existing_request = self.db.subscription_requests.find_one({
                "email": email,
                "status": "pending"
            })
            if existing_request:
                st.warning("이미 구독 신청이 진행 중입니다. 승인을 기다려주세요.")
                return False

            # 새로운 구독 신청 생성
            subscription_request = {
                "email": email,
                "status": "pending",
                "requested_at": datetime.now(),
                "expires_at": None
            }
            self.db.subscription_requests.insert_one(subscription_request)
            return True
        except Exception as e:
            st.error(f"구독 신청 중 오류가 발생했습니다: {str(e)}")
            return False

class AdManager:
    def __init__(self):
        self.ad_shown = False
        self.last_ad_time = 0
        self.min_ad_interval = 900  # 15분 (초 단위)

    def should_show_ad(self):
        current_time = time.time()
        if not self.ad_shown or (current_time - self.last_ad_time) > self.min_ad_interval:
            return True
        return False

    def show_ad(self):
        if self.should_show_ad():
            st.warning("광고를 시청하시면 무료로 이용하실 수 있습니다!")

            # 광고 컨테이너 생성
            ad_container = st.empty()

            # 광고 버튼 표시
            if st.button("광고 시청하기 (15초)", key="ad_button"):
                with st.spinner("광고를 시청하고 있습니다..."):
                    # AdSense 비디오 광고 표시
                    ad_container.markdown("""
                        <div align="center">
                            <script async src="https://pagead2.googlesyndication.com/pagead/js/adsbygoogle.js?client=YOUR-CLIENT-ID"></script>
                            <ins class="adsbygoogle"
                                style="display:inline-block;width:300px;height:250px"
                                data-ad-client="YOUR-CLIENT-ID"
                                data-ad-slot="YOUR-AD-SLOT"
                                data-ad-format="video"></ins>
                            <script>
                                (adsbygoogle = window.adsbygoogle || []).push({});
                            </script>
                        </div>
                    """, unsafe_allow_html=True)

                    # 15초 타이머
                    progress_bar = st.progress(0)
                    for i in range(100):
                        time.sleep(0.15)
                        progress_bar.progress(i + 1)

                    self.ad_shown = True
                    self.last_ad_time = time.time()
                    ad_container.empty()
                    st.success("광고 시청이 완료되었습니다!")
                    return True
                return False
            return True

# AdManager 인스턴스 생성
ad_manager = AdManager()

# EmailManager 인스턴스 생성
email_manager = EmailManager(
    email_address=os.getenv('EMAIL_ADDRESS'),
    email_password=os.getenv('EMAIL_PASSWORD')
)

# UserAuth 인스턴스 생성
user_auth = UserAuth(db, email_manager)

def save_questions_to_file(questions, file_path="saved_questions.json"):
    """ Saves the generated questions to a JSON file. """
    try:
        formatted_questions = [{
            'question': q.question,
            'choices': q.choices,
            'correct_answer': q.correct_answer,
            'explanation': q.explanation,
            'difficulty': q.difficulty,
            'key_points': q.key_points,
            'wrong_answer_analysis': q.wrong_answer_analysis
        } for q in questions]
        with open(file_path, 'w', encoding='utf-8') as f:
            json.dump(formatted_questions, f, ensure_ascii=False, indent=4)
        st.success("Questions saved successfully!")
    except Exception as e:
        st.error(f"Error saving questions: {e}")

def load_questions_from_file(file_path="saved_questions.json"):
    """ Loads questions from a JSON file. """
    try:
        with open(file_path, 'r', encoding='utf-8') as f:
            questions = json.load(f)
        st.success("Questions loaded successfully!")
        # Convert dict to structured format using parser
        return [QuestionOutput(**q) for q in questions]
    except FileNotFoundError:
        st.error("File not found. Please generate questions first.")
        return []
    except Exception as e:
        st.error(f"Error loading questions: {e}")
        return []

def save_questions_to_txt(questions, file_path="saved_questions.txt"):
    """ Saves the generated questions to a TXT file. """
    try:
        with open(file_path, 'w') as txt_file:
            for question in questions:
                txt_file.write(f"{question}\n")
        st.success("Questions saved to TXT file successfully!")
    except IOError as e:
        st.error(f"Error saving questions to TXT file: {e}")

def sanitize_filename(filename: str) -> str:
    """Clean up filename and convert to proper encoding"""
    try:
        # Remove invalid characters and convert to proper encoding
        clean_name = re.sub(r'[^\w\s-]', '', filename.encode('cp437').decode('cp949'))
        return clean_name.strip()
    except Exception as e:
        st.error(f"Filename encoding error: {e}")
        return "unnamed_file"

def split_text(text, max_words=150):
    """긴 텍스트를 여러 개의 작은 단락으로 나누는 함수"""
    words = text.split()
    passages = []
    current_passage = []
    current_word_count = 0

    for word in words:
        current_passage.append(word)
        current_word_count += 1

        if current_word_count >= max_words and current_passage:
            # Save current passage
            passage_text = ' '.join(current_passage)
            passages.append({
                'text': passage_text,
                'word_count': current_word_count,
                'sentence_count': len(current_passage)
            })
            # Start new passage
            current_passage = []
            current_word_count = 0

    if current_passage:  # 마지막 단락
        passage_text = ' '.join(current_passage)
        passages.append({
            'text': passage_text,
            'word_count': current_word_count,
            'sentence_count': len(current_passage)
        })

    return passages

def process_passage_and_generate_questions(passage_text, db_manager, passage_id=None, title=None):
    # 광고 체크
    if not ad_manager.show_ad():
        st.warning("서비스를 이용하시려면 광고를 시청해 주세요.")
        return None

    progress_bar = None
    status_text = None

    try:
        # 이미 질문이 있는지 확인
        if passage_id and db_manager.has_questions(passage_id):
            st.warning("이미 이 지문에 대한 문제가 생성되어 있습니다.")
            return False

        progress_bar = st.progress(0)
        status_text = st.empty()

        # 진행 상태 표시
        status_text.text("지문을 분석하고 있습니다...")
        progress_bar.progress(10)

        # 지문이 너무 길면 분할
        if len(passage_text.split()) > 150:
            passages = split_text(passage_text)
        else:
            passages = [passage_text]

        progress_bar.progress(20)
        status_text.text("한국어 번역을 생성하고 있습니다...")

        # 번역 생성
        generator = QuestionGenerator()
        translation_prompt = f"""
        다음 영어 지문을 한국어로 번역해주세요. 자연스러운 한국어로 번역하되,
        원문의 의미와 뉘앑스를 정확하게 전달하는 것이 중요합니다:

        {passage_text}
        """

        translation_messages = [
            SystemMessage(content="You are a professional English to Korean translator."),
            HumanMessage(content=translation_prompt)
        ]

        translation = generator.llm(translation_messages).content
        progress_bar.progress(40)
        status_text.text("문제를 생성하고 있습니다...")

        all_questions = []
        total_passages = len(passages)
        for i, p in enumerate(passages):
            questions = generator.generate_all_questions(p)
            all_questions.extend(questions)
            # 진행률 업데이트 (40%~80%)
            progress = 40 + int((i + 1) / total_passages * 40)
            progress_bar.progress(progress)
            status_text.text(f"문제 생성 중... ({i+1}/{total_passages})")

        progress_bar.progress(80)
        status_text.text("데이터베이스에 저장하고 있습니다...")

        # 질문 저장
        if passage_id is None and title:  # 새 지문인 경우
            passage_id = db_manager.save_passage(title, passage_text)

        for q in all_questions:
            q.original_text = passage_text
            q.korean_translation = translation
            db_manager.save_question(passage_id, q)

        progress_bar.progress(100)
        status_text.text("완료!")
        st.success(f'총 {len(all_questions)}개의 문제가 생성되었습니다!')

        # 잠시 후 progress bar와 status text 제거
        time.sleep(1)
        if progress_bar:
            progress_bar.empty()
        if status_text:
            status_text.empty()

        return True

    except Exception as e:
        if progress_bar:
            progress_bar.empty()
        if status_text:
            status_text.empty()
        st.error(f'문제 생성 중 오류가 발생했습니다: {str(e)}')
        return False

def main():
    st.title("내신쌀먹")

    # 세션 상태 초기화
    if 'logged_in' not in st.session_state:
        st.session_state.logged_in = False
    if 'current_auth_tab' not in st.session_state:
        st.session_state.current_auth_tab = "로그인"
    if 'mode' not in st.session_state:
        st.session_state.mode = "edit"
    if 'show_subscription_modal' not in st.session_state:
        st.session_state.show_subscription_modal = False

    # 사이드바 UI
    with st.sidebar:
        # 구독 버튼
        st.markdown("<h2 style='text-align: center; margin-top: 0;'>구독하기</h2>", unsafe_allow_html=True)
        if st.button("프리미엄 구독 신청", key="subscribe_button", type="primary", use_container_width=True):
            st.session_state.show_subscription_modal = True

        # 구독 정보 모달
        if st.session_state.show_subscription_modal:
            with st.expander("구독 신청 안내", expanded=True):
                st.markdown("""
                ### 프리미엄 구독 안내
                #### 월 구독료: 3900원

                #### 입금 계좌 정보
                - 은행: 농협
                - 계좌번호: 3521153275163
                - 예금주: 이준우

                #### 구독 혜택
                - 무제한 문제 생성
                - 모든 기능 이용 가능
                - 광고 제거

                #### 구독 방법
                1. 위 계좌로 구독료 입금
                2. 아래 버튼을 클릭하여 구독 신청
                3. 입금 확인 후 24시간 이내 승인
                """)

                if st.button("입금 완료 - 구독 신청하기", key="confirm_subscription", type="primary"):
                    if user_auth.request_subscription(st.session_state.user['email']):
                        st.success("구독 신청이 완료되었습니다. 입금 확인 후 승인됩니다.")
                        st.session_state.show_subscription_modal = False
                        st.rerun()

                if st.button("닫기", key="close_subscription_modal"):
                    st.session_state.show_subscription_modal = False
                    st.rerun()

        if not st.session_state.logged_in:
            # 커스텀 탭 UI
            cols = st.columns([1,1,1])
            with cols[0]:
                if st.button("로그인", key="tab_login",
                           help="로그인하여 서비스를 이용하세요",
                           use_container_width=True,
                           type="secondary" if st.session_state.current_auth_tab != "로그인" else "primary"):
                    st.session_state.current_auth_tab = "로그인"
                    st.rerun()
            with cols[1]:
                if st.button("회원가입", key="tab_register",
                           help="새로운 계정을 만드세요",
                           use_container_width=True,
                           type="secondary" if st.session_state.current_auth_tab != "회원가입" else "primary"):
                    st.session_state.current_auth_tab = "회원가입"
                    st.rerun()
            with cols[2]:
                if st.button("비밀번호 찾기", key="tab_reset",
                           help="비밀번호를 잊으셨나요?",
                           use_container_width=True,
                           type="secondary" if st.session_state.current_auth_tab != "비밀번호 재설정" else "primary"):
                    st.session_state.current_auth_tab = "비밀번호 재설정"
                    st.rerun()

            st.markdown("<br>", unsafe_allow_html=True)

            # 현재 선택된 탭에 따른 UI
            with st.container():
                if st.session_state.current_auth_tab == "로그인":
                    st.markdown("<h3 style='text-align: center; color: #1D1D1F;'>로그인</h3>", unsafe_allow_html=True)
                    email = st.text_input("이메일", key="login_email", placeholder="example@email.com")
                    password = st.text_input("비밀번호", type="password", key="login_password", placeholder="비밀번호를 입력하세요")

                    if st.button("로그인", key="login_button", use_container_width=True):
                        if user_auth.login(email, password):
                            st.session_state.logged_in = True
                            st.session_state.user_email = email
                            st.rerun()

                elif st.session_state.current_auth_tab == "회원가입":
                    st.markdown("<h3 style='text-align: center; color: #1D1D1F;'>회원가입</h3>", unsafe_allow_html=True)

                    if 'registration_email' in st.session_state and 'verification_sent' in st.session_state:
                        st.info(f"이메일({st.session_state.registration_email})로 발송된 인증 코드를 입력해주세요.")
                        verification_code = st.text_input("인증 코드", key="verification_code", placeholder="6자리 인증 코드")

                        col1, col2 = st.columns([7,3])
                        with col1:
                            if st.button("인증하기", key="verify_button", use_container_width=True):
                                if user_auth.verify_email(st.session_state.registration_email, verification_code):
                                    st.success("이메일 인증이 완료되었습니다!")
                                    del st.session_state.registration_email
                                    del st.session_state.verification_sent
                                    st.rerun()
                                else:
                                    st.error("잘못된 인증 코드입니다.")
                        with col2:
                            if st.button("재발송", key="resend_button", use_container_width=True):
                                user_auth.send_verification_email(st.session_state.registration_email)
                                st.info("인증 코드가 재발송되었습니다.")

                    else:
                        email = st.text_input("이메일", key="register_email", placeholder="example@email.com")
                        password = st.text_input("비밀번호", type="password", key="register_password",
                                              placeholder="8자 이상, 영문/숫자/특수문자")
                        password_confirm = st.text_input("비밀번호 확인", type="password", key="register_password_confirm",
                                                      placeholder="비밀번호를 다시 입력하세요")

                        if st.button("가입하기", key="register_button", use_container_width=True):
                            if password != password_confirm:
                                st.error("비밀번호가 일치하지 않습니다.")
                            else:
                                success = user_auth.register_user(email, password)
                                if success:
                                    st.session_state.registration_email = email
                                    st.session_state.verification_sent = True
                                    st.rerun()

                elif st.session_state.current_auth_tab == "비밀번호 재설정":
                    st.markdown("<h3 style='text-align: center; color: #1D1D1F;'>비밀번호 재설정</h3>", unsafe_allow_html=True)

                    if 'reset_email' in st.session_state:
                        st.info(f"이메일({st.session_state.reset_email})로 발송된 인증 코드를 입력해주세요.")
                        reset_code = st.text_input("인증 코드", placeholder="6자리 인증 코드")
                        new_password = st.text_input("새 비밀번호", type="password",
                                                  placeholder="8자 이상, 영문/숫자/특수문자")
                        new_password_confirm = st.text_input("새 비밀번호 확인", type="password",
                                                          placeholder="비밀번호를 다시 입력하세요")

                        if st.button("비밀번호 변경", use_container_width=True):
                            if new_password != new_password_confirm:
                                st.error("비밀번호가 일치하지 않습니다.")
                            elif user_auth.reset_password(reset_code, new_password):
                                st.success("비밀번호가 성공적으로 변경되었습니다!")
                                del st.session_state.reset_email
                                st.session_state.current_auth_tab = "로그인"
                                st.rerun()

                    else:
                        email = st.text_input("이메일", placeholder="example@email.com")
                        if st.button("인증 코드 받기", use_container_width=True):
                            if user_auth.request_password_reset(email):
                                st.session_state.reset_email = email
                                st.rerun()

        else:
            # 로그인된 상태의 UI
            st.markdown(f"""
                <div style='background-color: white; padding: 15px; border-radius: 10px; margin-bottom: 20px;'>
                    <p style='color: #1D1D1F; margin: 0;'>👋 {st.session_state.user_email}</p>
                </div>
            """, unsafe_allow_html=True)

            if st.button("로그아웃", key="logout_button", type="secondary", use_container_width=True):
                user_auth.logout()
                st.session_state.logged_in = False
                st.session_state.current_auth_tab = "로그인"
                st.rerun()

    # 메인 컨텐츠
    if st.session_state.logged_in:
        user_email = st.session_state.user_email

        # 구독 상태 표시
        st.sidebar.title("구독 상태")
        is_subscribed = subscription_manager.check_subscription_status()
        is_admin = user_auth.is_admin(user_email)

        if is_admin:
            st.sidebar.success("🌟 관리자 계정")

            # 관리자용 구독 관리 인터페이스
            st.subheader("구독 신청 관리")
            pending_requests = user_auth.get_pending_subscriptions()

            for request in pending_requests:
                with st.container():
                    st.markdown(f"""
                        <div style='background-color: white; padding: 15px; border-radius: 10px; margin-bottom: 10px;'>
                            <p style='margin: 0;'><strong>신청자:</strong> {request['email']}</p>
                            <p style='margin: 5px 0;'><strong>신청일:</strong> {request['requested_at'].strftime('%Y-%m-%d %H:%M')}</p>
                        </div>
                    """, unsafe_allow_html=True)
                    col1, col2 = st.columns(2)
                    with col1:
                        if st.button("승인", key=f"approve_{request['_id']}", type="primary"):
                            if user_auth.approve_subscription(request['_id']):
                                st.success("구독이 승인되었습니다.")
                                st.rerun()
                    with col2:
                        if st.button("거절", key=f"reject_{request['_id']}", type="secondary"):
                            if user_auth.deny_subscription(request['_id']):
                                st.success("구독이 거절되었습니다.")
                                st.rerun()

        elif is_subscribed:
            expiry_date = subscription_manager.get_subscription_expiry()
            st.sidebar.markdown("""
                <div style='background-color: white; padding: 15px; border-radius: 10px; margin-bottom: 20px;'>
                    <p style='color: #34C759; margin: 0; font-weight: bold;'>🌟 프리미엄 구독 활성화</p>
                </div>
            """, unsafe_allow_html=True)
            st.sidebar.info(f"구독 만료일: {expiry_date.strftime('%Y-%m-%d')}")

        else:
            remaining = subscription_manager.get_remaining_questions()
            st.sidebar.markdown(f"""
                <div style='background-color: white; padding: 15px; border-radius: 10px; margin-bottom: 20px;'>
                    <p style='color: #FF9500; margin: 0;'>무료 버전 (남은 문제 수: {remaining})</p>
                </div>
            """, unsafe_allow_html=True)

            with st.sidebar.container():
                st.markdown("""
                    <div style='background-color: white; padding: 15px; border-radius: 10px; margin-bottom: 20px;'>
                        <h4 style='color: #1D1D1F; margin-top: 0;'>프리미엄 구독 혜택</h4>
                        <ul style='color: #1D1D1F; margin-bottom: 0;'>
                            <li>무제한 문제 생성</li>
                            <li>광고 제거</li>
                            <li>30일 이용권</li>
                        </ul>
                    </div>
                """, unsafe_allow_html=True)

            if not ad_manager.should_show_ad():
                with st.sidebar.container():
                    st.markdown("""
                        <div style='background-color: white; padding: 15px; border-radius: 10px; margin-bottom: 20px;'>
                            <h4 style='color: #1D1D1F; margin-top: 0;'>구독 신청</h4>
                            <p style='color: #1D1D1F;'>구독 금액: 월 9,900원</p>
                            <p style='color: #1D1D1F;'>계좌 정보:</p>
                            <div style='background-color: #F2F2F7; padding: 10px; border-radius: 8px;'>
                                <p style='margin: 0;'>은행: 카카오뱅크</p>
                                <p style='margin: 5px 0;'>계좌번호: 3333-12-1234567</p>
                                <p style='margin: 0;'>예금주: 내신쌀먹</p>
                            </div>
                        </div>
                    """, unsafe_allow_html=True)
                    st.write("입금 후 아래 버튼을 클릭하시면 확인 후 구독이 활성화됩니다.")
                    if st.button("구독 신청", type="primary", use_container_width=True):
                        if user_auth.request_subscription(user_email):
                            st.success("구독 신청이 완료되었습니다. 입금 확인 후 승인됩니다.")
                            st.rerun()

        # 모드 선택
        mode_tabs = st.tabs(["편집 모드", "학습 모드"])
        with mode_tabs[0]:
            st.title("편집 모드")
            # CSS for loading overlay
            st.markdown("""
                <style>
                .loading-overlay {
                    position: fixed;
                    top: 0;
                    left: 0;
                    width: 100vw;
                    height: 100vh;
                    background-color: rgba(0, 0, 0, 0.7);
                    display: flex;
                    justify-content: center;
                    align-items: center;
                    z-index: 9999;
                }
                .loading-spinner {
                    width: 50px;
                    height: 50px;
                    border: 5px solid #f3f3f3;
                    border-top: 5px solid #3498db;
                    border-radius: 50%;
                    animation: spin 1s linear infinite;
                }
                .loading-text {
                    color: white;
                    font-size: 20px;
                    margin-left: 20px;
                }
                @keyframes spin {
                    0% { transform: rotate(0deg); }
                    100% { transform: rotate(360deg); }
                }
                </style>
            """, unsafe_allow_html=True)

            def show_loading(text):
                """전체 화면 로딩 오버레이 표시"""
                loading_html = f"""
                    <div class="loading-overlay">
                        <div class="loading-spinner"></div>
                        <div class="loading-text">{text}</div>
                    </div>
                """
                loading_placeholder = st.empty()
                loading_placeholder.markdown(loading_html, unsafe_allow_html=True)
                return loading_placeholder

            # 메인 섹션
            st.title("내신쌀먹-영어")
            ad1_code = """
<ins class="kakao_ad_area" style="display:none;"
data-ad-unit = "DAN-UmrCKsYsWAnoWb53"
data-ad-width = "728"
data-ad-height = "90"></ins>
<script type="text/javascript" src="//t1.daumcdn.net/kas/static/ba.min.js" async></script>
            """
            components.html(ad1_code)
            # 저장된 지문 표시
            saved_passages = db.get_passages()
            if saved_passages:
                st.subheader("저장된 지문 목록")
                for passage in saved_passages:
                    with st.expander(passage['title'], expanded=False):
                        st.text_area("지문 내용", passage['text'], height=200, key=f"saved_passage_{passage['id']}", disabled=True)
                        if st.button("문제 생성", key=f"gen_btn_{passage['id']}"):
                            process_passage_and_generate_questions(passage['text'], db, passage['id'], passage['title'])
            else:
                st.info("저장된 지문이 없습니다. PDF를 업로드하여 지문을 추가해주세요.")

            st.divider()

            # PDF 업로드 섹션
            st.subheader("PDF 업로드")
            add5_code = """<ins class="kakao_ad_area" style="display:none;"
data-ad-unit = "DAN-9Wx3II9iVdFfwL95"
data-ad-width = "320"
data-ad-height = "100"></ins>
<script type="text/javascript" src="//t1.daumcdn.net/kas/static/ba.min.js" async></script>"""
            components.html(add5_code)
            uploaded_file = st.file_uploader("PDF 파일을 업로드하세요", type="pdf")
            if uploaded_file:
                # 광고 체크
                if not ad_manager.show_ad():
                    st.warning("PDF를 처리하시려면 광고를 시청해 주세요.")
                    return

                loading_overlay = show_loading("PDF 파일을 분석하고 있습니다...")
                try:
                    parser = PDFParser()
                    passages = parser.process_pdf(uploaded_file)
                    loading_overlay.empty()

                    for idx, passage_text in enumerate(passages):
                        with st.expander(f"지문 {idx + 1}", expanded=True):
                            col1, col2 = st.columns([3, 1])
                            with col1:
                                title = st.text_input(
                                    "지문 제목",
                                    key=f"pdf_title_{idx}",
                                    placeholder="지문의 제목을 입력하세요"
                                )
                                st.text_area("지문 내용", passage_text, height=200, key=f"pdf_text_{idx}")
                            with col2:
                                if st.button("저장", key=f"save_btn_{idx}"):
                                    if title:
                                        loading_overlay = show_loading("지문을 저장하고 있습니다...")
                                        try:
                                            passage_id = db.save_passage(title, passage_text)
                                            loading_overlay.empty()
                                            st.success("저장되었습니다!")
                                            st.rerun()
                                        except Exception as e:
                                            loading_overlay.empty()
                                            st.error(f"저장 중 오류가 발생했습니다: {str(e)}")
                                    else:
                                        st.warning("제목을 입력해주세요.")

                        if st.button("문제 생성", key=f"pdf_gen_btn_{idx}"):
                            if title:
                                loading_overlay = show_loading("문제를 생성하고 있습니다...")
                                try:
                                    process_passage_and_generate_questions(passage_text, db, title=title)
                                finally:
                                    loading_overlay.empty()
                            else:
                                st.warning("제목을 입력해주세요.")
                except Exception as e:
                    loading_overlay.empty()
                    st.error(f"PDF 파일 분석 중 오류가 발생했습니다: {str(e)}")

            # 직접 입력 섹션
            st.subheader("지문 직접 입력")
            add4_code = """<ins class="kakao_ad_area" style="display:none;"
data-ad-unit = "DAN-S960Z5NosbztZSNN"
data-ad-width = "320"
data-ad-height = "100"></ins>
<script type="text/javascript" src="//t1.daumcdn.net/kas/static/ba.min.js" async></script>"""
            components.html(add4_code)
            manual_passage = st.text_area("영어 지문을 입력하세요", height=200)
            if manual_passage:
                title = st.text_input("지문 제목", key="manual_title", placeholder="지문의 제목을 입력하세요")
                col1, col2 = st.columns([3, 1])
                with col1:
                    ad3_code = """<ins class="kakao_ad_area" style="display:none;"
data-ad-unit = "DAN-XySQCAsnL6ZYVeZC"
data-ad-width = "300"
data-ad-height = "250"></ins>
<script type="text/javascript" src="//t1.daumcdn.net/kas/static/ba.min.js" async></script>"""
                    components.html(ad3_code)
                    st.subheader(f"문제 생성")
                    st.write(manual_passage)

                    # 선택지 표시
                    selected_answer = st.radio(
                        "정답을 선택하세요:",
                        [f"{i+1}. {choice}" for i, choice in enumerate([""])]
                    )

                    # 정답 확인 버튼
                    if not st.button("정답 확인"):
                        st.warning("정답을 선택해주세요.")

                    # 정답과 해설 표시
                    st.write(f"정답: ")
                    st.write("해설:")
                    st.write("")

                    # 이전/다음 문제 버튼
                    btn_col1, btn_col2 = st.columns(2)
                    with btn_col1:
                        if st.button("이전 문제"):
                            st.warning("이전 문제가 없습니다.")

                    with btn_col2:
                        if st.button("다음 문제"):
                            st.warning("다음 문제가 없습니다.")

        with mode_tabs[1]:
            st.title("학습 모드")

            if 'current_question_idx' not in st.session_state:
                st.session_state.current_question_idx = 0
            if 'show_answer' not in st.session_state:
                st.session_state.show_answer = False
            if 'show_translation' not in st.session_state:
                st.session_state.show_translation = False
            if 'practice_mode' not in st.session_state:
                st.session_state.practice_mode = False

            # 지문 선택
            saved_passages = db.get_passages()
            if not saved_passages:
                st.info("저장된 지문이 없습니다. 편집 모드에서 지문을 추가해주세요.")
                return

            passage_options = {p['title']: p for p in saved_passages}
            selected_title = st.selectbox("학습할 지문을 선택하세요", options=list(passage_options.keys()))

            if selected_title:
                selected_passage = passage_options[selected_title]
                questions = db.get_questions_for_passage(selected_passage['id'])

                if not questions:
                    st.warning("이 지문에 대한 문제가 아직 생성되지 않았습니다.")
                    return

                # 현재 문제 표시
                current_q = questions[st.session_state.current_question_idx]

                # 문제 번호 표시
                st.write(f"문제 {st.session_state.current_question_idx + 1}/{len(questions)}")

                # 영어 본문 표시
                if current_q.original_text is not None:
                    st.markdown("### 지문")
                    st.markdown(current_q.original_text)
                    st.markdown("---")

                # 문제 내용 표시
                st.markdown(f"**Q. {current_q.question}**")

                # 보기 표시
                choices = current_q.choices
                col1, col2 = st.columns(2)

                with col1:
                    for i, choice in enumerate(choices, 1):
                        if st.button(f"{i}. {choice}", key=f"choice_{i}"):
                            if i == current_q.correct_answer:
                                st.success("정답입니다! ")
                                st.session_state.show_answer = True
                            else:
                                st.error("틀렸습니다. ")
                                st.session_state.show_translation = True
                                st.session_state.practice_mode = True

                with col2:
                    # 한국어 해설 및 필사 모드
                    if st.session_state.show_translation:
                        st.markdown("### 한국어 해설")
                        if current_q.korean_translation is not None:
                            st.write(current_q.korean_translation)

                        if st.session_state.practice_mode:
                            st.markdown("### 필사 연습")
                            user_input = st.text_area("정답을 필사해보세요:", key="transcription")
                            if st.button("확인"):
                                if user_input.strip() == choices[current_q.correct_answer-1].strip():
                                    st.success("잘했습니다! 다음 문제로 넘어갈 수 있습니다.")
                                else:
                                    st.error("정확하게 필사해주세요.")

                # 정답 설명
                if st.session_state.show_answer:
                    st.markdown("### 정답 설명")
                    st.write(current_q.explanation)

                # 네비게이션 버튼
                col1, col2, col3 = st.columns(3)

                with col1:
                    if st.session_state.current_question_idx > 0:
                        if st.button("이전 문제"):
                            st.session_state.current_question_idx -= 1
                            st.session_state.show_answer = False
                            st.session_state.show_translation = False
                            st.session_state.practice_mode = False
                            st.rerun()

                with col2:
                    if st.button("정답 보기"):
                        st.session_state.show_answer = True

                with col3:
                    if st.session_state.current_question_idx < len(questions) - 1:
                        if st.button("다음 문제"):
                            st.session_state.current_question_idx += 1
                            st.session_state.show_answer = False
                            st.session_state.show_translation = False
                            st.session_state.practice_mode = False
                            st.rerun()

    else:
        st.info("로그인하여 서비스를 이용해주세요.")

if __name__ == "__main__":
    main()

    # Footer with contact and license information
    st.markdown("---")
    st.markdown("""
    ### 문의 및 라이선스
    - **문의전화**: 010-9493-6576
    - **오픈소스 라이선스**: [라이선스 정보](https://github.com/ENVERLEE/nsssalmuk_eng/blob/main/LICENSES.md)
    """)
