# 기본 설정
COMPOSE = docker compose

# 빌드 + 실행
up:
	$(COMPOSE) up --build

# 백그라운드 실행
up-d:
	$(COMPOSE) up --build -d

# 종료
down:
	$(COMPOSE) down

# 로그 보기
logs:
	$(COMPOSE) logs -f

# 재시작
re:
	$(COMPOSE) down
	$(COMPOSE) up --build -d

# 이미지까지 전부 삭제 (fclean)
fclean:
	$(COMPOSE) down --rmi all --volumes --remove-orphans

# 캐시 없이 빌드
build:
	$(COMPOSE) build --no-cache