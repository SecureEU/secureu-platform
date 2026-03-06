package ro.simavi.sphinx.model.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class AlertMetadataModel implements Serializable {

    @JsonProperty("signature_severity")
    private String[] signatureSeverity;

    @JsonProperty("affected_product")
    private String[] affectedProduct;

    //@JsonProperty("performance_impact")
    //private String performanceImpact;

}
