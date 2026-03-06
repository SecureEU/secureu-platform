FROM alpine:3.19
RUN apk add --no-cache tshark kcat bash
COPY capture.sh /capture.sh
RUN chmod +x /capture.sh
ENTRYPOINT ["/bin/bash", "/capture.sh"]
