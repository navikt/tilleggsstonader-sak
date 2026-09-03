FROM europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/jre:openjdk-21@sha256:fc1f5fa87d8525638c5923f4df087da810ff181954ef73137c26a5104cbf832c

COPY --chown=1069:1069 build/libs/app.jar /app.jar

EXPOSE 8080

CMD ["-jar", "/app.jar"]
