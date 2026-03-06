package ro.simavi.sphinx.dtm.entities;

import lombok.Getter;
import lombok.Setter;
import ro.simavi.sphinx.entities.common.SphinxEntity;

import javax.persistence.*;

@Getter
@Setter
@Entity
@Table(name = "BLACKWEB")
@SequenceGenerator(name = "SEQ_GEN", sequenceName = "SEQ_BLACKWEB", allocationSize = 1)
public class BlackWebEntity extends SphinxEntity {

    @Column(name = "DOMAIN")
    private String domain;

    @ManyToOne
    @JoinColumn(name = "BLACKWEB_CATEGORY_ID")
    private BlackWebCategoryEntity category;

}
