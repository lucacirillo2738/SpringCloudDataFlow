FROM openjdk:11-jre-slim
ARG JAR_FILE=target/spring-cloud-dataflow-server-2.8.1.jar
COPY ${JAR_FILE} app.jar
ENTRYPOINT ["java","-jar","/app.jar"]