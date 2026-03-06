package ro.simavi.sphinx.model.event.alert;

import lombok.Getter;
import lombok.Setter;
import ro.simavi.sphinx.model.event.AlertEventModel;

@Getter
@Setter
public class TcpAnalysisFlagsAlertModel extends AlertEventModel {

    private long bytes;

    private String info;
}
