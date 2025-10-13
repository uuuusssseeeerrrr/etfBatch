FROM amazoncorretto:21-alpine AS builder
WORKDIR /app
COPY . .
RUN chmod +x gradlew
RUN ./gradlew bootJar
EXPOSE 7777
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
