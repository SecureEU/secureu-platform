package ro.simavi.sphinx.entities.common;

import lombok.Getter;
import lombok.Setter;
import ro.simavi.sphinx.entities.common.enums.ConfigType;

import javax.persistence.*;

@Getter
@Setter
@Entity
@Table(name = "COMPONENT_CONFIG") // DTM_CONFIG
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@SequenceGenerator(name = "SEQ_GEN", sequenceName = "SEQ_COMPONENT_CONFIG", allocationSize = 1) // SEQ_DTM_CONFIG
@DiscriminatorColumn(
        discriminatorType = DiscriminatorType.INTEGER,
        name = "component_type_id",
        columnDefinition = "TINYINT(1)"
)
public class ConfigEntity extends SphinxEntity {

    @Column(name = "CODE")
    private String code;

    @Column(name = "NAME")
    private String name;

    @Column(name = "DESCRIPTION")
    private String description;

    @Column(name = "VALUE")
    private String value;

    @Enumerated
    @Column(name = "TYPE")
    private ConfigType type;

    @Column(name = "DEFAULT_VALUE")
    private String defaultValue;

}
