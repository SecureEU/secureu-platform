package ro.simavi.sphinx.ad.services.impl;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import ro.simavi.sphinx.ad.kernel.model.AdmlAlert;
import ro.simavi.sphinx.ad.model.*;
import ro.simavi.sphinx.ad.services.MessagingSystemService;

import java.util.*;
import java.text.SimpleDateFormat;

@Service
public class KafkaMessagingSystemServiceImpl implements MessagingSystemService {

    private KafkaTemplate<String, StixAnomalyDetectionAlert> alertKafkaTemplate;

    @Value("${topic.ad.alert}")
    private String topicAlert;

    public KafkaMessagingSystemServiceImpl(@Qualifier("alertKafkaTemplate") KafkaTemplate<String, StixAnomalyDetectionAlert> alertKafkaTemplate){
        this.alertKafkaTemplate = alertKafkaTemplate;
    }

    @Override
    public void sendAlert(AdmlAlert alertModel) {

        //String timp = String.valueOf((System.currentTimeMillis() / 1000)+((TimeZone.getDefault().getRawOffset() + TimeZone.getDefault().getDSTSavings()) / 1000));
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        
        //Here you say to java the initial timezone. This is the secret
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        
        String timp = sdf.format(calendar.getTime());
        
        String uuid = UUID.randomUUID().toString();
        String identityId = ("identity--" + UUID.randomUUID().toString());
        String alertId = ("x-sphinx-ad-alert--" + UUID.randomUUID().toString());
        String relationshipId = ("relationship--" + UUID.randomUUID().toString());

        HashMap details = new HashMap();
        details.put("title",alertModel.getTitle());
        details.put("text", alertModel.getText());
        details.put("flowId", alertModel.getFlowId());
        details.put("totalFlows", alertModel.getTotalFlows());
        details.put("username", alertModel.getUsername());
        details.put("coords", alertModel.getCoords());
        details.put("timestamp", alertModel.getTimestamp());
        details.put("algorithm", alertModel.getAlgorithm());
        details.put("protocolFlow", alertModel.getProtocolFlow());

        StixAnomalyDetectionAlert stixAnomalyDetectionAlert = new StixAnomalyDetectionAlert();
        List<StixObject> objects = new ArrayList<>();
        StixIdentity stixIdentity = new StixIdentity("identity", identityId, "2.1", timp, timp, "AD");
        StixDetectedAlert stixDetectedAlert = new StixDetectedAlert("x-sphinx-ad-alert", alertId, "2.1", timp, timp, details);
        StixRelationship stixRelationship = new StixRelationship("relationship", relationshipId, "2.1", timp, timp, identityId, "observed", alertId);


        objects.add(stixIdentity);
        objects.add(stixDetectedAlert);
        objects.add(stixRelationship);

        stixAnomalyDetectionAlert.setId("bundle--"+uuid);
        stixAnomalyDetectionAlert.setType("bundle");
        stixAnomalyDetectionAlert.setObjects(objects);
        alertKafkaTemplate.send(topicAlert, stixAnomalyDetectionAlert);
    }

}
