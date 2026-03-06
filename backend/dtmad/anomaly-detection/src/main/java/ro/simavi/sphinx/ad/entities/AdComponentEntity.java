package ro.simavi.sphinx.ad.entities;

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
@Table(name = "AD_COMPONENT")
@SequenceGenerator(name = "SEQ_GEN", sequenceName = "SEQ_AD_COMPONENT", allocationSize = 1)
public class AdComponentEntity extends SphinxEntity {

    @Column(name = "NAME")
    private String name;

    @Column(name = "DESCRIPTION")
    private String description;

    @Column(name = "ENABLED")
    private Boolean enabled;

    @Column(name = "CODE")
    private String code;

    @Column(name = "CONFIGURABLE")
    private Boolean configurable;

}
