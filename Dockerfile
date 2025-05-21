FROM registry.access.redhat.com/ubi9/ubi-minimal:9.5 AS build
RUN microdnf update -y && microdnf install -y java-17-openjdk-devel unzip && microdnf clean all
WORKDIR /app
COPY . .
RUN ./gradlew build -Dquarkus.native.enabled=true

FROM quay.io/quarkus/ubi9-quarkus-micro-image:2.0
WORKDIR /work
COPY --from=build /app/target/*-runner /work/application
EXPOSE 8080
CMD ["./application", "-Dquarkus.http.host=0.0.0.0", "-Dquarkus.config.locations=file:/config/application.yml"]
