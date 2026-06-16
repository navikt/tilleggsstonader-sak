FROM europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/jre:openjdk-21@sha256:e5c0b3ba352f0850a786961b083c44a1134ceb59c8e12db9deaf077fd02c28dc

COPY --chown=1069:1069 build/libs/app.jar /app.jar

EXPOSE 8080

CMD ["-jar", "/app.jar"]
