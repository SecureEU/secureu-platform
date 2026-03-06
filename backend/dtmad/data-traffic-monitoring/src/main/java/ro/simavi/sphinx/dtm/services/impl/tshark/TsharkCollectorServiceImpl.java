package ro.simavi.sphinx.dtm.services.impl.tshark;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ro.simavi.sphinx.dtm.services.EventService;
import ro.simavi.sphinx.dtm.services.MessagingSystemService;
import ro.simavi.sphinx.dtm.services.ToolCollectorService;
import ro.simavi.sphinx.dtm.services.tshark.TsharkStatisticsService;

import java.math.BigDecimal;

@Service
public class TsharkCollectorServiceImpl implements ToolCollectorService {

    private static final Logger logger = LoggerFactory.getLogger(TsharkCollectorServiceImpl.class);

    private final TsharkStatisticsService tsharkStatisticsService;

    private final MessagingSystemService messagingSystemService;

    private final EventService eventService;

    private BigDecimal count = BigDecimal.ZERO;

    @Value("${topic.package.enable:false}")
    private boolean topicPackageEnable;

    public TsharkCollectorServiceImpl(TsharkStatisticsService tsharkStatisticsService,
                                      MessagingSystemService messagingSystemService,
                                      EventService eventService){

        this.tsharkStatisticsService = tsharkStatisticsService;
        this.messagingSystemService = messagingSystemService;
        this.eventService = eventService;
    }

    @Override
    public void collector(String message, Boolean fromPcap) {
        String[] pcapMessage = message.split("\t",-1);
//        int i = 0;
//        for(String f:pcapMessage){
//            logger.info("field ["+i+"]="+f);
//            i++;
//        }
        count = count.add(BigDecimal.ONE);

        if( count.remainder(new BigDecimal(1000)).compareTo(BigDecimal.ZERO) == 0 ) {
            logger.info("collect more than " + count + " messages and counting...");
        }

        tsharkStatisticsService.collect(pcapMessage);
        eventService.collect(pcapMessage);

        if (topicPackageEnable) {
            messagingSystemService.sendMessage(message);
        }
    }

}
