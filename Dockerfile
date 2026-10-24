FROM europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/jre:openjdk-21@sha256:c98e1c3f3d4a1db634ef62f8c564e6541325b9635abeeb1cbe8cf50c7db6fdd1

COPY --chown=1069:1069 build/libs/app.jar /app.jar

EXPOSE 8080

CMD ["-jar", "/app.jar"]
