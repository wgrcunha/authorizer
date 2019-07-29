FROM openjdk:8-alpine

COPY target/uberjar/authorizer.jar /authorizer/app.jar

EXPOSE 3000

CMD ["java", "-jar", "/authorizer/app.jar"]
