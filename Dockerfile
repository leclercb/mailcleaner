FROM quay.io/quarkus/quarkus-micro-image:3.11 AS build
WORKDIR /app
COPY . .
RUN ./mvnw package -Dnative

FROM quay.io/quarkus/quarkus-micro-image:3.11
WORKDIR /work/
COPY --from=build /app/target/*-runner /work/application
CMD ["./application"]
