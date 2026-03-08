FROM europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/jre:openjdk-21@sha256:bc9cf92fa2a4f1ea4ba6d84ed4b153c00a1c4dec168d2bb0b24b69dabdf216c8

COPY --chown=1069:1069 build/libs/app.jar /app.jar

EXPOSE 8080

CMD ["-jar", "/app.jar"]
