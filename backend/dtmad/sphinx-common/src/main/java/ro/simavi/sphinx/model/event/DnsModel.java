package ro.simavi.sphinx.model.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
@ToString
public class DnsModel {

    private String rrname;

    @JsonProperty("rrtype")
    private String rrType;

    private String type;

    @JsonProperty("flags")
    private String flags;

    private String query;

    private String rd;

    private String qr;

    private String ra;

    private String rcode;

    private List<AuthorityModel> authorities;

    private List<AdditionalModel> additional;


}
