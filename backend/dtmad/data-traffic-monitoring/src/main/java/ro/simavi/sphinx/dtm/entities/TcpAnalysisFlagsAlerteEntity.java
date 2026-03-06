package ro.simavi.sphinx.dtm.entities;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Getter
@Setter
@Entity
@DiscriminatorValue("6")
public class TcpAnalysisFlagsAlerteEntity extends AlertEntity {

//    @Column(name = "BYTES")
//    private long bytes;
//
//    @Column(name = "INFO")
//    private String info;
}
