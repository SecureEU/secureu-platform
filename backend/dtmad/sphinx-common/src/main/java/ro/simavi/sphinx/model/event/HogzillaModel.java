package ro.simavi.sphinx.model.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import ro.simavi.sphinx.serializers.SphinxLocalDateTimeDeserializer;
import ro.simavi.sphinx.serializers.SphinxLocalDateTimeSerializer;


import java.io.Serializable;
import java.time.LocalDateTime;

@Getter
@Setter
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class HogzillaModel implements Serializable {

    @JsonProperty("@timestamp")
    @JsonSerialize(using = SphinxLocalDateTimeSerializer.class)
    @JsonDeserialize(using = SphinxLocalDateTimeDeserializer.class)
    private LocalDateTime timestamp;

//    @JsonProperty("id")
    private String flowId;

    @JsonProperty("protocol")
    private String protocol;

//    @JsonProperty("host")
//    private String host;

    @JsonProperty("src")
    private String srcIp;

    @JsonProperty("src_port")
    private String srcPort;

    @JsonProperty("dst")
    private String destIp;

    @JsonProperty("dst_port")
    private String destPort;

    @JsonProperty("payload_bytes_avg")
    private String avgPacketSize;

    @JsonProperty("inter_time_avg")
    private String avgInterTime;

    @JsonProperty("flow_duration")
    private String flowDuration;

    @JsonProperty("payload_bytes_max")
    private String maxPacketSize;

    @JsonProperty("payload_bytes_min")
    private String minPacketSize;

    @JsonProperty("bytes")
    private String bytes;

    @JsonProperty("packets")
    private String packets;

    @JsonProperty("inter_time-0")
    private String interTime0;

    @JsonProperty("inter_time-1")
    private String interTime1;

    @JsonProperty("inter_time-2")
    private String interTime2;

    @JsonProperty("inter_time-3")
    private String interTime3;

    @JsonProperty("inter_time-4")
    private String interTime4;

    @JsonProperty("inter_time-5")
    private String interTime5;

    @JsonProperty("packet_pay_size-0")
    private String packetSize0;

    @JsonProperty("packet_pay_size-1")
    private String packetSize1;

    @JsonProperty("packet_pay_size-2")
    private String packetSize2;

    @JsonProperty("packet_pay_size-3")
    private String packetSize3;

    @JsonProperty("packet_pay_size-4")
    private String packetSize4;

    @JsonProperty("packet_pay_size-5")
    private String packetSize5;

    @JsonProperty("packets_without_payload")
    private String packetsWithoutPayload;

    @JsonProperty("dns_num_queries")
    private String dnsNumQueries;

    @JsonProperty("dns_num_answers")
    private String dnsNumAnswers;

    @JsonProperty("dns_reply_code")
    private String dnsRetode;

    @JsonProperty("dns_query_type")
    private String dnsQueryType;

    @JsonProperty("dns_rsp_type")
    private String dnRspType;

    //http
    @JsonProperty("http_method")
    private long httpMethod;

//    @JsonProperty("udps.uri")
//    private String httpUrl;

//    @JsonProperty("udps.host")
//    private String hostServerName;
}
