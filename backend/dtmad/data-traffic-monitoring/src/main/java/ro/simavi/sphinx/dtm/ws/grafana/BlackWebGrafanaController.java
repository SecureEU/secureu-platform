package ro.simavi.sphinx.dtm.ws.grafana;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import ro.simavi.sphinx.dtm.entities.BlackWebCategoryEntity;
import ro.simavi.sphinx.dtm.services.BlackWebCategoryService;
import ro.simavi.sphinx.model.grafana.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api")
public class BlackWebGrafanaController {

    private final BlackWebCategoryService blackWebCategoryService;

    @Autowired
    public BlackWebGrafanaController(BlackWebCategoryService blackWebCategoryService){
        this.blackWebCategoryService = blackWebCategoryService;
    }

    @PostMapping(value = "/getBlackWebAlertList/search")
    @ResponseBody
    public List<GrafanaSearchResult> getBlackWebAlertsSearch(@RequestBody GrafanaSearch grafanaSearch) {

        List<GrafanaSearchResult> grafanaMetrics = new ArrayList<>();

        List<BlackWebCategoryEntity> blackWebCategoryEntities = blackWebCategoryService.getList();
        for(BlackWebCategoryEntity blackWebCategoryEntity: blackWebCategoryEntities){
            GrafanaSearchResult grafanaMetric = new GrafanaSearchResult();
            grafanaMetric.setText(blackWebCategoryEntity.getName());
            grafanaMetric.setValue(blackWebCategoryEntity.getDescription());
            grafanaMetrics.add(grafanaMetric);

        }

        return grafanaMetrics;
    }

    @PostMapping(value = "/getBlackWebAlertList/query")
    @ResponseBody
    public List<GrafanaQueryTableItem> getBlackWebAlertsQuery(@RequestBody GrafanaQuery grafanaQuery) {

        List<GrafanaQueryTableItem> grafanaQueryTableResults = new ArrayList<>();

        List<GrafanaColumn> grafanaColumns = new ArrayList<>();
        grafanaColumns.add(new GrafanaColumn("time","string"));
        grafanaColumns.add(new GrafanaColumn("ethSource","string"));
        grafanaColumns.add(new GrafanaColumn("httpHost","string"));
        grafanaColumns.add(new GrafanaColumn("ipDest","string"));
        grafanaColumns.add(new GrafanaColumn("dnsQry","string"));
        grafanaColumns.add(new GrafanaColumn("type","string"));

        List<Object[]> rows = new ArrayList<>();
/*  /* todo ??!???
        String target = grafanaQuery.getTargets().get(0).getTarget();
        List<? extends AnomalyDetectionAlert> blackWebAlerts = anomalyDetectionService.getAnomalyDetectionAlert(AnomalyDetectionType.BLACK_WEB);
        for(AnomalyDetectionAlert anomalyDetectionAlert: blackWebAlerts){
            BlackWebAlert blackWebAlert = (BlackWebAlert)anomalyDetectionAlert;
            if (!target.equals(blackWebAlert.getType())){
                continue;
            }
            Object[] row = new Object[6];
            row[0] = blackWebAlert.getTime();
            row[1] = blackWebAlert.getEthSource();
            row[2] = blackWebAlert.getHttpHost();
            row[3] = blackWebAlert.getIpDest();
            row[4] = blackWebAlert.getDnsQry();
            row[5] = blackWebAlert.getType();
            rows.add(row);
        }
*/
        GrafanaQueryTableItem grafanaQueryTableResult = new GrafanaQueryTableItem();
        grafanaQueryTableResult.setColumns(grafanaColumns);
        grafanaQueryTableResult.setRows(rows);

        grafanaQueryTableResults.add(grafanaQueryTableResult);

        return grafanaQueryTableResults;

    }

}
