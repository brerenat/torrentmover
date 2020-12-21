FROM java:8-jdk-alpine
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} /usr/torrentmover/torrentmover.jar
COPY application.properties /usr/torrentmover/application.properties
WORKDIR /usr/torrentmover
EXPOSE 443
ENTRYPOINT ["java", "-Xmx256m", "-jar", "torrentmover.jar"]