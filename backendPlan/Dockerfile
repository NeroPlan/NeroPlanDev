# builder
FROM gradle:8-jdk17 AS builder
WORKDIR /app
COPY . .
# Maven Central이 느리거나 연결이 끊겨도 실패하지 않도록 재시도·타임아웃을 늘리고 동시 다운로드 수를 줄임
ARG GRADLE_NET_OPTS="--no-daemon --max-workers=2 -Dorg.gradle.internal.repository.max.retries=10 -Dorg.gradle.internal.repository.initial.backoff=2000 -Dorg.gradle.internal.http.connectionTimeout=120000 -Dorg.gradle.internal.http.socketTimeout=120000"
RUN gradle build -x test $GRADLE_NET_OPTS
# runner
# FROM openjdk
FROM eclipse-temurin:17-jdk
WORKDIR /app
COPY --from=builder /app/build/libs/backendPlan-0.0.1-SNAPSHOT.jar app.jar
CMD ["java", "-jar", "app.jar"]