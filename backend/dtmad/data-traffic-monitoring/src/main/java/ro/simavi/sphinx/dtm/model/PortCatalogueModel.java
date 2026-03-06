package ro.simavi.sphinx.dtm.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Getter;
import lombok.Setter;
import ro.simavi.sphinx.serializers.SphinxLocalDateTimeDeserializer;
import ro.simavi.sphinx.serializers.SphinxLocalDateTimeSerializer;

import java.time.LocalDateTime;

@Getter
@Setter
public class PortCatalogueModel {

    private Long id;

    @JsonProperty("@timestamp")
    @JsonSerialize(using = SphinxLocalDateTimeSerializer.class)
    @JsonDeserialize(using = SphinxLocalDateTimeDeserializer.class)
    private LocalDateTime timestamp;
    
    private Long port;

    private Long endPortInterval;

    private String name;

    private String description;
}
