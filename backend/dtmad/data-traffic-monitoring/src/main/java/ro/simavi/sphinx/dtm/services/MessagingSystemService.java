package ro.simavi.sphinx.dtm.services;

import ro.simavi.sphinx.dtm.model.AssetModel;
import ro.simavi.sphinx.dtm.model.event.MetricModel;
import ro.simavi.sphinx.model.event.AlertEventModel;
import ro.simavi.sphinx.model.event.DtmCmdEventModel;

public interface MessagingSystemService {

    void sendMessage(String message);

    void sendMetric(MetricModel metricModel);

    void sendAsset(AssetModel assetModel);

    void sendAlert(AlertEventModel alertModel);

    void sendDtmCmd(DtmCmdEventModel dtmCmdEventModel);

    void sendRealtimePackage(String packageString);

    void sendRealtimePackageStatus(String packageString);

}
