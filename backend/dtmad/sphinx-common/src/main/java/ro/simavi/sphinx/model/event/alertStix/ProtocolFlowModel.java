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
public class ProtocolFlowModel implements Serializable {

    @JsonProperty("detectedProtocol")
    private String detectedProtocol;

    @JsonProperty("lowerPort")
    private int lowerPort;

    @JsonProperty("upperPort")
    private int upperPort;

    @JsonProperty("upperIp")
    private String upperIp;

    @JsonProperty("lowerIp")
    private String lowerIp;

    @JsonProperty("ipProtocol")
    private int ipProtocol;

    @JsonProperty("flowDuration")
    private int flowDuration;

    @JsonProperty("bytes")
    private int bytes;

    @JsonProperty("packets")
    private int packets;

    @JsonProperty("hostname")
    private String hostname;


}
