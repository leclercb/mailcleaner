FROM gradle:8.14.0-jdk21-corretto AS build
WORKDIR /build
COPY . .
RUN ./gradlew build --no-daemon

FROM amazoncorretto:21
WORKDIR /app
COPY --from=build /build/build/quarkus-app /app
EXPOSE 8080
ENTRYPOINT sh -c "ls -la /config && java -Dquarkus.http.host=0.0.0.0 -Dquarkus.config.locations=file:/config/config.yml -jar quarkus-run.jar"