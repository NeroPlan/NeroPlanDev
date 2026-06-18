# builder
FROM gradle:8-jdk17 AS builder

WORKDIR /app

COPY . .

RUN gradle clean build -x test

# runner
FROM eclipse-temurin:17-jdk

WORKDIR /app

COPY --from=builder /app/build/libs/backendUser-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]