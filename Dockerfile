# -----------------------------
# Étape 1 : build
# -----------------------------
FROM eclipse-temurin:24-jdk AS builder

# Installation de Maven
RUN apt-get update && apt-get install -y maven && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Précharger les dépendances
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copier le code source
COPY src ./src

# Compiler le projet
RUN mvn clean package -DskipTests

# Trouver le JAR et le renommer proprement
RUN cp target/*.jar app.jar

# --------------------------------
# Étape 2 : runtime avec JRE 24
# --------------------------------
FROM eclipse-temurin:24-jre AS runtime
WORKDIR /app

# Créer le répertoire pour les secrets
RUN mkdir -p /etc/secrets

# Récupérer le JAR depuis le builder
COPY --from=builder /app/app.jar app.jar

# Port par défaut de Render
EXPOSE 8080

# Commande de lancement avec fichier de configuration additionnel
CMD ["java", "-jar", "app.jar", "--spring.config.additional-location=file:/etc/secrets/secret.properties"]