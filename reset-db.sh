#!/bin/bash

# 데이터베이스 초기화 스크립트
# Oracle 데이터베이스의 모든 테이블을 삭제합니다

echo "========================================="
echo "데이터베이스 초기화 시작"
echo "========================================="

# 환경 변수 확인
if [ -z "$SPRING_DATASOURCE_URL" ] || [ -z "$DB_USERNAME" ] || [ -z "$DB_PASSWORD" ]; then
    echo "❌ 오류: 환경 변수가 설정되지 않았습니다."
    echo "다음 환경 변수를 설정해주세요:"
    echo "  - SPRING_DATASOURCE_URL"
    echo "  - DB_USERNAME"
    echo "  - DB_PASSWORD"
    exit 1
fi

# URL에서 호스트, 포트, 서비스명 추출
# 예: jdbc:oracle:thin:@(description=(retry_count=20)(retry_delay=3)(address=(protocol=tcps)(port=1522)(host=adb.ap-chuncheon-1.oraclecloud.com))(connect_data=(service_name=gcb3601657097fa_learningeng_high.adb.oraclecloud.com))(security=(ssl_server_dn_match=yes)))
CONNECT_STRING=$(echo "$SPRING_DATASOURCE_URL" | sed 's/jdbc:oracle:thin:@//')

# SQL 스크립트 내용 (stdin으로 전달, CONNECT 포함)
# CONNECT 명령에서 연결 문자열을 올바르게 처리하기 위해 변수 확장 사용
SQL_SCRIPT=$(cat << SQL_EOF
SET SERVEROUTPUT ON;
SET FEEDBACK OFF;
SET VERIFY OFF;
CONNECT ${DB_USERNAME}/${DB_PASSWORD}@${CONNECT_STRING}
-- 모든 테이블 삭제 (외래키 제약조건 고려)
BEGIN
   -- 외래키 제약조건 때문에 순서대로 삭제
   BEGIN
      EXECUTE IMMEDIATE 'DROP TABLE questions CASCADE CONSTRAINTS';
      DBMS_OUTPUT.PUT_LINE('✓ questions 테이블 삭제됨');
   EXCEPTION
      WHEN OTHERS THEN
         IF SQLCODE != -942 THEN -- 테이블이 존재하지 않음
            DBMS_OUTPUT.PUT_LINE('⚠ questions 삭제 오류: ' || SQLERRM);
         END IF;
   END;

   BEGIN
      EXECUTE IMMEDIATE 'DROP TABLE passages CASCADE CONSTRAINTS';
      DBMS_OUTPUT.PUT_LINE('✓ passages 테이블 삭제됨');
   EXCEPTION
      WHEN OTHERS THEN
         IF SQLCODE != -942 THEN
            DBMS_OUTPUT.PUT_LINE('⚠ passages 삭제 오류: ' || SQLERRM);
         END IF;
   END;

   BEGIN
      EXECUTE IMMEDIATE 'DROP TABLE subscriptions CASCADE CONSTRAINTS';
      DBMS_OUTPUT.PUT_LINE('✓ subscriptions 테이블 삭제됨');
   EXCEPTION
      WHEN OTHERS THEN
         IF SQLCODE != -942 THEN
            DBMS_OUTPUT.PUT_LINE('⚠ subscriptions 삭제 오류: ' || SQLERRM);
         END IF;
   END;

   BEGIN
      EXECUTE IMMEDIATE 'DROP TABLE subscription_requests CASCADE CONSTRAINTS';
      DBMS_OUTPUT.PUT_LINE('✓ subscription_requests 테이블 삭제됨');
   EXCEPTION
      WHEN OTHERS THEN
         IF SQLCODE != -942 THEN
            DBMS_OUTPUT.PUT_LINE('⚠ subscription_requests 삭제 오류: ' || SQLERRM);
         END IF;
   END;

   BEGIN
      EXECUTE IMMEDIATE 'DROP TABLE verification_tokens CASCADE CONSTRAINTS';
      DBMS_OUTPUT.PUT_LINE('✓ verification_tokens 테이블 삭제됨');
   EXCEPTION
      WHEN OTHERS THEN
         IF SQLCODE != -942 THEN
            DBMS_OUTPUT.PUT_LINE('⚠ verification_tokens 삭제 오류: ' || SQLERRM);
         END IF;
   END;

   BEGIN
      EXECUTE IMMEDIATE 'DROP TABLE users CASCADE CONSTRAINTS';
      DBMS_OUTPUT.PUT_LINE('✓ users 테이블 삭제됨');
   EXCEPTION
      WHEN OTHERS THEN
         IF SQLCODE != -942 THEN
            DBMS_OUTPUT.PUT_LINE('⚠ users 삭제 오류: ' || SQLERRM);
         END IF;
   END;

   -- 남은 테이블이 있는지 확인
   FOR cur_rec IN (SELECT table_name FROM user_tables)
   LOOP
      BEGIN
         EXECUTE IMMEDIATE 'DROP TABLE ' || cur_rec.table_name || ' CASCADE CONSTRAINTS';
         DBMS_OUTPUT.PUT_LINE('✓ ' || cur_rec.table_name || ' 테이블 삭제됨');
      EXCEPTION
         WHEN OTHERS THEN
            DBMS_OUTPUT.PUT_LINE('⚠ ' || cur_rec.table_name || ' 삭제 오류: ' || SQLERRM);
      END;
   END LOOP;

   COMMIT;
   DBMS_OUTPUT.PUT_LINE('');
   DBMS_OUTPUT.PUT_LINE('=========================================');
   DBMS_OUTPUT.PUT_LINE('데이터베이스 초기화 완료!');
   DBMS_OUTPUT.PUT_LINE('애플리케이션을 재시작하면 테이블이 자동으로 생성됩니다.');
   DBMS_OUTPUT.PUT_LINE('=========================================');
END;
/
EXIT;
SQL_EOF
)

echo "데이터베이스 연결 중..."
echo "사용자: $DB_USERNAME"
echo ""

# sqlplus를 사용한 연결 시도 (stdin으로 SQL 전달)
if command -v sqlplus &> /dev/null; then
    echo "$SQL_SCRIPT" | sqlplus -S /nolog
    EXIT_CODE=$?
elif command -v sql &> /dev/null; then
    # Oracle Instant Client의 sql 사용
    echo "$SQL_SCRIPT" | sql -S /nolog
    EXIT_CODE=$?
else
    echo "❌ 오류: Oracle SQL 클라이언트(sqlplus 또는 sql)를 찾을 수 없습니다."
    echo ""
    echo "다음 중 하나를 설치해주세요:"
    echo "  1. Oracle Instant Client (권장)"
    echo "  2. Oracle SQL*Plus"
    echo ""
    echo "또는 application.properties에서 ddl-auto를 create-drop으로 변경하고"
    echo "애플리케이션을 재시작하는 방법도 있습니다."
    exit 1
fi

if [ $EXIT_CODE -eq 0 ]; then
    echo ""
    echo "✅ 데이터베이스 초기화가 완료되었습니다!"
    echo "이제 애플리케이션을 재시작하면 테이블이 자동으로 생성됩니다."
else
    echo ""
    echo "❌ 데이터베이스 초기화 중 오류가 발생했습니다."
    echo "환경 변수와 연결 정보를 확인해주세요."
    exit $EXIT_CODE
fi

