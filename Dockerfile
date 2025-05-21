FROM quay.io/quarkus/ubi9-quarkus-micro-image:2.0 AS build
WORKDIR /app
COPY . .
RUN ./mvnw package -Dnative

FROM quay.io/quarkus/ubi9-quarkus-micro-image:2.0
WORKDIR /work/
COPY --from=build /app/target/*-runner /work/application
CMD ["./application"]
