package ro.simavi.sphinx.dtm.schedule;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ro.simavi.sphinx.dtm.model.event.MetricModel;
import ro.simavi.sphinx.model.event.alert.SphinxModel;
import ro.simavi.sphinx.dtm.services.MessagingSystemService;
import ro.simavi.sphinx.dtm.services.tshark.TsharkStatisticsService;
import ro.simavi.sphinx.dtm.util.NetworkHelper;
import ro.simavi.sphinx.dtm.util.SphinxModelHelper;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Component
public class StatisticsScheduler {

    private final MessagingSystemService messagingSystemService;

    private final TsharkStatisticsService tsharkStatisticsService;

    public StatisticsScheduler(MessagingSystemService messagingSystemService, TsharkStatisticsService tsharkStatisticsService){
        this.messagingSystemService = messagingSystemService;
        this.tsharkStatisticsService = tsharkStatisticsService;
    }

    //  "*/10 * * * * *" - every 10 seconds
   // @Scheduled(cron = "${tshark.statistics.scheduler}")
    public void buildMetric(){

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        /*todo avoid ConcurrentModificationException */
        SphinxModel sphinxModel = SphinxModelHelper.getSphinxModel("tshark");

        Map<String,Object> data = new HashMap<>();
        data.put("traffic",tsharkStatisticsService.getProtocolStatistics());
        data.put("user",tsharkStatisticsService.getUsernameStatistics());

        data.put("ethernet",tsharkStatisticsService.getPackageEthernetStatistics());
        data.put("ipv4",tsharkStatisticsService.getPackageIPv4Statistics());
        data.put("ipv6",tsharkStatisticsService.getPackageIPv6Statistics());
        data.put("tcp",tsharkStatisticsService.getPackageTCPStatistics());
        data.put("udp",tsharkStatisticsService.getPackageUDPStatistics());

        data.put("agregation", Boolean.FALSE);

        MetricModel metricModel = new MetricModel();
        metricModel.setSphinxModel(sphinxModel);
        metricModel.setHost(NetworkHelper.getHostName());
        metricModel.setUsername( NetworkHelper.getUsername());
        metricModel.setTimestamp(LocalDateTime.now());
        metricModel.setVersion("1");
        metricModel.setEventKind("metric");
        metricModel.setType("tshark");
        metricModel.setStats(data);
        messagingSystemService.sendMetric(metricModel);
    }

}
