FROM openjdk:latest

WORKDIR /app

COPY .mvn/ .mvn
COPY .mvn/mvnw pom.xml ./
COPY src ./src

RUN .mvn/mvnw dependency:go-offline

CMD [".mvn/mvnw", "test"]
