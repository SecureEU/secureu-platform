package ro.simavi.sphinx.model.event.alertStix;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import ro.simavi.sphinx.model.event.EventModel;

import java.io.Serializable;
import java.util.List;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
@ToString
public class StixModelAlert implements Serializable {

    @JsonProperty("type")
    private String type;

    @JsonProperty("id")
    private String id;

    private List<ObjectStixModel> objects;

    @JsonProperty("flow_id")
    private String flowId;

    @JsonProperty("event_type")
    private String eventType;

}
