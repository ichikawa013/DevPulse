FROM eclipse-temurin:21-jdk-alpine AS builder


WORKDIR /app

COPY pom.xml ./pom.xml
COPY services/user-service/.mvn/ services/user-service/.mvn/
COPY services/user-service/mvnw services/user-service/pom.xml services/user-service/
COPY services/user-service/pom.xml services/user-service/

WORKDIR /app/services/user-service

RUN chmod +x mvnw
RUN ./mvnw dependency:go-offline

COPY services/user-service/src ./src
RUN ./mvnw package -DskipTests -Dmaven.test.skip=true

FROM eclipse-temurin:21-jre-alpine AS final-stage
LABEL authors="pmish"

WORKDIR /app

EXPOSE 8081

COPY --from=builder /app/services/user-service/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]