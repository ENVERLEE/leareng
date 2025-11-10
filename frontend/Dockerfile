FROM nginx:alpine

# nginx 설정 파일 복사
COPY nginx.conf /etc/nginx/conf.d/default.conf

# 프론트엔드 파일 복사
COPY . /usr/share/nginx/html

# 포트 노출
EXPOSE 80

# nginx 시작
CMD ["nginx", "-g", "daemon off;"]

