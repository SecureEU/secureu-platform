package ro.simavi.sphinx.dtm.services.impl;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import ro.simavi.sphinx.dtm.configuration.DTMConfigProps;
import ro.simavi.sphinx.dtm.model.AssetModel;
import ro.simavi.sphinx.dtm.model.event.MetricModel;
import ro.simavi.sphinx.dtm.services.MessagingSystemService;
import ro.simavi.sphinx.model.event.AlertEventModel;
import ro.simavi.sphinx.model.event.DtmCmdEventModel;

@Service//("KafkaMessagingSystemServiceImpl")
public class KafkaMessagingSystemServiceImpl implements MessagingSystemService {

    private KafkaTemplate<String, String> packageKafkaTemplate;

    private KafkaTemplate<String, MetricModel> metricKafkaTemplate;

    private KafkaTemplate<String, AssetModel> assetKafkaTemplate;

    private KafkaTemplate<String, AlertEventModel> alertKafkaTemplate;

    private KafkaTemplate<String, DtmCmdEventModel> dtmCmdKafkaTemplate;

    private KafkaTemplate<String, String> realTimeKafkaTemplate;

    private DTMConfigProps dtmConfigProps;

    @Value(value="${topic.package}")
    private String topicPackage;

    @Value("${topic.metric}")
    private String topicMetric;

    @Value("${topic.asset}")
    private String topicAsset;

    @Value("${topic.alert}")
    private String topicAlert;

    public KafkaMessagingSystemServiceImpl(
            @Qualifier("packageKafkaTemplate") KafkaTemplate<String, String> packageKafkaTemplate,
            @Qualifier("metricKafkaTemplate") KafkaTemplate<String, MetricModel> metricKafkaTemplate,
            @Qualifier("assetKafkaTemplate") KafkaTemplate<String, AssetModel> assetKafkaTemplate,
            @Qualifier("alertKafkaTemplate") KafkaTemplate<String,AlertEventModel> alertKafkaTemplate,
            @Qualifier("realTimeKafkaTemplate") KafkaTemplate<String, String> realTimeKafkaTemplate,
            @Qualifier("dtmCmdKafkaTemplate") KafkaTemplate<String, DtmCmdEventModel> dtmCmdKafkaTemplate,
            DTMConfigProps dtmConfigProps){
        this.packageKafkaTemplate = packageKafkaTemplate;
        this.metricKafkaTemplate = metricKafkaTemplate;
        this.assetKafkaTemplate= assetKafkaTemplate;
        this.alertKafkaTemplate = alertKafkaTemplate;
        this.dtmCmdKafkaTemplate= dtmCmdKafkaTemplate;

        this.realTimeKafkaTemplate = realTimeKafkaTemplate;

        this.dtmConfigProps=dtmConfigProps;
    }

    public void sendMessage(String message) {

        //ListenableFuture<SendResult<String, String>> future =
        packageKafkaTemplate.send(topicPackage, message);

        /*
        future.addCallback(new ListenableFutureCallback<SendResult<String, String>>() {

            @Override
            public void onSuccess(SendResult<String, String> result) {
               System.out.println("Sent message=[" + message +
                        "] with offset=[" + result.getRecordMetadata().offset() + "]");
            }
            @Override
            public void onFailure(Throwable ex) {
                System.out.println("Unable to send message=["
                        + message + "] due to : " + ex.getMessage());
            }
        });
        */
    }

    @Override
    public void sendMetric(MetricModel metricModel) {
        metricKafkaTemplate.send(topicMetric, metricModel);
    }

    @Override
    public void sendAsset(AssetModel assetModel) {
        assetKafkaTemplate.send(topicAsset, assetModel);
    }

    @Override
    public void sendAlert(AlertEventModel alertModel) {
        alertKafkaTemplate.send(topicAlert, alertModel);
    }

    @Override
    public void sendDtmCmd(DtmCmdEventModel dtmCmdEventModel) {
        dtmCmdKafkaTemplate.send("dtm-cmd", dtmCmdEventModel);
    }

    @Override
    public void sendRealtimePackage(String packageString) {
        realTimeKafkaTemplate.send("dtm-rt-"+dtmConfigProps.getInstanceKey(), packageString);
    }

    @Override
    public void sendRealtimePackageStatus(String status) {
        realTimeKafkaTemplate.send("dtm-rts-"+dtmConfigProps.getInstanceKey(), status);
    }
}
