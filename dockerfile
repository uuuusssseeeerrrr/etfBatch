# 빌드 스테이지
FROM amazoncorretto:21-alpine AS builder
WORKDIR /app
COPY . .
RUN chmod +x gradlew
RUN ./gradlew shadowJar --no-daemon

# 실행 스테이지
FROM amazoncorretto:21-alpine
RUN apk add --no-cache tzdata

WORKDIR /app
COPY --from=builder /app/build/libs/etfBatch.jar build/libs/etfBatch.jar

COPY entrypoint.sh /app/entrypoint.sh
RUN chmod +x /app/entrypoint.sh
EXPOSE 7777
ENTRYPOINT ["/app/entrypoint.sh"]