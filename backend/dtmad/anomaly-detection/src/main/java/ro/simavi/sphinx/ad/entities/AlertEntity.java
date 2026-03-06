package ro.simavi.sphinx.ad.entities;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import lombok.Getter;
import lombok.Setter;
import ro.simavi.sphinx.entities.common.SphinxEntity;

import javax.persistence.*;
import java.time.LocalDateTime;


@Getter
@Setter
@Entity
@Table(name = "ALERT")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@SequenceGenerator(name = "SEQ_GEN", sequenceName = "SEQ_ALERTS", allocationSize = 1)
public class AlertEntity extends SphinxEntity {

    @JsonFormat(pattern = ("yyyy/MM/dd HH:mm:ss"))
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @Column(name = "TIMESTAMP", nullable = false, columnDefinition = "TIMESTAMP")
    private LocalDateTime timestamp;

    @Column(name = "COUNT")
    private Integer count;

    @Column(name = "SPHINX_TOOL")
    private String sphinxTool;

    @Column(name = "EVENT_TYPE")
    private String eventType;

    @Column(name = "DETAILS")
    private String details;

    @Column(name = "SOURCE_REF")
    public String sourceref;

    @Column(name = "RELATIONSHIP_TYPE")
    public String relationshipType;

    @Column(name = "TARGET_REF")
    public String targetRef;

}
