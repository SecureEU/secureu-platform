package ro.simavi.sphinx.dtm.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Getter;
import lombok.Setter;
import ro.simavi.sphinx.model.event.alert.SphinxModel;
import ro.simavi.sphinx.serializers.SphinxLocalDateTimeDeserializer;
import ro.simavi.sphinx.serializers.SphinxLocalDateTimeSerializer;

import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Getter
@Setter
public class AssetModel {

    private String id;

    @JsonProperty("@timestamp")
    @JsonSerialize(using = SphinxLocalDateTimeSerializer.class)
    @JsonDeserialize(using = SphinxLocalDateTimeDeserializer.class)
    private LocalDateTime timestamp;

    @NotNull
    private String physicalAddress;

    @NotNull
    private String name;

    private String description;

    private String status;

    private SphinxModel sphinx;

    private String ip;

    @JsonProperty("lastTouch")
    @JsonSerialize(using = SphinxLocalDateTimeSerializer.class)
    @JsonDeserialize(using = SphinxLocalDateTimeDeserializer.class)
    private LocalDateTime lastTouch;

    private int typeId;

}
