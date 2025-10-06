# Usar imagen base de OpenJDK 21 con Alpine
FROM eclipse-temurin:21-jdk-alpine

# Instalar dependencias necesarias
RUN apk add --no-cache \
    bash \
    curl \
    tzdata

# Establecer zona horaria
ENV TZ=UTC

# Crear directorio de trabajo
WORKDIR /app

# Copiar archivos de configuración
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Hacer ejecutable el wrapper de Maven
RUN chmod +x ./mvnw

# Descargar dependencias (para cache de Docker)
RUN ./mvnw dependency:resolve -B

# Copiar código fuente
COPY src ./src

# Compilar la aplicación
RUN ./mvnw clean package -DskipTests

# Crear directorios necesarios
RUN mkdir -p /app/data /app/raw_users /app/processed_users /app/dlq /app/state

# Exponer puerto
EXPOSE 8080

# Variables de entorno
ENV SPRING_PROFILES_ACTIVE=docker
ENV SPRING_DATASOURCE_URL=jdbc:sqlite:/app/data/telus.db

# Comando de inicio
CMD ["java", "-jar", "target/telus-0.0.1-SNAPSHOT.jar"]
