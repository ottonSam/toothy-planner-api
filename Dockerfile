FROM eclipse-temurin:25-jdk AS build

WORKDIR /app

COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw dependency:go-offline -DskipTests

COPY src src
RUN ./mvnw clean package -DskipTests

FROM eclipse-temurin:25-jre

WORKDIR /app

RUN groupadd --system spring && useradd --system --gid spring spring

COPY --from=build --chown=spring:spring /app/target/*.jar app.jar

USER spring

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
