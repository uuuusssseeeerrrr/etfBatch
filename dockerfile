# 빌드 스테이지
FROM amazoncorretto:21-alpine AS builder
WORKDIR /app
COPY . .
RUN chmod +x gradlew
RUN ./gradlew shadowJar --no-daemon

# 실행 스테이지
FROM amazoncorretto:21-alpine
WORKDIR /app
COPY --from=builder /app/build/libs/app.jar build/libs/app.jar
EXPOSE 7777

CMD ["/bin/sh", "-c", "\
  export DB_URL=$(cat /run/secrets/DB_URL) && \
  export DB_USERNAME=$(cat /run/secrets/DB_USERNAME) && \
  export DB_PASSWORD=$(cat /run/secrets/DB_PASSWORD) && \
  export KIS_KEY=$(cat /run/secrets/KIS_KEY) && \
  export KIS_SECRET=$(cat /run/secrets/KIS_SECRET) && \
  export BATCHTOKEN=$(cat /run/secrets/BATCHTOKEN) && \
  exec java -server -XX:TieredStopAtLevel=1 -jar build/libs/app.jar -port=7777 \
"]