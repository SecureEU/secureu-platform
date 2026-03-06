package ro.simavi.sphinx.model.event.alert;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Setter
@Getter
public class SphinxModel implements Serializable {

    private String component;

    private String tool;

    private String username;

    private String instanceKey;

    private String hostname;
}
