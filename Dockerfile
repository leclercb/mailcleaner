FROM registry.access.redhat.com/ubi9/ubi-minimal:9.5 AS build
RUN microdnf update -y && microdnf install -y curl java-17-openjdk-devel unzip && microdnf clean all
WORKDIR /app
COPY . .
RUN ./gradlew build -Dquarkus.native.enabled=true

FROM quay.io/quarkus/ubi9-quarkus-micro-image:2.0
WORKDIR /work
COPY --from=build /app/target/*-runner /work/application
CMD ["./application"]
