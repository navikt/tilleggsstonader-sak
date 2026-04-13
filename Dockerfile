FROM europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/jre:openjdk-21@sha256:d6b729bc7e9ec9198b1af32c8a10a69a9c6b2ec48b84c4f7f1b36f45334ac137

COPY --chown=1069:1069 build/libs/app.jar /app.jar

EXPOSE 8080

CMD ["-jar", "/app.jar"]
