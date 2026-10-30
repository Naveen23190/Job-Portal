
# Stage 1: Build
FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /app

COPY pom.xml .

RUN mvn dependency:go-offline

COPY src ./src

RUN mvn clean package -DskipTests

# Verify and copy generated executable JAR
RUN set -eux; \
    find /app/target -maxdepth 1 -type f -name "*.jar" -print; \
    JAR=$(find /app/target -maxdepth 1 -type f -name "*.jar" \
      ! -name "*.jar.original" ! -name "*-plain.jar" | head -n 1); \
    test -n "$JAR"; \
    cp "$JAR" /app/app.jar

# Stage 2: Run
FROM eclipse-temurin:17-jre

WORKDIR /app

COPY --from=build /app/app.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
