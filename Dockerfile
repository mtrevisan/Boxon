FROM openjdk:latest

WORKDIR /app

COPY .mvn/ .mvn
COPY .mvn/mvnw pom.xml ./
COPY src ./src

CMD [".mvn/mvnw", "test"]
