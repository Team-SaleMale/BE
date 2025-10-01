# Multi-stage build for Spring Boot application
FROM openjdk:17-jdk-slim AS builder

# Set working directory
WORKDIR /app

# Copy gradle files
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

# Make gradlew executable
RUN chmod +x ./gradlew

# Copy source code
COPY src src

# Build the application
RUN ./gradlew build -x test

# Final stage
# 더 가벼운 런타임 전용 JRE 이미지를 사용 (공식 temurin)
FROM eclipse-temurin:17-jre

# Set working directory
WORKDIR /app

# 비루트 사용자 생성 (보안 강화)
RUN addgroup --system spring && adduser --system --group spring

# Healthcheck를 위해 curl 설치 (비루트 전환 전 설치)
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

# Copy the built jar from builder stage (boot JAR만 복사)
# 예) app-0.0.1-SNAPSHOT.jar
COPY --from=builder /app/build/libs/*-SNAPSHOT.jar app.jar

# 권한 변경 후 비루트 사용자로 전환
RUN chown -R spring:spring /app
USER spring

# Expose port
EXPOSE 8080

# 실행 프로파일 설정 (필요한 환경변수는 컨테이너 실행 시 주입)
ENV SPRING_PROFILES_ACTIVE=prod

# Healthcheck (Spring Actuator 사용 시 권장)
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD curl -fsS http://localhost:8080/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
