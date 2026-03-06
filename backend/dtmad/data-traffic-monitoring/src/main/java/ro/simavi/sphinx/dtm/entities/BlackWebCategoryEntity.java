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
@Table(name = "BLACKWEB_CATEGORY")
@SequenceGenerator(name = "SEQ_GEN", sequenceName = "SEQ_BLACKWEB_CATEGORY", allocationSize = 1)
public class BlackWebCategoryEntity extends SphinxEntity {

    @Column(name = "NAME")
    private String name;

    @Column(name = "DESCRIPTION")
    private String description;

}
