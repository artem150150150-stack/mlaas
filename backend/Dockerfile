FROM maven:3.9.9-eclipse-temurin-21-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S lumen && adduser -S lumen -G lumen
COPY --from=build /app/target/*.jar /app/app.jar
RUN mkdir -p /data/datasets && chown -R lumen:lumen /data
USER lumen
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
