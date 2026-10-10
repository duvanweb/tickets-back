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

# Copiar el JAR construido en la stage anterior
COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
