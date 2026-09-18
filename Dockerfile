# Wiki Collection Backend — imagen para Render (Docker runtime)
# Build multi-stage: compila con Maven + JDK 25 y ejecuta con JRE 25 ligero.
# Sin wrapper mvnw en el repo, se usa la imagen oficial de Maven.

FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /app

# Cachea dependencias antes de copiar el código
COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B clean package -DskipTests

FROM eclipse-temurin:25-jre-alpine
WORKDIR /app

COPY --from=build /app/target/wiki-collection-backend-*.jar app.jar

# Render inyecta PORT; en local usa 8080
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dserver.port=${PORT:-8080} -jar app.jar"]
