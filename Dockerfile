# ─── Build stage ───────────────────────────────────────────
# Maven + JDK 25 para compilar el proyecto y generar el JAR
FROM maven:3.9.12-eclipse-temurin-25 AS builder
WORKDIR /app

# Copiar solo el pom.xml para cachear dependencias
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn dependency:go-offline -B

# Copiar el código fuente y empaquetar
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn package -B -DskipTests

# ─── Runtime stage ─────────────────────────────────────────
# Solo JRE 25 — imagen más pequeña, sin Maven ni JDK completo
FROM eclipse-temurin:25-jre
WORKDIR /app

# Copiar ambos JARs: API (default) y Worker (classifier)
COPY --from=builder /app/target/tickets-back-0.0.1-SNAPSHOT.jar /app/api.jar
COPY --from=builder /app/target/tickets-back-0.0.1-SNAPSHOT-worker.jar /app/worker.jar

EXPOSE 8080

# Seleccionar JAR vía CMD — docker-compose sobrescribe para el worker
ENTRYPOINT ["java", "-jar"]
CMD ["api.jar"]
