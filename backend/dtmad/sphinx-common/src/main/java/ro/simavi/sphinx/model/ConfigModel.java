package ro.simavi.sphinx.model;

import lombok.Getter;
import lombok.Setter;
import ro.simavi.sphinx.entities.common.enums.ConfigType;

import java.io.Serializable;

@Getter
@Setter
public class ConfigModel implements Serializable {

    private Long id;

    private String code;

    private String value;

    private String name;

    private String description;

    private ConfigType type;

    private String defaultValue;

}
