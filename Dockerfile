# -----------------------------
# Étape 1 : build avec JDK 24
# -----------------------------
FROM openjdk:24-jdk-slim AS builder
# Installer Maven dans l'image de build
RUN apt-get update \
 && apt-get install -y --no-install-recommends maven \
 && rm -rf /var/lib/apt/lists/*

WORKDIR /app
# Précharger les dépendances pour accélérer le rebuild
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copier le code et compiler
COPY src ./src
RUN mvn clean package -DskipTests

# --------------------------------
# Étape 2 : runtime avec JRE 24
# --------------------------------
FROM openjdk:24-jdk-slim AS runtime
WORKDIR /app

# Récupérer le JAR issu du build
COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080
CMD ["java", "-jar", "app.jar"]