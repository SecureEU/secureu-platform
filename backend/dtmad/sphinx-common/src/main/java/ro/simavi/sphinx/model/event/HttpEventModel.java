package ro.simavi.sphinx.model.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
@ToString(callSuper=true, includeFieldNames=true)
public class HttpEventModel extends EventModel {

    private HttpModel http;

}
