FROM europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/jre:openjdk-24@sha256:902f4bb3c34aef336fad9c417fcbce476bd05aab120d1f585c94db4a99046659

COPY --chown=1069:1069 build/libs/app.jar /app.jar

EXPOSE 8080

CMD ["-jar", "/app.jar"]
