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
@Table(name = "DTM_PROCESS_FILTER")
@SequenceGenerator(name = "SEQ_GEN", sequenceName = "SEQ_DTM_PROCESS_FILTER", allocationSize = 1)
public class ProcessFilterEntity extends SphinxEntity {

    @Column(name = "NAME")
    private String name;

    @Column(name = "COMMAND")
    private String command;

    @Column(name = "DESCRIPTION")
    private String description;

    @Column(name = "CODE")
    private String code;

    @Column(name = "CAN_DELETE")
    private Boolean canDelete;
}
