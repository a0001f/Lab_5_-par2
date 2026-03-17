# ==========================================
# Stage 1: Build
# ==========================================
FROM maven:3.9-eclipse-temurin-21 AS build

#ARG MAVEN_OPTS="-XX:+TieredCompilation -XX:TieredStopAtLevel=1"
#WORKDIR .

COPY pom.xml .
RUN mvn dependency:go-offline -B

# Build
COPY src ./src
RUN mvn clean package -DskipTests

# ==========================================
# Stage 2: Runtime
# ==========================================
FROM eclipse-temurin:21-jre

LABEL description="TodoList REST API"
LABEL java.version="21"

WORKDIR /app

# Security: non-root user
RUN groupadd -r javalin && useradd -r -g javalin javalin

# Copiar o JAR compilado e os recursos estáticos
COPY --from=build target/taskslist-phase1-2-1.0-SNAPSHOT-jar-with-dependencies.jar app.jar
COPY --from=build src/main/resources/public ./src/main/resources/public

# Ownership
RUN chown -R javalin:javalin /app
USER javalin

# Executar
ENTRYPOINT ["sh", "-c", "java -jar app.jar"]