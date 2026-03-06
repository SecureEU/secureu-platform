#!/bin/sh

## Do whatever you need with env vars here ...

/etc/init.d/cron start
java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005 -Dspring.profiles.active=docker,kafka org.springframework.boot.loader.JarLauncher
