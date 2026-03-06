rem mvn clean -Dspring.profiles.active=docker -Ddtm.instanceKey=1234567 package -DskipTests
docker build -t sphinxproject/data-traffic-monitoring .
docker run -it --link sphinx-postgres --net docker_default -p8087:8087 -p5005:5005 sphinxproject/data-traffic-monitoring
# docker run -it --link docker_sphinx-postgres --net sphinx-network -p8087:8087 -p5005:5005 sphinxproject/data-traffic-monitoring
rem docker push sphinxproject/data-traffic-monitoring

rem docker exec -t -i CONTAINER_ID /bin/bash
rem docker ps -- obtinere CONTAINER_ID
rem docker exec -t -i 446c17a227e0 /bin/bash

rem /etc/suricata
rem /var/log/suricata
rem /etc/suricata/rules

rem /usr/share/logstash/bin/logstash --version