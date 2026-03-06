FROM adoptopenjdk:14-jre-hotspot as builder
WORKDIR application
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} application.jar
RUN java -Djarmode=layertools -jar application.jar extract

FROM adoptopenjdk:14-jre-hotspot
# RUN  mkdir -p /sphinx/tshark/export
# pun fisierul de test
# ARG TEST_FILE=temp/input.csv
# COPY ${TEST_FILE} /sphinx/tshark/export/input.csv

RUN mkdir -p /var/log
RUN groupadd tshark
RUN useradd sphinx
RUN usermod -a -G tshark sphinx
RUN apt-get update -y
RUN apt-get install -y tshark
RUN chgrp tshark /usr/bin/dumpcap
RUN chmod 777 /usr/bin/dumpcap
RUN setcap cap_net_raw,cap_net_admin=eip /usr/bin/dumpcap
RUN getcap /usr/bin/dumpcap
RUN mkdir -p /var/log/tshark

#RUN mkdir /workshop
#ADD ./src/main/resources/pcap/fpcap.pcap /workshop/fpcap.pcap
#ADD ./src/main/resources/csv/test_ISCX.csv /workshop/test_ISCX.csv

#RUN mkdir /sflow
#ADD ./src/main/resources/csv/*.csv /sflow
#ADD ./src/main/resources/logstash/workshop/alerte_suricata.json /workshop/alerte_suricata.json

# >>>>>>>: [jks]
RUN mkdir /jks
ADD ./src/main/resources/*.jks /jks

# >>>>>>>: [suricata]
RUN apt-get update -y
RUN apt-get install -y apt-utils
RUN apt-get install software-properties-common -y
RUN add-apt-repository ppa:oisf/suricata-stable -y
RUN apt-get update -y
RUN apt-get install -y suricata
RUN suricata -V
RUN apt-get upgrade suricata -y
# RUN apt-get install -y suricata-update
RUN mkdir -p /var/log/suricata
RUN rm -v /etc/suricata/suricata.yaml
ADD ./src/main/resources/suricata/suricata.yaml /etc/suricata/
ADD ./src/main/resources/suricata/classification.config /etc/suricata/
ADD ./src/main/resources/suricata/magic.mgc /etc/suricata/
ADD ./src/main/resources/suricata/reference.config /etc/suricata/
ADD ./src/main/resources/suricata/rules/other/*.rules /etc/suricata/rules/
ADD ./src/main/resources/suricata/rules/blacklist/*.rules /etc/suricata/rules/
#ADD ./src/main/resources/suricata/rules/blacklist/sslblacklist_tls_cert.rules /etc/suricata/rules/sslblacklist_tls_cert.rules
#ADD ./src/main/resources/suricata/rules/blacklist/sslipblacklist.rules /etc/suricata/rules/sslipblacklist.rules

# >>>>>>>: [logstash]
RUN apt install software-properties-common -y
RUN add-apt-repository ppa:openjdk-r/ppa -y
RUN apt-get update -y
RUN apt-get install -y gnupg2
RUN wget -qO - https://packages.elastic.co/GPG-KEY-elasticsearch | apt-key add -
RUN apt-get install apt-transport-https ca-certificates -y
RUN echo "deb https://artifacts.elastic.co/packages/7.x/apt stable main"| tee -a /etc/apt/sources.list.d/elastic-7.x.list
RUN apt-get update -y
RUN apt autoremove logstash -y
#RUN apt-get install -y logstash
#RUN apt-get install -y [logstash-7.13.2]
RUN apt-get install -y logstash=1:7.13.2-1


RUN mkdir -p /usr/share/logstash/vendor/geoip

RUN mkdir -p /usr/share/logstash/vendor/bundle/jruby/2.5.0/gems/logstash-integration-kafka-10.7.6-java/vendor/jar-dependencies/nimbusds
RUN mkdir -p /usr/share/logstash/vendor/bundle/jruby/2.5.0/gems/logstash-integration-kafka-10.7.6-java/vendor/jar-dependencies/nimbusds/nimbus-jose-jwt
RUN mkdir -p /usr/share/logstash/vendor/bundle/jruby/2.5.0/gems/logstash-integration-kafka-10.7.6-java/vendor/jar-dependencies/nimbusds/nimbus-jose-jwt/9.10.1

RUN mkdir -p /usr/share/logstash/vendor/bundle/jruby/2.5.0/gems/logstash-integration-kafka-10.7.6-java/vendor/jar-dependencies/io
RUN mkdir -p /usr/share/logstash/vendor/bundle/jruby/2.5.0/gems/logstash-integration-kafka-10.7.6-java/vendor/jar-dependencies/io/kafka-oauth-common
RUN mkdir -p /usr/share/logstash/vendor/bundle/jruby/2.5.0/gems/logstash-integration-kafka-10.7.6-java/vendor/jar-dependencies/io/kafka-oauth-common/0.8.1

RUN mkdir -p /usr/share/logstash/vendor/bundle/jruby/2.5.0/gems/logstash-integration-kafka-10.7.6-java/vendor/jar-dependencies/io/kafka-oauth-client
RUN mkdir -p /usr/share/logstash/vendor/bundle/jruby/2.5.0/gems/logstash-integration-kafka-10.7.6-java/vendor/jar-dependencies/io/kafka-oauth-client/9.10.1

RUN mkdir -p /usr/share/logstash/config
# ADD ./src/main/resources/logstash/templates/suricata_template.json /usr/share/logstash/templates/suricata_template.json
RUN mkdir -p /usr/share/logstash/logs

# >>>>>>>: [nfstream]
RUN apt-get install -y python3
RUN apt-get install python3-pip -y
RUN pip3 install nfstream==6.3.4
#RUN apt-get update -y
RUN apt-get install autoconf automake libtool pkg-config flex bison gettext libjson-c-dev -y
RUN apt-get install libusb-1.0-0-dev libdbus-glib-1-dev libbluetooth-dev libnl-genl-3-dev -y

RUN apt-get -y install git
RUN git clone --branch libgpg-error-1.42 https://github.com/gpg/libgpg-error
WORKDIR /libgpg-error
RUN ./autogen.sh
RUN ./configure -enable-maintainer-mode --enable-static --enable-shared --with-pic --disable-doc --disable-nls
RUN make
RUN make install

WORKDIR ../
RUN rm -rf libgpg-error

RUN git clone --branch libgcrypt-1.8.8 https://github.com/gpg/libgcrypt
WORKDIR /libgcrypt
RUN ./autogen.sh
RUN ./configure -enable-maintainer-mode --enable-static --enable-shared --with-pic --disable-doc
RUN make
RUN make install

WORKDIR ../
RUN rm -rf libgcrypt

RUN git clone --branch fanout https://github.com/tsnoam/libpcap
WORKDIR /libpcap
RUN ./configure --enable-ipv6 --disable-universal --enable-dbus=no --without-libnl
RUN make
RUN make install

WORKDIR ../
RUN rm -rf libpcap
RUN git clone --branch dev https://github.com/ntop/nDPI.git
WORKDIR /nDPI
RUN git checkout tags/4.0
RUN apt-get install libjson-c-dev -y
RUN ./autogen.sh
RUN ./configure
RUN make
RUN mkdir /usr/local/include/ndpi
RUN cp -a src/include/. /usr/local/include/ndpi/
RUN cp example/ndpiReader /usr/local/bin/ndpiReader
RUN cp src/lib/libndpi.a /usr/local/lib/libndpi.a
WORKDIR ../
RUN rm -rf nDPI
RUN git clone https://github.com/nfstream/nfstream.git
WORKDIR /nfstream
RUN git checkout tags/v6.3.4
RUN python3 -m pip install -r requirements.txt
RUN python3 setup.py bdist_wheel
WORKDIR ../

ADD ./src/test/resources/features-collect.py /nfstream/nfstream/features-collect.py
#ADD ./src/main/resources/pcap/fpcap.pcap /nfstream/nfstream/fpcap.pcap
# RUN mkdir -p /usr/share/logstash/templates
# RUN rm -v /usr/share/logstash/vendor/geoip/GeoLite2-City.mmdb
# RUN rm -v /usr/share/logstash/config/logstash.conf
# RUN rm -v /usr/share/logstash/templates/suricata_template.json
# ADD ./src/main/resources/logstash/config/logstash.conf /usr/share/logstash/config/logstash.conf
# ADD ./src/main/resources/suricata/eve.json /var/log/suricata/eve.json
ADD ./src/main/resources/logstash/config/logstash_without_elasticsearch.conf /usr/share/logstash/config/logstash_without_elasticsearch.conf
ADD ./src/main/resources/logstash/config/logstashNf.conf /usr/share/logstash/config/logstashNf.conf
# ADD ./src/main/resources/logstash/config/logstashCicFlow.conf /usr/share/logstash/config/logstashCicFlow.conf
# ADD ./src/main/resources/logstash/config/logstashInfo.conf /usr/share/logstash/config/logstash.conf

ADD ./src/main/resources/logstash/config/stix_format_dtm.conf /usr/share/logstash/config/stix_format_dtm.conf

#ADD ./src/main/resources/logstash/vendor/geoip/GeoLite2-City.mmdb /usr/share/logstash/vendor/geoip/GeoLite2-City.mmdb

RUN rm -v /usr/share/logstash/vendor/bundle/jruby/2.5.0/gems/logstash-integration-kafka-10.7.6-java/lib/logstash/plugin_mixins/kafka_support.rb
ADD ./src/main/resources/logstash/vendor/bundle/jruby/2.5.0/gems/logstash-integration-kafka-10.7.6-java/lib/logstash/plugin_mixins/kafka_support.rb /usr/share/logstash/vendor/bundle/jruby/2.5.0/gems/logstash-integration-kafka-10.7.6-java/lib/logstash/plugin_mixins/kafka_support.rb

RUN rm -v /usr/share/logstash/vendor/bundle/jruby/2.5.0/gems/logstash-integration-kafka-10.7.6-java/lib/logstash-integration-kafka_jars.rb
ADD ./src/main/resources/logstash/vendor/bundle/jruby/2.5.0/gems/logstash-integration-kafka-10.7.6-java/lib/logstash-integration-kafka_jars.rb /usr/share/logstash/vendor/bundle/jruby/2.5.0/gems/logstash-integration-kafka-10.7.6-java/lib/logstash-integration-kafka_jars.rb

ADD ./src/main/resources/logstash/vendor/bundle/jruby/2.5.0/gems/logstash-integration-kafka-10.7.6-java/vendor/jar-dependencies/com/nimbusds/nimbus-jose-jwt/9.10.1/nimbus-jose-jwt-9.10.1.jar /usr/share/logstash/vendor/bundle/jruby/2.5.0/gems/logstash-integration-kafka-10.7.6-java/vendor/jar-dependencies/com/nimbusds/nimbus-jose-jwt/9.10.1/nimbus-jose-jwt-9.10.1.jar
ADD ./src/main/resources/logstash/vendor/bundle/jruby/2.5.0/gems/logstash-integration-kafka-10.7.6-java/vendor/jar-dependencies/io/strimzi/kafka-oauth-client/0.8.1/kafka-oauth-client-0.8.1.jar  /usr/share/logstash/vendor/bundle/jruby/2.5.0/gems/logstash-integration-kafka-10.7.6-java/vendor/jar-dependencies/io/strimzi/kafka-oauth-client/0.8.1/kafka-oauth-client-0.8.1.jar
ADD ./src/main/resources/logstash/vendor/bundle/jruby/2.5.0/gems/logstash-integration-kafka-10.7.6-java/vendor/jar-dependencies/io/strimzi/kafka-oauth-common/0.8.1/kafka-oauth-common-0.8.1.jar  /usr/share/logstash/vendor/bundle/jruby/2.5.0/gems/logstash-integration-kafka-10.7.6-java/vendor/jar-dependencies/io/strimzi/kafka-oauth-common/0.8.1/kafka-oauth-common-0.8.1.jar

RUN mkdir -p /usr/share/logstash/dictionaires

ADD ./src/main/resources/logstash/dictionaires/ /usr/share/logstash/dictionaires/

ADD ./src/main/resources/logstash/config/jvm.options /etc/logstash/jvm.options

# >>>>>>>>>: cron
RUN apt-get -y install cron

ADD ./src/main/resources/crontab/cronSuricata.sh /etc/cron.d/cronSuricata.sh
RUN crontab -l | { cat; echo "0 5 * * * /usr/bin/sh /etc/cron.d/cronSuricata.sh  > /tmp/cronjob.log 2>&1"; } | crontab
ADD ./src/main/resources/crontab/script.sh /script.sh
#RUN chmod 0644  /etc/cron.d/cronSuricata.sh
#RUN service cron start

WORKDIR application
COPY --from=builder application/dependencies/ ./
COPY --from=builder application/spring-boot-loader/ ./
COPY --from=builder application/snapshot-dependencies/ ./
# COPY --from=builder application/resources/ ./
COPY --from=builder application/application/ ./
ADD ./src/main/resources/crontab/script.sh ./script.sh
# ENV JAVA_TOOL_OPTIONS -agentlib:jdwp=transport=dt_socket,address=5005,server=y,suspend=n
EXPOSE 8080 5005
ENTRYPOINT ["/bin/sh", "script.sh"]
#ENTRYPOINT ["java", "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005","-Dspring.profiles.active=docker,kafka", "org.springframework.boot.loader.JarLauncher"]
# ENTRYPOINT ["java", "-Dspring.profiles.active=docker,kafka", "org.springframework.boot.loader.JarLauncher"]