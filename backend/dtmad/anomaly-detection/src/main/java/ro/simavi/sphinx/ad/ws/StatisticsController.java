package ro.simavi.sphinx.ad.ws;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ro.simavi.sphinx.ad.services.StatisticsService;

@RestController
@RequestMapping("/statistics")
public class StatisticsController {

    private final StatisticsService statisticsService;

    public StatisticsController(StatisticsService statisticsService){
        this.statisticsService = statisticsService;
    }

}
