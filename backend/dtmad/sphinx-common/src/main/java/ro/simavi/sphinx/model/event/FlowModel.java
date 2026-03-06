package ro.simavi.sphinx.model.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
@ToString
public class FlowModel {

    @JsonProperty("pkts_toserver")
    private String pktsToServer;

    @JsonProperty("pkts_toclient")
    private String pktsToClient;

    @JsonProperty("bytes_toserver")
    private String bytesToServer;

    @JsonProperty("bytes_toclient")
    private String bytesToClient;

    private String start;

    private String end;

    private String age;

}
