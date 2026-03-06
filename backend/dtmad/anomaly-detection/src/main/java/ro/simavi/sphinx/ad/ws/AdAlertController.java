package ro.simavi.sphinx.ad.ws;

import io.swagger.annotations.ApiOperation;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ro.simavi.sphinx.ad.entities.AlertEntity;
import ro.simavi.sphinx.ad.services.AdAlertService;

import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/alert")
public class AdAlertController {

    private final AdAlertService adAlertService;

    public AdAlertController(AdAlertService adAlertService) {
        this.adAlertService = adAlertService;
    }

    @GetMapping(value = "/getAlerts")
    @ApiOperation(value = "Return the last alert list (collected from Kafka)")
    public List<AlertEntity> getAlerts(@RequestParam(required=false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date date ,
                                       @RequestParam(defaultValue = "10") Long limit) {
        return adAlertService.getAlerts(date, limit);
    }
}
