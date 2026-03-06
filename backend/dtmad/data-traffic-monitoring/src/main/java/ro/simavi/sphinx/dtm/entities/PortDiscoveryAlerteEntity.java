package ro.simavi.sphinx.dtm.entities;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Getter
@Setter
@Entity
@DiscriminatorValue("4")
public class PortDiscoveryAlerteEntity extends AlertEntity {

}
