package ro.simavi.sphinx.dtm.entities;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Getter
@Setter
@Entity
@DiscriminatorValue("2")
public class BlackWebAlertEntity extends AlertEntity {

//    @Column(name = "ETH_SOURCE")
//    private String ethSource;
//
//    @Column(name = "HTTP_HOST")
//    private String httpHost;
//
//    @Column(name = "DNS_QRY")
//    private String dnsQry;
//
//    @Column(name = "DNS_TYPE")
//    private String type;

}
