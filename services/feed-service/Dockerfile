FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

COPY pom.xml ./pom.xml
COPY services/feed-service/.mvn/ services/feed-service/.mvn/
COPY services/feed-service/mvnw services/feed-service/pom.xml services/feed-service/
COPY services/feed-service/pom.xml services/feed-service/

WORKDIR /app/services/feed-service

RUN chmod +x mvnw
RUN ./mvnw dependency:go-offline

COPY services/feed-service/src ./src
RUN ./mvnw package -DskipTests


FROM eclipse-temurin:21-jre-alpine AS final-stage
LABEL authors="pmish"

WORKDIR /app

EXPOSE 8082

COPY --from=builder /app/services/feed-service/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]