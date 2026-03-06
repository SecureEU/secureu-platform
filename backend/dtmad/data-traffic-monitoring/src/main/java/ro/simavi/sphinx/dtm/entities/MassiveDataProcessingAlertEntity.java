package ro.simavi.sphinx.dtm.entities;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Getter
@Setter
@Entity
@DiscriminatorValue("3")
public class MassiveDataProcessingAlertEntity extends AlertEntity {

//    @Column(name = "BYTES")
//    private long bytes;
//
//    @Column(name = "PACKETS")
//    private long packets;
//
//    @Column(name = "LENGHT")
//    private String len;
}
