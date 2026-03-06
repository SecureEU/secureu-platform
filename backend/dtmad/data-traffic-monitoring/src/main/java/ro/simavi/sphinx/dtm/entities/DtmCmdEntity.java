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
@Table(name = "DTM_CMD")
@SequenceGenerator(name = "SEQ_GEN", sequenceName = "SEQ_DTM_CMD", allocationSize = 1)
public class DtmCmdEntity extends SphinxEntity {

    @Column(name = "URL")
    private String url;

    @Column(name = "METHOD")
    private String method;

    @Column(name ="INSTANCE_KEY")
    private String instanceKey;

    @Column(name ="HOSTNAME")
    private String hostname;

    @Column(name ="USERNAME")
    private String username;

}
