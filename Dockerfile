FROM maven:3.9.4-amazoncorretto-11 AS build

WORKDIR /app
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn clean package -Dmaven.test.skip  && rm -r target

COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn package -Dmaven.test.skip

FROM eclipse-temurin:11.0.20.1_1-jre-alpine
WORKDIR /app
COPY --from=build /app/target/htmx-table-test.jar .
COPY --from=build /app/target/libs/* ./libs/

ENTRYPOINT ["java" ,"-jar","htmx-table-test.jar"]
