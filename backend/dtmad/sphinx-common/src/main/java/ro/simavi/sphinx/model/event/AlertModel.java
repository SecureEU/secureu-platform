package ro.simavi.sphinx.model.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class AlertModel implements Serializable {

    @JsonProperty("signature")
    private String signature;

    @JsonProperty("severity")
    private String severity;

    @JsonProperty("category")
    private String category;

    @JsonProperty("action")
    private String action;

    @JsonProperty("metadata")
    private AlertMetadataModel metadata;
}
