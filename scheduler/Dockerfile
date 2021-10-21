FROM openjdk:8-jdk-alpine
#INSTALL CURL FOR ACTUATOR
RUN apk --no-cache add curl
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} app.jar
ENTRYPOINT ["java","-jar","/app.jar"]