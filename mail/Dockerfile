# Étape 1 : Build (Compilation dyal l'JAR avec Java 21)
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Étape 2 : Production (Lancement dyal l'App avec JRE 21 Alpine pour 0% Poids)
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
# Kan-jib l'JAR mn l'étape llowla
COPY --from=build /app/target/*.jar app.jar
EXPOSE 4778
ENTRYPOINT ["java", "-jar", "app.jar"]