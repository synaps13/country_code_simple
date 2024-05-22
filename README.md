# Getting Started

### Requirements

* Java 21
* Internet connection (for downloading dependencies, application can also work offline)
* Docker
* Docker Compose

### Docker Compose support
This project contains a Docker Compose file named `compose.yaml`.
In this file, the following services have been defined:

* postgres: [`postgres:latest`](https://hub.docker.com/_/postgres)

It will start a PostgreSQL database that will be usable by the application and no other configuration than that will be needed.

### Testcontainers support

This project uses [Testcontainers at development time](https://docs.spring.io/spring-boot/docs/3.2.5/reference/html/features.html#features.testing.testcontainers.at-development-time).

CccApplicationTests is a test class that demonstrates how to use Testcontainers in a Spring Boot application testing.

### Extra notes

* The application will start on port 8088
* Migrations (there is only one) will be executed automatically on application startup by Flyway
* The application front-end will be available at `http://localhost:8088/home.html`
* API call will be performed to `GET /code` endpoint with query parameter `phoneNumber`, so feel free to test it out separately.
* Phone number input is limited from 3 to 50 digits. I don't really know what kind of weird or strange numbers may exist. So feel free to adjust upper end of limit if REALLY needed.
* Application is intended to be built and run with gralde wrapper. So, no need to install gradle on your machine.
* This was pretty much my first time using reactive stack for Spring Boot. It looks very similar to Quarkus reactive. Takes a lot from functional languages, so I definitely will be using it more in the future.

# Launching developer setup

    ./gradlew bootRun

Starts the application on port 8088, sets up the database using docker composer and runs the migration.

# Running tests

    ./gradlew test

Runs the tests and also produces a test report in `build/reports/tests/test/index.html` and on terminal.