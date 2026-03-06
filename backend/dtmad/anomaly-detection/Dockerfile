FROM adoptopenjdk:8-jre-hotspot as builder
WORKDIR application
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} application.jar
ADD ./target/anomaly-detection-0.0.1-SNAPSHOT.jar anomaly-detection-0.0.1-SNAPSHOT.jar
RUN java -Djarmode=layertools -jar application.jar extract

FROM adoptopenjdk:8-jre-hotspot

# resources for simulation
RUN  mkdir -p /sphinx/ad/simulation/sflow
ADD ./src/test/resources/sflow/*.csv /sphinx/ad/simulation/sflow/
#ADD ./src/test/resources/sflow/*.csv /home

RUN  apt-get update \
  && apt-get install -y wget \
  && rm -rf /var/lib/apt/lists/*

WORKDIR application
COPY --from=builder application/dependencies/ ./
COPY --from=builder application/spring-boot-loader/ ./
COPY --from=builder application/snapshot-dependencies/ ./
# COPY --from=builder application/resources/ ./
COPY --from=builder application/application/ ./
COPY --from=builder application/anomaly-detection-0.0.1-SNAPSHOT.jar ./
ENTRYPOINT ["java", "-Dspring.profiles.active=docker", "org.springframework.boot.loader.JarLauncher"]
