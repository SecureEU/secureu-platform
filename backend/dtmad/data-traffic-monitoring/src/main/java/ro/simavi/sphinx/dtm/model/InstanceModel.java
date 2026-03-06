package ro.simavi.sphinx.dtm.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InstanceModel {

    private Long id;

    private String name;

    private String key;

    private String description;

    private Boolean enabled;

    private String url;

    private Boolean up;

    private Boolean isMaster;

    private Boolean hasTshark;

    private Boolean hasSuricata;
}
