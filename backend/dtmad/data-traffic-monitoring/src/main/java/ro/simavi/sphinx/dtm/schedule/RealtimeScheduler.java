package ro.simavi.sphinx.dtm.schedule;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ro.simavi.sphinx.dtm.services.tshark.TsharkRealTimeService;

import java.util.Date;

@Component
public class RealtimeScheduler {

    private final TsharkRealTimeService tsharkRealTimeService;

    public RealtimeScheduler(TsharkRealTimeService tsharkRealTimeService){
        this.tsharkRealTimeService = tsharkRealTimeService;
    }

    //  "*/60 * * * * *" - every 60 seconds
    @Scheduled(cron = "*/60 * * * * *")
    public void stopRealtimeProcess(){
        long now = new Date().getTime();
        Long lastTouch = tsharkRealTimeService.lastTouch();
        if (lastTouch==null){
            return;
        }

        long seconds = (now-lastTouch)/1000;

        if (seconds>60){
            tsharkRealTimeService.stop();
        }
    }

}
