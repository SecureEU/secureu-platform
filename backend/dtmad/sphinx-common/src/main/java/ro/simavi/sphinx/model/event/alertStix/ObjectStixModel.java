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
public class ObjectStixModel implements Serializable {

    @JsonProperty("type")
    private String type;

    @JsonProperty("id")
    private String id;

    @JsonProperty("spec_version")
    private String specVersion;

    @JsonProperty("created")
    private String created;

    @JsonProperty("modified")
    private String modified;

    @JsonProperty("name")
    private String name;

    @JsonProperty("details")
    private DetailsAlertModel details;

    @JsonProperty("source_ref")
    private String sourceRef;

    @JsonProperty("relationship_type")
    private String relationshipType;

    @JsonProperty("target_ref")
    private String targetRef;
}
