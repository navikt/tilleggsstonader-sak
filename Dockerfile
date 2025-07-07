FROM gcr.io/distroless/java21:nonroot

EXPOSE 8080
COPY build/libs/app.jar /app.jar

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75"

ENTRYPOINT ["java", "-jar", "/app.jar"]