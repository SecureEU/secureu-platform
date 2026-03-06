package ro.simavi.sphinx.ad.entities;

import ro.simavi.sphinx.entities.common.ConfigEntity;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("2")
public class AdConfigEntity extends ConfigEntity {

    /*
     @ManyToOne
    @JoinColumn(name = "ID_COMPONENT")
    private AdComponentEntity component;
     */
}
