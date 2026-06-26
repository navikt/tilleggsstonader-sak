FROM europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/jre:openjdk-21@sha256:f901ce6dd7f0ff565030b0c7221e7e019d4ccf6b0f6dd2abd72e144c6bbd69b1

COPY --chown=1069:1069 build/libs/app.jar /app.jar

EXPOSE 8080

CMD ["-jar", "/app.jar"]
