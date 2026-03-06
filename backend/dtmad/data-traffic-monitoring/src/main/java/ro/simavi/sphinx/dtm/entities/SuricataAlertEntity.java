package ro.simavi.sphinx.dtm.entities;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@Getter
@Setter
@DiscriminatorValue("1")
public class SuricataAlertEntity extends AlertEntity {

    @Column(name = "SOURCE_REF")
    private String sourceRef;

    @Column(name = "RELATIONSHIP_TYPE")
    private String relationshipType;

    @Column(name = "TARGET_REF")
    private String targetRef;

    @Column(name = "LOCATION")
    private String location;

}
