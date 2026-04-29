#!/bin/bash
# Capture packets with tshark and produce tab-separated lines to Kafka.
# Field order matches dtm.tool.tshark.fields in application.properties:
#   frame.number, frame.time_delta, frame.time, frame.interface_name,
#   frame.interface_id, frame.interface_description, frame.cap_len, frame.len,
#   frame.protocols, eth.src, eth.dst, ip.src, ip.dst, ip, ip.proto,
#   ip.src_host, ip.dst_host, tcp.port, udp.port, ipv6, ipv6.addr,
#   ipv6.src, ipv6.dst, http.host, dns.qry.name, tcp.stream, tcp.srcport,
#   tcp.dstport, udp.srcport, udp.dstport, _ws.col.Info
#
# KafkaHandler.receiveMessage() does: message.split("\t", -1)

set -euo pipefail

IFACE="${CAPTURE_INTERFACE:-any}"
BROKER="${KAFKA_BROKER:-localhost:9092}"
TOPIC="${KAFKA_TOPIC:-dtm-package}"

echo "tshark capture starting: interface=${IFACE} broker=${BROKER} topic=${TOPIC}"

# Wait a few seconds for the interface to be ready
sleep 3

tshark -l -n -i "${IFACE}" \
  -T fields \
  -e frame.number \
  -e frame.time_delta \
  -e frame.time \
  -e frame.interface_name \
  -e frame.interface_id \
  -e frame.interface_description \
  -e frame.cap_len \
  -e frame.len \
  -e frame.protocols \
  -e eth.src \
  -e eth.dst \
  -e ip.src \
  -e ip.dst \
  -e ip \
  -e ip.proto \
  -e ip.src_host \
  -e ip.dst_host \
  -e tcp.port \
  -e udp.port \
  -e ipv6 \
  -e ipv6.addr \
  -e ipv6.src \
  -e ipv6.dst \
  -e http.host \
  -e dns.qry.name \
  -e tcp.stream \
  -e tcp.srcport \
  -e tcp.dstport \
  -e udp.srcport \
  -e udp.dstport \
  -e _ws.col.Info \
  -E separator=/t \
  -E quote=n \
  -E occurrence=f \
  2>/dev/null | kcat -P -b "${BROKER}" -t "${TOPIC}"
