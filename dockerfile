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
COPY ~/simplex.der /tmp/simplex.der
COPY ~/.oci/config ~/.oci/config
COPY ~/.oci/oci.pem ~/.oci/oci.pem
COPY --from=builder /app/build/libs/etfBatch.jar build/libs/etfBatch.jar

RUN keytool -importcert \
    -file /tmp/simplex.der \
    -alias custom-ca-alias \
    -keystore $JAVA_HOME/lib/security/cacerts \
    -storepass changeit \
    -noprompt

EXPOSE 7777
CMD ["java", "-server", "-XX:TieredStopAtLevel=1", "-jar", "build/libs/etfBatch.jar"]