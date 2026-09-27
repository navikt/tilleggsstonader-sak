FROM europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/jre:openjdk-21@sha256:3002922db538ebd3ec14e92d5c01da387165c63f64d0a4956b9d23fb45de6cdf

COPY --chown=1069:1069 build/libs/app.jar /app.jar

EXPOSE 8080

CMD ["-jar", "/app.jar"]
