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
@Table(name = "PORT_CATALOGUE")
@SequenceGenerator(name = "SEQ_GEN", sequenceName = "SEQ_PORT_CATALOGUE", allocationSize = 1)
public class PortCatalogueEntity extends SphinxEntity {

    @Column(name = "START_PORT_INTERVAL")
    private Long port;

    @Column(name = "END_PORT_INTERVAL")
    private Long endPortInterval;

    @Column(name = "name")
    private String name;

    @Column(name = "description")
    private String description;


}
