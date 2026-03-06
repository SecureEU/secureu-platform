package ro.simavi.sphinx.model.event.alertStix;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
@ToString
public class AlertStixModel {

    @JsonProperty("severity")
    public String severity;

    @JsonProperty("signature")
    public String signature;

    @JsonProperty("category")
    public String category;

    @JsonProperty("action")
    public String action;
}
