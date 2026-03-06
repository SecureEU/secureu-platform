rem mvn clean -Dspring.profiles.active=docker package
docker build -t sphinxproject/anomaly-detection .
docker run -it --link sphinx-postgres --net docker_default -p8087:8087 sphinxproject/anomaly-detection