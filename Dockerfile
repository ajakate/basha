FROM openjdk:8-alpine

COPY target/uberjar/basha.jar /basha/app.jar

EXPOSE 3000

CMD ["java", "-jar", "/basha/app.jar"]
