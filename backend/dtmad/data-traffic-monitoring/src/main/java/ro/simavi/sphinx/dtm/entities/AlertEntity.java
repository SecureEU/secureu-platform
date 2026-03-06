package ro.simavi.sphinx.dtm.entities;

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
@DiscriminatorColumn(
        discriminatorType = DiscriminatorType.INTEGER,
        name = "alert_type_id",
        columnDefinition = "BIGINT"
)
public class AlertEntity extends SphinxEntity {

    @JsonFormat(pattern = ("yyyy/MM/dd HH:mm:ss"))
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @Column(name = "TIMESTAMP", nullable = false, columnDefinition = "TIMESTAMP")
    private LocalDateTime timestamp;

    @Column(name = "COUNT")
    private Integer count;

    @Column(name = "SPHINX_TOOL")
    private String sphinxTool;

    @Column(name = "PROTOCOL")
    private String protocol;

    @Column(name = "HOST")
    private String host;

    @Column(name = "SRC_IP")
    private String srcIp;

    @Column(name = "SRC_PORT")
    private String srcPort;

    @Column(name = "DEST_IP")
    private String destIp;

    @Column(name = "DEST_PORT")
    private String destPort;

    @Column(name = "EVENT_TYPE")
    private String eventType;

    @Column(name = "SIGNATURE")
    private String signature;

    @Column(name = "SEVERITY")
    private String severity;

    @Column(name = "CATEGORY")
    private String category;

    @Column(name = "ACTION")
    private String action;

    @Column(name = "SIGNATURE_SEVERITY")
    private String signatureSeverity;

    @Column(name = "DETAILS")
    private String details;
}
