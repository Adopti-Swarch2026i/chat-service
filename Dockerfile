# Usar imagen base optimizada
FROM eclipse-temurin:17-jdk-alpine

# Directorio de trabajo
WORKDIR /app

# Copiar el compilado usando el target por defecto de de la build de Maven
COPY target/chat-0.0.1-SNAPSHOT.jar app.jar

# Exponer el puerto del microservicio (8081)
EXPOSE 8081

# Ejecutar el microservicio
ENTRYPOINT ["java", "-jar", "app.jar"]
