package ro.simavi.sphinx.ad.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import ro.simavi.sphinx.ad.dpi.handler.DPIHandler;
import ro.simavi.sphinx.ad.services.AdAlertService;
import ro.simavi.sphinx.model.event.alertStix.StixModelAlert;

import java.io.IOException;

@Component
public class KafkaHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaHandler.class);

    @Autowired
    private NetworkHandler networkHandler;

    @Autowired
    private DPIHandler dpiHandler;

    private final AdAlertService adAlertService;

    public KafkaHandler(AdAlertService adAlertService) {
        this.adAlertService = adAlertService;
    }

    @KafkaListener(topics = "${topic.package}", groupId = "${kafka.group.id}")
    public void receiveMessage(@Payload String message) {
//        LOGGER.info("Received Messasge in group Anomaly Detection:" + message);
        networkHandler.receiveMessage(message);
    }

    @KafkaListener(topics = "${topic.nfstream}", groupId = "${kafka.group.id}")
    public void receiveEvent(@Payload String message) {
//        LOGGER.info("**************Received Messasge in group Anomaly Detection:" + message);
        dpiHandler.receiveMessage(message); // transform message into => EventModel object => ProtocolFlow object => save to hbase
    }

    @KafkaListener(topics = "${topic.hogzilla}", groupId = "${kafka.group.id}")
    public void receiveHogEvent(@Payload String message) {
//        LOGGER.info("**************R************" + message);
        dpiHandler.receiveHogMessage(message); // transform message into => EventModel object => ProtocolFlow object => save to hbase
    }

    @KafkaListener(topics = "${topic.ad.alert}", groupId = "${kafka.group.id}")
    public void receiveAlert(@Payload String  stixModelString) {
        ObjectMapper mapper = new ObjectMapper();

        try {

            StixModelAlert stixModelAlert = mapper.readValue(stixModelString, StixModelAlert.class);
            adAlertService.collect(stixModelAlert);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
