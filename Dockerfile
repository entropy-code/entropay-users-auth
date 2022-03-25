# Build
FROM maven:3.8.4-openjdk-17 AS build-env
WORKDIR /app
COPY pom.xml ./
RUN mvn dependency:go-offline
COPY . ./
RUN mvn package
# Run
FROM openjdk:17
WORKDIR /app
COPY --from=build-env /app/target/entropay-users-auth.jar ./entropay-users-auth.jar
CMD ["java", "-jar", "/app/entropay-users-auth.jar", "-Xmx256m"]