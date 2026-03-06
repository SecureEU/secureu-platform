package ro.simavi.sphinx.dtm.entities;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import ro.simavi.sphinx.entities.common.SphinxEntity;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "ASSETS")
@SequenceGenerator(name = "SEQ_GEN", sequenceName = "SEQ_ASSETS", allocationSize = 1)
public class AssetCatalogueEntity extends SphinxEntity {

    @Column(name = "physical_address")
    @NotBlank
    private String physicalAddress;

    @Column(name = "asset_name")
    private String name;

    @Column(name = "asset_desc")
    private String description;

    @JsonFormat(pattern = ("yyyy/MM/dd HH:mm:ss"))
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @CreationTimestamp
    @Column(name = "LAST_TOUCH_DATE", nullable = false, columnDefinition = "TIMESTAMP")
    private LocalDateTime lastTouchDate;

    @Column(name = "ip")
    private String ip;

    @Transient
    @JsonIgnore
    private Boolean touch;

    @Column(name = "LAST_DELAY")
    private Long lastDelay;

    @Column(name = "TYPE_ID")
    private Long typeId;

    @Column(name = "ID_ALERT")
    private String idAlert;

}
