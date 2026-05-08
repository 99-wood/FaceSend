FROM openjdk:21-jdk-slim
WORKDIR /app

COPY facesend-server.jar app.jar

ENV FACE_SEND_PROFILE=docker

ENTRYPOINT ["java", "-Dspring.profiles.active=docker", "-jar", "app.jar"]
