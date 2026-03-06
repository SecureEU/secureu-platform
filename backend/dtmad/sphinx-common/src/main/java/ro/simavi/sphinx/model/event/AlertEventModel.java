package ro.simavi.sphinx.model.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import ro.simavi.sphinx.model.event.alert.SphinxModel;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
@ToString
public class AlertEventModel extends EventModel {

    private Long count;

    @JsonProperty("alert")
    private AlertModel alert;

    @JsonProperty("sphinx")
    private SphinxModel sphinxModel;

}
