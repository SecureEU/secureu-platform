package ro.simavi.sphinx.model.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Getter;
import lombok.Setter;
import ro.simavi.sphinx.serializers.SphinxLocalDateTimeDeserializer;
import ro.simavi.sphinx.serializers.SphinxLocalDateTimeSerializer;

import java.io.Serializable;
import java.time.LocalDateTime;


@Getter
@Setter
public class DtmCmdEventModel implements Serializable {

    @JsonProperty("@timestamp")
    @JsonSerialize(using = SphinxLocalDateTimeSerializer.class)
    @JsonDeserialize(using = SphinxLocalDateTimeDeserializer.class)
    private LocalDateTime createdDate;

    private String url;

    private String method;

    private String instanceKey;

    private String hostname;

    private String username;

    private String uuid;

}
