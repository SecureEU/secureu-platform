package ro.simavi.sphinx.model.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class EthernetModel {

    @JsonProperty("src_mac")
    private String srcMac;

    @JsonProperty("dest_mac")
    private String destMac;
}
