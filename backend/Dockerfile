# builder
FROM gradle:8-jdk17 AS builder
WORKDIR /app
COPY . .
RUN gradle build

# runner
# FROM openjdk
FROM eclipse-temurin:17-jdk
WORKDIR /app
COPY --from=builder /app/build/libs/neroplan-0.0.1-SNAPSHOT.jar app.jar
CMD ["java", "-jar", "app.jar"]