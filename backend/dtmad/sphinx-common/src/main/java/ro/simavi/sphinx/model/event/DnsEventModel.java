package ro.simavi.sphinx.model.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Getter
@Setter
@ToString(callSuper=true, includeFieldNames=true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class DnsEventModel extends EventModel{

    @JsonProperty("dns")
    private DnsModel dns;

}
