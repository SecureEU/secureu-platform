package ro.simavi.sphinx.dtm.model.event;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Getter;
import lombok.Setter;
import ro.simavi.sphinx.model.event.alert.SphinxModel;
import ro.simavi.sphinx.serializers.HostStringDeserializer;
import ro.simavi.sphinx.serializers.SphinxLocalDateTimeDeserializer;
import ro.simavi.sphinx.serializers.SphinxLocalDateTimeSerializer;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class MetricModel implements Serializable {

    @JsonProperty("@timestamp")
    @JsonSerialize(using = SphinxLocalDateTimeSerializer.class)
    @JsonDeserialize(using = SphinxLocalDateTimeDeserializer.class)
    private LocalDateTime timestamp;

    @JsonProperty("event_type")
    private String eventType = "stats";

    @JsonProperty("sphinx")
    private SphinxModel sphinxModel;

    @JsonDeserialize(using = HostStringDeserializer.class)
    private String host;

    private String username;

    @JsonProperty("@version")
    private String version;

    @JsonProperty("event_kind")
    private String eventKind;

    private String[] tags;

    private String type;

    // suricata
    private String path;

    private Map<String,Object> stats;

}
