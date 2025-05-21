FROM registry.access.redhat.com/ubi9/ubi-minimal:9.5 AS build
WORKDIR /app
COPY . .
RUN ./mvnw package -Dnative

FROM quay.io/quarkus/ubi9-quarkus-micro-image:2.0
WORKDIR /work/
COPY --from=build /app/target/*-runner /work/application
CMD ["./application"]
