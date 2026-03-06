#!/bin/bash

# Configuration
LOG_FILE="/var/seuxdr/manager/queue/genlogs.log"  # Output log file
START=1                        # Start of the username counter
END=10000                      # End of the username counter
LOG_TEMPLATE="2024-12-02T14:34:18.954235+00:00 esteban sudo: clonetest%s : 3 incorrect password attempts ; TTY=pts/0 ; PWD=/home/stefanos/seuxdr_CloneSystems_1_linux_arm64 ; USER=root ; COMMAND=/usr/bin/su [group_id=1]"

# Remove existing log file if it exists
if [ -f "$LOG_FILE" ]; then
    rm "$LOG_FILE"
fi

# Generate logs
for i in $(seq $START $END); do
    LOG_MESSAGE=$(printf "$LOG_TEMPLATE" "$i")
    echo "$LOG_MESSAGE" >> "$LOG_FILE"
done

echo "Generated $END logs in $LOG_FILE."
