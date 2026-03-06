package ro.simavi.sphinx.dtm.entities;

import ro.simavi.sphinx.entities.common.ConfigEntity;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("1")
public class DtmConfigEntity extends ConfigEntity {
}
