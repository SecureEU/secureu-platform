package ro.simavi.sphinx.dtm.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import ro.simavi.sphinx.dtm.model.event.MetricModel;
import ro.simavi.sphinx.dtm.services.AlertService;
import ro.simavi.sphinx.dtm.services.EventService;
import ro.simavi.sphinx.dtm.services.NetworkPersistService;
import ro.simavi.sphinx.dtm.services.StatisticsService;
import ro.simavi.sphinx.model.event.AlertEventModel;
import ro.simavi.sphinx.model.event.EventModel;
import ro.simavi.sphinx.model.event.alertStix.StixModelAlert;

import java.io.IOException;

@Component
@Profile("!agent")
public class KafkaHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaHandler.class);

    private final AlertService alertService;

    private final StatisticsService statisticsService;

    private final EventService eventService;

    @Autowired
    private NetworkPersistService networkPersistService;

    public KafkaHandler(AlertService alertService,
                        StatisticsService statisticsService,
                        EventService eventService){
        this.alertService = alertService;
        this.statisticsService = statisticsService;
        this.eventService = eventService;
    }

/*
    @KafkaListener(topics = "${topic.metric}", groupId = "${kafka.group.id}")
    public void receiveMessage(@Payload MetricModel metricModel) {
        //LOGGER.info("Received Messasge in group DTM:" + message);
        tsharkStatisticsService.collect(metricModel);
    }
*/


    @KafkaListener(topics = "${topic.metric}", groupId = "${kafka.group.id}")
    public void receiveMetric(@Payload String metricModelString) {
        //LOGGER.info("Received Messasge in group DTM:" + message);
       // tsharkStatisticsService.collect(metricModel);

        ObjectMapper mapper = new ObjectMapper();

        try {
            MetricModel metricModel = mapper.readValue(metricModelString, MetricModel.class);
            statisticsService.collect(metricModel);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @KafkaListener(topics = "${topic.alert}", groupId = "${kafka.group.id}")
    public void receiveAlert(@Payload String alertModelString) {
        ObjectMapper mapper = new ObjectMapper();

        try {
            StixModelAlert stixAlertModel = mapper.readValue(alertModelString, StixModelAlert.class);
            AlertEventModel alertModel = mapper.readValue(alertModelString, AlertEventModel.class);
            alertService.collect(alertModel, stixAlertModel);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @KafkaListener(topics = "${topic.event}", groupId = "${kafka.group.id}")
    public void receiveEvent(@Payload String eventModelString) {
        //LOGGER.info("Received Messasge in group DTM:" + message);
        // tsharkStatisticsService.collect(metricModel);

        ObjectMapper mapper = new ObjectMapper();

        try {
            EventModel eventModel = mapper.readValue(eventModelString, EventModel.class);
            eventService.collect(eventModel);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @KafkaListener(topics = "${topic.package}", groupId = "${kafka.group.id}")
    public void receiveMessage(@Payload String message) {
//        LOGGER.info("Received Messasge in group Anomaly Detection:" + message);
        String[] pcapMessage = message.split("\t",-1);
        eventService.collect(pcapMessage);
    }

    @KafkaListener(topics = "${topic.dss-dtm-record}", groupId = "${kafka.group.id}")
    public Boolean receiveNotifications() {
        return networkPersistService.persist();
    }

}
