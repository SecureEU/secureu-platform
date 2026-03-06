package ro.simavi.sphinx.ad.kernel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ro.simavi.sphinx.ad.handlers.SaveToListAlertHandler;
import ro.simavi.sphinx.ad.kernel.model.AdmlAlert;
import ro.simavi.sphinx.ad.kernel.util.AdmlAlgorithm;
import ro.simavi.sphinx.ad.services.MessagingSystemService;
import ro.simavi.sphinx.ad.ws.SimulationController;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Component
@ConditionalOnProperty(
        value = "app.scheduling.enable", havingValue = "true", matchIfMissing = true
)
public class AdmlScheduler implements Serializable {

    private static final Logger logger = LoggerFactory.getLogger(AdmlScheduler.class);

    private final MessagingSystemService messagingSystemService;

    private final SaveToListAlertHandler saveToKafkaAlertHandler;

    private final AdmlAlgorithm admlAlgorithm;

    public AdmlScheduler(AdmlAlgorithm admlAlgorithm,
                         SaveToListAlertHandler saveToKafkaAlertHandler,
                         MessagingSystemService messagingSystemService){
        this.admlAlgorithm = admlAlgorithm;
        this.saveToKafkaAlertHandler = saveToKafkaAlertHandler;
        this.messagingSystemService = messagingSystemService;
    }

    //@Scheduled(cron = "*/60 * * * * *") // la fiecare minut / 60 secunde
    @Scheduled(fixedDelay = 1000)
    public void runMLAlgoritm(){
        //start alert
        long start = new Date().getTime();

        logger.info("============= runMLAlgoritm ============= [start]" + start);
        List<AdmlAlert> list = admlAlgorithm.execute(saveToKafkaAlertHandler);

        for(AdmlAlert admlAlert: list) {
            messagingSystemService.sendAlert(admlAlert);
        }

        saveToKafkaAlertHandler.clearList();

        long end = new Date().getTime();

        logger.info("============= runMLAlgoritm ============= [end]" );
        logger.info("============= runMLAlgoritm ============= [time]" + (end-start));
        System.out.println("===> Scheduler Time: "+(end-start));

    }


}
