# syntax = docker/dockerfile:1.2
# -----------------------------
# Étape 1 : build avec JDK 24
# -----------------------------
# Pour utiliser ce Dockerfile avec des secrets:
# 1. Construire l'image: docker build -t triviatreck-app .
# 2. Exécuter le conteneur avec le secret monté:
#    docker run --rm -p 8080:8080 --mount=type=secret,id=secret.properties,dst=/etc/secrets/secret.properties triviatreck-app
FROM openjdk:24-jdk-slim AS builder
# Installer Maven dans l'image de build
RUN apt-get update \
 && apt-get install -y --no-install-recommends maven \
 && rm -rf /var/lib/apt/lists/* \


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

# Créer le répertoire pour les secrets
RUN mkdir -p /etc/secrets

# Récupérer le JAR issu du build
COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080

# Utiliser le secret monté lors de l'exécution
CMD ["java", "-jar", "app.jar", "--spring.config.additional-location=file:/etc/secrets/secret.properties"]
