FROM eclipse-temurin:25-jdk-alpine AS builder


WORKDIR /app

COPY pom.xml ./pom.xml
COPY services/api-gateway/.mvn/ services/api-gateway/.mvn/
COPY services/api-gateway/mvnw services/api-gateway/pom.xml services/api-gateway/
COPY services/api-gateway/pom.xml services/api-gateway/

WORKDIR /app/services/api-gateway

RUN chmod +x mvnw
RUN ./mvnw dependency:go-offline

COPY services/api-gateway/src ./src
RUN ./mvnw package -DskipTests


FROM eclipse-temurin:25-jre-alpine AS final-stage
LABEL authors="pmish"

WORKDIR /app

EXPOSE 8080

COPY --from=builder /app/services/api-gateway/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]