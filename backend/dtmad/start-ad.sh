#!/bin/bash
DIR="$(cd "$(dirname "$0")" && pwd)"
exec java \
  --add-opens=java.base/java.nio=ALL-UNNAMED \
  --add-opens=java.base/sun.nio.ch=ALL-UNNAMED \
  --add-opens=java.base/java.lang=ALL-UNNAMED \
  --add-opens=java.base/java.util=ALL-UNNAMED \
  --add-opens=java.base/java.lang.invoke=ALL-UNNAMED \
  --add-opens=java.base/java.lang.reflect=ALL-UNNAMED \
  -jar "$DIR/anomaly-detection/target/anomaly-detection-0.0.1-SNAPSHOT.jar" \
  "--spring.datasource.url=jdbc:postgresql://localhost:8432/sphinx?currentSchema=sphinx" \
  --spring.datasource.username=sphinx \
  --spring.datasource.password=sphinx
