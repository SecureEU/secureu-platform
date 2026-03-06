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
public class AlgorithmAlertModel implements Serializable {

    @JsonProperty("type")
    private String type;

    @JsonProperty("numberOfClusters")
    private int numberOfClusters;

    @JsonProperty("minDirtyProportion")
    private double minDirtyProportion;

    @JsonProperty("maxAnomalousClusterProportion")
    private double maxAnomalousClusterProportion;

    @JsonProperty("numberOfPairs")
    private String numberOfPairs;

    @JsonProperty("alienIP")
    private String alienIP;

    @JsonProperty("bytesUp")
    private String bytesUp;

    @JsonProperty("bytesDown")
    private String bytesDown;

    @JsonProperty("numberPkts")
    private String numberPkts;

    @JsonProperty("ports")
    private String ports;

    @JsonProperty("myIP")
    private String myIP;

    @JsonProperty("hostname")
    private String hostname;

    @JsonProperty("connections")
    private String connections;

    @JsonProperty("tcpport")
    private String tcpport;

    @JsonProperty("dataMean")
    private String dataMean;

    @JsonProperty("dataStdev")
    private String dataStdev;

    @JsonProperty("pairsMean")
    private String pairsMean;

    @JsonProperty("pairsStdev")
    private String pairsStdev;

    @JsonProperty("aliens")
    private String aliens;

    @JsonProperty("numberOfFlows")
    private String numberOfFlows;

    @JsonProperty("numberOfFlowsPerPort")
    private String numberOfFlowsPerPort;

    @JsonProperty("flowsMean")
    private String flowsMean;

    @JsonProperty("flowsStdev")
    private String flowsStdev;

    @JsonProperty("numberOfFlowsAlienPort")
    private String numberOfFlowsAlienPort;

    @JsonProperty("portsMean")
    private String portsMean;

    @JsonProperty("portsStdev")
    private String portsStdev;
}
