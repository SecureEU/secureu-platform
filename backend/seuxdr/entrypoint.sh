
/bin/bash", "-c", "
      apt update && apt install -y curl && \
      curl -sO https://packages.wazuh.com/4.11/wazuh-install.sh && \
      bash ./wazuh-install.sh -a && \
      tail -f /var/ossec/logs/ossec.log
    