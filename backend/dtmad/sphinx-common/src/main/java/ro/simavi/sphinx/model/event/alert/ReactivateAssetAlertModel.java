package ro.simavi.sphinx.model.event.alert;

import lombok.Getter;
import lombok.Setter;
import ro.simavi.sphinx.model.event.AlertEventModel;

@Getter
@Setter
public class ReactivateAssetAlertModel extends AlertEventModel {

    String timeSilent;

    String lastDelay;

    String physicalAddress;

    String name;

}
