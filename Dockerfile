###############################
# Stage 1 — Build backend + dashboard
###############################
FROM eclipse-temurin:17-jdk-jammy AS build

WORKDIR /app

# Install Maven & Node (required for frontend-maven-plugin)
RUN apt-get update && \
    apt-get install -y maven curl && \
    curl -fsSL https://deb.nodesource.com/setup_18.x | bash - && \
    apt-get install -y nodejs

# Copy ENTIRE project (backend + dashboard)
COPY . .

# Move into backend folder because pom.xml is there
WORKDIR /app/backend

# Build backend (frontend plugin will execute /dashboard)
RUN mvn -DskipTests package


###############################
# Stage 2 — Runtime (final image)
###############################
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

# Copy built Spring Boot JAR from backend target directory
COPY --from=build /app/backend/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]