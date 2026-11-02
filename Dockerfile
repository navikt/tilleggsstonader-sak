FROM europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/jre:openjdk-21@sha256:f4b5a81ff71fcd1fa2bb88b6fefe71ddcd434a3113d58796268e4aaf08a1cdfc

COPY --chown=1069:1069 build/libs/app.jar /app.jar

EXPOSE 8080

CMD ["-jar", "/app.jar"]
