FROM java:8-jdk-alpine
COPY ./target/torrentmover-0.0.9.jar /usr/torrentmover/torrentmover.jar
COPY ./application.properties /usr/torrentmover/application.properties
WORKDIR /usr/torrentmover
EXPOSE 443
ENTRYPOINT ["java", "-Xmx256m", "-jar", "torrentmover.jar"]