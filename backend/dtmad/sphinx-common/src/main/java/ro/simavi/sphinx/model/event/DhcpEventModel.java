package ro.simavi.sphinx.model.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString(callSuper=true, includeFieldNames=true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class DhcpEventModel extends EventModel {
}
