package ro.simavi.sphinx.dtm.entities;

import lombok.Getter;
import lombok.Setter;
import ro.simavi.sphinx.entities.common.SphinxEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

@Getter
@Setter
@Entity
@Table(name = "DTM_INSTANCE")
@SequenceGenerator(name = "SEQ_GEN", sequenceName = "SEQ_DTM_INSTANCE", allocationSize = 1)
public class InstanceEntity extends SphinxEntity {

    @Column(name = "NAME")
    private String name;

    @Column(name = "KEY")
    private String key;

    @Column(name = "DESCRIPTION")
    private String description;

    @Column(name = "ENABLED")
    private Boolean enabled;

    @Column(name = "URL")
    private String url;

    @Column(name = "IS_MASTER")
    private Boolean master;

    @Column(name = "HAS_TSHARK")
    private Boolean hasTshark;

    @Column(name = "HAS_SURICATA")
    private Boolean hasSuricata;

}
