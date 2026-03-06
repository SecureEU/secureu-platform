package ro.simavi.sphinx.dtm.entities;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Getter
@Setter
@Entity
@DiscriminatorValue("5")
public class ReactivateAssetAlertEntity extends AlertEntity {

//    @Column(name = "TIME_SILENT")
//    String timeSilent;
//
//    @Column(name = "LAST_DELAY")
//    String lastDelay;
//
//    @Column(name = "PHYSICAL_ADDRESS")
//    String physicalAddress;
//
//    @Column(name = "NAME")
//    String name;
}
