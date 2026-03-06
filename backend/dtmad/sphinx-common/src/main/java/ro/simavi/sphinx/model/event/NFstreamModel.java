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
public class NFstreamModel implements Serializable {

    @JsonProperty("@timestamp")
    @JsonSerialize(using = SphinxLocalDateTimeSerializer.class)
    @JsonDeserialize(using = SphinxLocalDateTimeDeserializer.class)
    private LocalDateTime timestamp;

    @JsonProperty("id")
    private String flowId;

    @JsonProperty("application_name")
    private String protocol;

    @JsonProperty("protocol")
    private String ipProtocol;

    @JsonProperty("host")
    private String host;

    @JsonProperty("src_ip")
    private String srcIp;

    @JsonProperty("src_port")
    private String srcPort;

    @JsonProperty("dst_ip")
    private String destIp;

    @JsonProperty("dst_port")
    private String destPort;

//    private String detectedProtocol;

    @JsonProperty("bidirectional_mean_ps")
    private String avgPacketSize;

    @JsonProperty("bidirectional_mean_piat_ms")
    private String avgInterTime;

    @JsonProperty("bidirectional_duration_ms")
    private String flowDuration;

    @JsonProperty("bidirectional_max_ps")
    private String maxPacketSize;

    @JsonProperty("bidirectional_min_ps")
    private String minPacketSize;

    @JsonProperty("bidirectional_bytes")
    private String bytes;

    @JsonProperty("bidirectional_packets")
    private String packets;

    @JsonProperty("src2dst_bytes")
    private String srcToDstBytes;

    @JsonProperty("application_category_name")
    private String applicationCategoryName;

    @JsonProperty("dst2src_bytes")
    private String dstToSrcBytes;

    //0 - dns_num_queries
    //1 - dns_num_answers
    //-1 nu are pachete
    @JsonProperty("splt_direction")
    private String splt_direction;

    //packet_size_n
    //-1 nu mai exista pachete
    @JsonProperty("splt_ps")
    private String splt_ps;

    //inter_time_n
    //0 pentru primul pachet
    //-1 nu mai exista pachete
    @JsonProperty("splt_piat_ms")
    private String splt_piat_ms;

    @JsonProperty("udps.queryType")
    private String queryType;

    @JsonProperty("udps.rspType")
    private String rspType;

    @JsonProperty("udps.flags")
    private String flags;

    @JsonProperty("requested_server_name")
    private String serverName;

    //http
    @JsonProperty("udps.method")
    private long httpMethod;

    @JsonProperty("udps.uri")
    private String httpUrl;

    @JsonProperty("udps.host")
    private String hostServerName;


//    // @JsonProperty("ether")
//    private EthernetModel ether;

}
