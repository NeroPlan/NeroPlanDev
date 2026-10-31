FROM python:3.12-slim
WORKDIR /app

COPY requirements-api.txt .
RUN pip install --no-cache-dir -r requirements-api.txt

COPY shared ./shared
COPY plan_classifier ./plan_classifier
COPY planning_fallacy_detector ./planning_fallacy_detector
COPY success_rate_scheduler ./success_rate_scheduler
COPY planner_api.py .

EXPOSE 8000
# Railway는 PORT 환경변수를 주입하므로 있으면 그 포트 사용
CMD ["sh", "-c", "uvicorn planner_api:app --host 0.0.0.0 --port ${PORT:-8000}"]
