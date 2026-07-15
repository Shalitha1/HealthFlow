# ── Stage 1: Build ────────────────────────────────────────────────────────────
# Use the official Maven image with Java 21 to compile and package the app
FROM maven:3.9.6-eclipse-temurin-21 AS build

WORKDIR /app

# Copy pom.xml first and download dependencies as a separate layer
# This is a cache optimisation — if pom.xml hasn't changed, Docker reuses
# this layer and skips the slow dependency download on every rebuild
COPY pom.xml .

# Now copy source code and build the jar
COPY src ./src
RUN mvn clean package -DskipTests -B

# ── Stage 2: Run ──────────────────────────────────────────────────────────────
# Use a smaller JRE-only image for the final container — no Maven, no source
# eclipse-temurin:21-jre-alpine is ~200MB vs the build image at ~700MB
FROM eclipse-temurin:21-jre-alpine

RUN apk add --no-cache curl

WORKDIR /app

# Copy only the built jar from the build stage
COPY --from=build /app/target/*.jar app.jar

# Document that the container listens on port 8080
# This does NOT actually expose the port — docker-compose does that
EXPOSE 8080

# The command that runs when the container starts
ENTRYPOINT ["java", "-jar", "app.jar"]
