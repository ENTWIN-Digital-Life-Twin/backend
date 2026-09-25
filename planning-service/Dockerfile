FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY pom.xml .
COPY common/pom.xml common/pom.xml
COPY auth-service/pom.xml auth-service/pom.xml
COPY planning-service/pom.xml planning-service/pom.xml
COPY wellness-service/pom.xml wellness-service/pom.xml
COPY notification-service/pom.xml notification-service/pom.xml
COPY api-gateway/pom.xml api-gateway/pom.xml
COPY ai-service/pom.xml ai-service/pom.xml

COPY common/src common/src
COPY planning-service/src planning-service/src

RUN mvn -pl planning-service -am package -DskipTests -B

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S app && adduser -S app -G app
COPY --from=build /workspace/planning-service/target/planning-service-*.jar /app/app.jar
USER app

EXPOSE 8082
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
