# Etapa 1: Construcción (Build) con Java 21
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

# Copiar el pom.xml y descargar las dependencias para aprovechar la caché de Docker
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copiar el código fuente y compilar el archivo JAR saltándose los tests
COPY src ./src
RUN mvn clean package -DskipTests

# Etapa 2: Imagen de Ejecución (Runtime) con Java 21
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copiar el archivo JAR generado desde la etapa de construcción
COPY --from=build /app/target/*.jar app.jar

# Crear el punto de montaje para el almacenamiento temporal EFS que pide la pauta
RUN mkdir -p /mnt/efs/temporal

# Exponer el puerto por defecto de Spring Boot
EXPOSE 8080

# Comando para ejecutar la aplicación
ENTRYPOINT ["java", "-jar", "app.jar"]