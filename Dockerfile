FROM gradle:8.14.0-jdk21-corretto AS build
WORKDIR /build
COPY . .
RUN ./gradlew build --no-daemon

FROM amazoncorretto:21
WORKDIR /app
COPY --from=build /build/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar", "-Dquarkus.http.host=0.0.0.0", "-Dquarkus.config.locations=file:/config/config.yml"]