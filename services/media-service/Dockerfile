FROM eclipse-temurin:25-jdk-alpine AS builder

WORKDIR /app

COPY pom.xml ./pom.xml
COPY services/media-service/.mvn/ services/media-service/.mvn/
COPY services/media-service/mvnw services/media-service/pom.xml services/media-service/
COPY services/media-service/pom.xml services/media-service/

WORKDIR /app/services/media-service

RUN chmod +x mvnw
RUN ./mvnw dependency:go-offline

COPY services/media-service/src ./src
RUN ./mvnw package -DskipTests


FROM eclipse-temurin:25-jre-alpine AS final-stage
LABEL authors="pmish"

WORKDIR /app

EXPOSE 8084

COPY --from=builder /app/services/media-service/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]