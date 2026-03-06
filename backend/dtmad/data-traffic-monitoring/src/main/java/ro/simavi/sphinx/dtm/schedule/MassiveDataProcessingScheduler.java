package ro.simavi.sphinx.dtm.schedule;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ro.simavi.sphinx.dtm.model.ConversationsModel;
import ro.simavi.sphinx.model.event.alert.MassiveDataProcessingAlertModel;
import ro.simavi.sphinx.model.event.alert.SphinxModel;
import ro.simavi.sphinx.dtm.services.MessagingSystemService;
import ro.simavi.sphinx.dtm.services.alert.MassiveDataProcessingAlertService;
import ro.simavi.sphinx.dtm.util.SphinxModelHelper;
import ro.simavi.sphinx.model.event.AlertMetadataModel;
import ro.simavi.sphinx.model.event.AlertModel;
import ro.simavi.sphinx.util.FormatHelper;

import java.util.HashMap;
import java.util.Map;

@Component
public class MassiveDataProcessingScheduler {

    private MassiveDataProcessingAlertService massiveDataProcessingAlertService;

    private MessagingSystemService messagingSystemService;

    public MassiveDataProcessingScheduler(MassiveDataProcessingAlertService massiveDataProcessingAlertService,
                                          MessagingSystemService messagingSystemService){
        this.massiveDataProcessingAlertService = massiveDataProcessingAlertService;
        this.messagingSystemService = messagingSystemService;
    }

    @Scheduled(cron = "*/60 * * * * *")
    public void buildAlert(){
        long maxLen = 0;
        HashMap<String,ConversationsModel> map = massiveDataProcessingAlertService.getConversations();

        for (Map.Entry<String,ConversationsModel> entry : map.entrySet()) {
            String key = entry.getKey();
            ConversationsModel conversationsModel = entry.getValue();

            if (conversationsModel.getBytes()>maxLen){
                maxLen = conversationsModel.getBytes();
            }
            if (conversationsModel.getBytes()> massiveDataProcessingAlertService.getThresholdAlertValue()){
                generateAlert(key, conversationsModel);
            }
        }

        System.out.println(maxLen+" " + map.size());
        massiveDataProcessingAlertService.clearConversations();
    }

    private void generateAlert(String key, ConversationsModel conversationsModel){
        String[] detailKey = key.split("@");
        if ("eth".equals(detailKey[0]) || "ip".equals(detailKey[0])|| "ipv6".equals(detailKey[0])){
            return;
        }

        SphinxModel sphinxModel = SphinxModelHelper.getSphinxModel("tshark");

        AlertMetadataModel alertMetadataModel = new AlertMetadataModel();
        alertMetadataModel.setSignatureSeverity(new String[]{"Minor"});

        AlertModel alertModel = new AlertModel();
        alertModel.setAction("allowed");
        alertModel.setSignature("MassiveDataProcessing - " + FormatHelper.humanReadableByteCountSI(conversationsModel.getBytes()));
        alertModel.setMetadata(alertMetadataModel);
        alertModel.setCategory("Massive Data Processing");

        MassiveDataProcessingAlertModel massiveDataProcessingAlert = new MassiveDataProcessingAlertModel();
        massiveDataProcessingAlert.setProtocol(detailKey[0]);
        massiveDataProcessingAlert.setSrcIp(detailKey[1]);
        massiveDataProcessingAlert.setDestIp(detailKey[2]);
        if ("udp".equals(detailKey[0]) || "tcp".equals(detailKey[0])){
            massiveDataProcessingAlert.setSrcPort(detailKey[3]);
            massiveDataProcessingAlert.setDestPort(detailKey[4]);
        }
        massiveDataProcessingAlert.setPackets(conversationsModel.getPackages());
        massiveDataProcessingAlert.setBytes(conversationsModel.getBytes());
        massiveDataProcessingAlert.setLen(FormatHelper.humanReadableByteCountSI(conversationsModel.getBytes()));
        massiveDataProcessingAlert.setTimestamp(FormatHelper.toLocalDateTime(conversationsModel.getTime()));
        massiveDataProcessingAlert.setHost(conversationsModel.getHost());

        massiveDataProcessingAlert.setSphinxModel(sphinxModel);
        massiveDataProcessingAlert.setAlert(alertModel);
        massiveDataProcessingAlert.setEventType("MassiveDataProcessingAlertModel"); // MassiveDataProcessingAlertModel

        messagingSystemService.sendAlert(massiveDataProcessingAlert);

    }

}
