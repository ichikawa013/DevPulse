FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

COPY pom.xml ./pom.xml
COPY services/notification-service/.mvn/ services/notification-service/.mvn/
COPY services/notification-service/mvnw services/notification-service/pom.xml services/notification-service/
COPY services/notification-service/pom.xml services/notification-service/

WORKDIR /app/services/notification-service

RUN chmod +x mvnw
RUN ./mvnw dependency:go-offline

COPY services/notification-service/src ./src
RUN ./mvnw package -DskipTests


FROM eclipse-temurin:21-jre-alpine AS final-stage
LABEL authors="pmish"

WORKDIR /app

EXPOSE 8083

COPY --from=builder /app/services/notification-service/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]