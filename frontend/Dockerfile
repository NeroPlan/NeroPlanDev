# 1️⃣ 빌드
FROM node:22-alpine AS builder
WORKDIR /app 
#여기를 루트로 두고 package.json을 찾아서 실행함 그래서 아래 두 번째 카피경로가 저렇다.

COPY ./my-app .
# COPY ./my-app/package*.json ./
RUN npm install

# Tailwind 설치
RUN npm run build

#  실행 (nginx)
FROM nginx:latest

COPY --from=builder /app/dist /usr/share/nginx/html
RUN ls -R

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]