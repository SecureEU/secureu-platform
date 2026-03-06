#!/bin/sh
rm /var/log/suricata/eve.json 
rm /var/log/suricata/stats.log 
kill -1 $(pidof suricata)



