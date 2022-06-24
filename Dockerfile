FROM openjdk:latest

WORKDIR /app

COPY .mvn/ .mvn
COPY pom.xml ./
COPY src ./src

CMD [".mvn/mvnw", "test"]
