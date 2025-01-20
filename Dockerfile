# Build
FROM maven:3.9.6-amazoncorretto-21 AS build-env
WORKDIR /app
COPY pom.xml ./
RUN mvn dependency:go-offline
COPY . ./
RUN mvn package

# Run
FROM amazoncorretto:21
WORKDIR /app
COPY --from=build-env /app/target/entropay-users-auth.jar ./entropay-users-auth.jar
CMD ["java", "-jar", "/app/entropay-users-auth.jar", "-Xmx256m"]