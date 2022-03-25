# Build
FROM maven:3.8.4-openjdk-8 AS build-env
WORKDIR /app
COPY pom.xml ./
RUN mvn dependency:go-offline
COPY . ./
RUN mvn package
# Run
FROM openjdk:8
WORKDIR /app
COPY --from=build-env /app/target/entropay.user-auth.jar ./entropay.user-auth.jar
CMD ["java", "-jar", "/app/entropay.user-auth.jar", "-Xmx256m"]