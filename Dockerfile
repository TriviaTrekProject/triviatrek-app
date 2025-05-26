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
 && rm -rf /var/lib/apt/lists/*

WORKDIR /app
# Précharger les dépendances pour accélérer le rebuild
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copier le code et compiler
COPY src ./src
COPY secret.properties ./secret.properties
RUN mvn clean package -DskipTests
# Vérifier que le JAR a été créé
RUN ls -la target/ || echo "Target directory is empty or doesn't exist"
# Renommer le JAR pour faciliter la copie
RUN find target -name "*.jar" -type f -print
RUN find target -name "*.jar" -type f | head -1 | xargs -I {} cp {} target/app.jar || echo "Failed to copy JAR file"
# Vérifier que app.jar a été créé
RUN ls -la target/app.jar || echo "app.jar was not created"
# Déplacer le JAR à la racine du répertoire /app pour faciliter la copie
RUN cp target/app.jar ./app.jar || echo "Failed to copy app.jar to root directory"
RUN ls -la /app/ || echo "Root directory is empty"

# --------------------------------
# Étape 2 : runtime avec JRE 24
# --------------------------------
FROM openjdk:24-jdk-slim AS runtime
WORKDIR /app

# Créer le répertoire pour les secrets
RUN mkdir -p /etc/secrets

# Récupérer le JAR issu du build
# Copier le JAR spécifique depuis le builder
COPY --from=builder /app/app.jar app.jar

EXPOSE 8080

# Utiliser le secret monté lors de l'exécution
CMD ["java", "-jar", "app.jar", "--spring.config.additional-location=file:/etc/secrets/secret.properties"]
