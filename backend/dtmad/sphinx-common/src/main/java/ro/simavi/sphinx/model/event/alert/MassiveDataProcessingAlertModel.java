package ro.simavi.sphinx.model.event.alert;

import lombok.Getter;
import lombok.Setter;
import ro.simavi.sphinx.model.event.AlertEventModel;

@Getter
@Setter
public class MassiveDataProcessingAlertModel extends AlertEventModel {

    private long bytes;

    private long packets;

    private String len;

}
