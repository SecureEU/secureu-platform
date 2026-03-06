package ro.simavi.sphinx.dtm.ws.suricata;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ro.simavi.sphinx.dtm.services.suricata.SuricataStatisticsService;

import java.util.HashMap;

@RestController
@RequestMapping("/suricata/statistics")
public class SuricataStatisticsController {

    private final SuricataStatisticsService statisticsService;

    public SuricataStatisticsController(SuricataStatisticsService statisticsService){
        this.statisticsService = statisticsService;
    }

    @GetMapping(value = "/getDecoderStatistics")
    public HashMap<String, String> getDecoderStatistics() {
        return statisticsService.getDecoderStatistics();
    }

    @GetMapping(value = "/getDecoderPerInstanceStatistics")
    public HashMap<String, HashMap<String, String>> getDecoderPerInstanceStatistics() {
        return statisticsService.getDecoderPerInstanceStatistics();
    }

}
