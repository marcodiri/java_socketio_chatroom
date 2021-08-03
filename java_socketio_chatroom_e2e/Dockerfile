FROM openjdk:8

COPY target/extra-resources/server-app/*jar-with-dependencies.jar /app/server.jar

CMD ["java", "-jar", "/app/server.jar", "mongodb"]