FROM europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/jre:openjdk-21@sha256:bd5cdf5a4bc98c288ed37ef11b2eb1fe1b02243fbce9f6811d8edd9be40dc0f5

COPY --chown=1069:1069 build/libs/app.jar /app.jar

EXPOSE 8080

CMD ["-jar", "/app.jar"]
