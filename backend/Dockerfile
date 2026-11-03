# Backend en contenedor (T-09). Lo usa `docker compose --profile app up`; para desarrollar se sigue usando el IDE.
# La configuración (base, JWT, código de activación...) llega por variables de entorno desde backend/.env:
# la imagen no lleva ningún dato sensible.

# Etapa 1: compilar el .jar con Maven (sin tests: se corren antes, en el desarrollo y en cada PR)
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
# Primero solo el pom: si no cambian las dependencias, Docker reutiliza esta capa y no las vuelve a descargar
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q package -DskipTests

# Etapa 2: imagen final, solo con Java para ejecutar (sin Maven ni el código fuente)
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/accesos-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
