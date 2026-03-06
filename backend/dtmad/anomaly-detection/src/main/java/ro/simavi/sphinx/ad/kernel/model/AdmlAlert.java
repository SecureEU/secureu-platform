package ro.simavi.sphinx.ad.kernel.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import ro.simavi.sphinx.ad.dpi.flow.model.ProtocolFlow;
import ro.simavi.sphinx.ad.kernel.model.algorithm.AlgorithmDetails;

import java.io.Serializable;

@Getter
@Setter
@ToString
public class AdmlAlert implements Serializable {

    private String title;

    // private type
    private String text;

    private String flowId;

    private long totalFlows;

    private String username;

    private String coords;

    private String timestamp;

    private AlgorithmDetails algorithm;

    private ProtocolFlow protocolFlow;

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getFlowId() {
        return flowId;
    }

    public void setFlowId(String flowId) {
        this.flowId = flowId;
    }

    public long getTotalFlows() {
        return totalFlows;
    }

    public void setTotalFlows(long totalFlows) {
        this.totalFlows = totalFlows;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getCoords() {
        return coords;
    }

    public void setCoords(String coords) {
        this.coords = coords;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public AlgorithmDetails getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(AlgorithmDetails algorithm) {
        this.algorithm = algorithm;
    }

    public ProtocolFlow getProtocolFlow() {
        return protocolFlow;
    }

    public void setProtocolFlow(ProtocolFlow protocolFlow) {
        this.protocolFlow = protocolFlow;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}
