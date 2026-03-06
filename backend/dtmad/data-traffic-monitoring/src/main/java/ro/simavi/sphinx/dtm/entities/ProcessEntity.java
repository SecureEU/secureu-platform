package ro.simavi.sphinx.dtm.entities;

import lombok.Getter;
import lombok.Setter;
import ro.simavi.sphinx.dtm.entities.enums.ProcessType;
import ro.simavi.sphinx.entities.common.SphinxEntity;

import javax.persistence.*;

@Getter
@Setter
@Entity
@Table(name = "DTM_PROCESS")
@SequenceGenerator(name = "SEQ_GEN", sequenceName = "SEQ_DTM_PROCESS", allocationSize = 1)
public class ProcessEntity extends SphinxEntity {

    @Column(name = "INTERFACE_NAME")
    private String interfaceName;

    @Column(name = "INTERFACE_DISPLAY_NAME")
    private String interfaceDisplayName;

    @Column(name = "INTERFACE_FULL_NAME")
    private String interfaceFullName;

    @Column(name = "DESCRIPTION")
    private String description;

    @Column(name = "ACTIVE")
    private Boolean active;

    @Column(name = "ENABLED")
    private Boolean enabled;

    @ManyToOne
    @JoinColumn(name = "FILTER_ID")
    private ProcessFilterEntity filter;

    @ManyToOne
    @JoinColumn(name = "INSTANCE_ID")
    private InstanceEntity instance;

    @Enumerated
    @Column(name = "TYPE")
    private ProcessType processType;

}
