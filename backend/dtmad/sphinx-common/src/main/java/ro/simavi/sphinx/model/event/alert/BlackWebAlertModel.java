package ro.simavi.sphinx.model.event.alert;

import lombok.Getter;
import lombok.Setter;
import ro.simavi.sphinx.model.event.AlertEventModel;

@Getter
@Setter
public class BlackWebAlertModel extends AlertEventModel {

    String ethSource;

    String httpHost;

    String dnsQry;

    String type;

}
