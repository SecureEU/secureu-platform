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
public class SrcGeoipModel {

    @JsonProperty("country_name")
    private String countryName;

    @JsonProperty("city_name")
    private String cityName;

    @JsonProperty("latitude")
    private int latitude;

    @JsonProperty("longitude")
    private int longitude;


}
