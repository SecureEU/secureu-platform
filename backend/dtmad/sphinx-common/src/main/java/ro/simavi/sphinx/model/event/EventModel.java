package ro.simavi.sphinx.model.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import ro.simavi.sphinx.model.event.alert.*;
import ro.simavi.sphinx.serializers.HostStringDeserializer;
import ro.simavi.sphinx.serializers.SphinxLocalDateTimeDeserializer;
import ro.simavi.sphinx.serializers.SphinxLocalDateTimeDeserializer2;
import ro.simavi.sphinx.serializers.SphinxLocalDateTimeSerializer;

import java.io.Serializable;
import java.time.LocalDateTime;

@Getter
@Setter
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonSubTypes({
        @JsonSubTypes.Type(value = DnsEventModel.class, name = "dns"),
        @JsonSubTypes.Type(value = HttpEventModel.class, name = "http") ,
        @JsonSubTypes.Type(value = DhcpEventModel.class, name = "dhcp") ,
        @JsonSubTypes.Type(value = Krb5EventModel.class, name = "krb5") ,
        @JsonSubTypes.Type(value = DcerpcEventModel.class, name = "dcerpc") ,
        @JsonSubTypes.Type(value = SmbEventModel.class, name = "smb") ,
        @JsonSubTypes.Type(value = SshEventModel.class, name = "ssh") ,
        @JsonSubTypes.Type(value = SipEventModel.class, name = "sip") ,
        @JsonSubTypes.Type(value = TftpEventModel.class, name = "tftp") ,
        @JsonSubTypes.Type(value = AnomalyEventModel.class, name = "anomaly") ,
        @JsonSubTypes.Type(value = TlsEventModel.class, name = "tls"),
        @JsonSubTypes.Type(value = StatsEventModel.class, name = "stats"),
        @JsonSubTypes.Type(value = FlowEventModel.class, name = "flow"),
        @JsonSubTypes.Type(value = SnmpEventModel.class, name = "snmp"),
        @JsonSubTypes.Type(value = AlertEventModel.class, name = "alert"),
        @JsonSubTypes.Type(value = FileInfoEventModel.class, name = "fileinfo"),
        @JsonSubTypes.Type(value = QuicEventModel.class, name = "quic"),
        @JsonSubTypes.Type(value = PortDiscoveryAlertModel.class, name = "PortDiscoveryAlertModel"),
        @JsonSubTypes.Type(value = BlackWebAlertModel.class, name = "BlackWebAlertModel"),
        @JsonSubTypes.Type(value = MassiveDataProcessingAlertModel.class, name = "MassiveDataProcessingAlertModel"),
        @JsonSubTypes.Type(value = TcpAnalysisFlagsAlertModel.class, name = "TcpAnalysisFlagsAlertModel"),
        @JsonSubTypes.Type(value = ReactivateAssetAlertModel.class, name = "ReactivateAssetAlertModel"),

})
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "event_type")
public class EventModel implements Serializable {

    @JsonProperty("@timestamp")
    @JsonSerialize(using = SphinxLocalDateTimeSerializer.class)
    @JsonDeserialize(using = SphinxLocalDateTimeDeserializer.class)
    private LocalDateTime timestamp;

    @JsonProperty("timestamp")
    @JsonDeserialize(using = SphinxLocalDateTimeDeserializer2.class)
    private LocalDateTime timestamp2;

    @JsonProperty("community_id")
    private String communityId;

    @JsonProperty("flow_id")
    private String flowId;

    @JsonProperty("proto")
    private String protocol;

    @JsonProperty("host")
    @JsonDeserialize(using = HostStringDeserializer.class)
    private String host;

    @JsonProperty("src_ip")
    private String srcIp;

    @JsonProperty("src_port")
    private String srcPort;

    @JsonProperty("dest_ip")
    private String destIp;

    @JsonProperty("dest_port")
    private String destPort;

    @JsonProperty("event_type")
    private String eventType;

    @JsonProperty("event_kind")
    private String eventKind;

    @JsonProperty("type")
    private String type;

    @JsonProperty("ether")
    private EthernetModel ether;

}