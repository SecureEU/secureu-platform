package ro.simavi.sphinx.model.event.alertStix;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
@ToString
public class DetailsAlertModel implements Serializable {

    @JsonProperty("totalFlows")
    private int totalFlow;

    @JsonProperty("type")
    private String type;

    @JsonProperty("protocolFlow")
    private ProtocolFlowModel protocolFlowModel;

    @JsonProperty("text")
    private String text;

    @JsonProperty("title")
    private String title;

    @JsonProperty("flowId")
    private String flowId;

    @JsonProperty("coords")
    private String coords;

    @JsonProperty("username")
    private String username;

    @JsonProperty("timestamp")
    private String timestamp;

    @JsonProperty("algorithm")
    private AlgorithmAlertModel algorithm;

    @JsonProperty("dest_port")
    private int destPort;

    @JsonProperty("dest_ip")
    private String destIp;

    @JsonProperty("src_port")
    private int srcPort;

    @JsonProperty("src_ip")
    private String srcIp;

    @JsonProperty("host")
    private String host;

    @JsonProperty("proto")
    private String proto;

    private AlertStixModel alert;

    @JsonProperty("src_geoip")
    private SrcGeoipModel srcGeoip;

}
