# 1단계: 빌드 이미지 설정
FROM gradle:8-jdk21-alpine AS build
WORKDIR /workspace
COPY . .
RUN gradle bootJar --no-daemon

# 2단계: 런타임 이미지 설정
# 작업 디렉토리 설정
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# 로그 디렉토리 생성
RUN mkdir -p /app/logs

# 빌드 단계에서 JAR 파일 복사
COPY --from=build /workspace/build/libs/*.jar app.jar

# JVM 최적화 설정
ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC -XX:+UseStringDeduplication"

# 포트 노출
EXPOSE 8080

# Health check 추가
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/health || exit 1

# 애플리케이션 실행
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar -Dspring.profiles.active=dev app.jar"]