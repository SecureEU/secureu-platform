package ro.simavi.sphinx.entities.common;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

@Getter
@Setter
@MappedSuperclass
public abstract class SphinxEntity {
    @Id
    @Column(name = "ID")
    @GeneratedValue(generator = "SEQ_GEN", strategy = GenerationType.AUTO)
    private Long id;

    @Version
    @Column(name = "JPA_VERSION")
    private Long version;

    @JsonFormat(pattern = ("yyyy/MM/dd HH:mm:ss"))
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @CreationTimestamp
    @Column(name = "CREATED_DATE", nullable = false, columnDefinition = "TIMESTAMP", updatable=false)
    private LocalDateTime createdDate;

    @UpdateTimestamp
    @Column(name = "UPDATED_DATE", nullable = false, columnDefinition = "TIMESTAMP")
    private LocalDateTime updatedDate;
}