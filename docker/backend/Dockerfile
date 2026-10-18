# Build stage: compile and package the backend.
FROM eclipse-temurin:21.0.12.1_1-jdk-noble AS build
WORKDIR /src
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw -B -q dependency:go-offline
COPY src/ src/
RUN ./mvnw -B -q package -DskipTests

# Run stage: small runtime image, non-root user.
FROM eclipse-temurin:21.0.12.1_1-jre-noble
WORKDIR /app
RUN useradd --system --uid 10001 app
COPY --from=build /src/target/backend-*.jar app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
