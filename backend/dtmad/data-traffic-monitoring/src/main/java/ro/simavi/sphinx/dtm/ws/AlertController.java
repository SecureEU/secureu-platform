package ro.simavi.sphinx.dtm.ws;

import io.swagger.annotations.ApiOperation;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import ro.simavi.sphinx.dtm.entities.AlertEntity;
import ro.simavi.sphinx.dtm.services.AlertService;
import ro.simavi.sphinx.model.event.AlertEventModel;

import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/alert")
public class AlertController {

    private final AlertService alertService;

    public AlertController(AlertService alertService){
        this.alertService = alertService;
    }

    @GetMapping(value = {"/all","/getAlerts"})
    @ApiOperation(value = "Return the last alert list (collected from Kafka)")
    public List<AlertEntity> getAlerts(@RequestParam(required=false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date date ,
                                       @RequestParam(defaultValue = "10") Long limit) {
        return alertService.getAlerts(date, limit);
    }


    @GetMapping(value = "/all/{prefix}")
    public Page<AlertEntity> getAlertFiler(@PathVariable(name = "prefix") String prefix,
                                              @RequestParam(defaultValue = "0") Integer pageNo,
                                              @RequestParam(defaultValue = "5") Integer pageSize){
        Page<AlertEntity> page = alertService.getAlertFiler(prefix, pageNo, pageSize);
        return page;
    }

    @GetMapping(value = "/clearAll")
    @ApiOperation(value = "Clear all last alerts")
    public List<AlertEventModel> clearAll() {
        this.alertService.removeAll();
        return alertService.getAlerts();
    }
}
