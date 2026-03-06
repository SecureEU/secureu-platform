package ro.simavi.sphinx.dtm.ws.grafana;

import org.springframework.web.bind.annotation.*;
import ro.simavi.sphinx.model.grafana.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api")
public class MassiveDataProcessingGrafanaController {

    @PostMapping(value = "/getMassiveDataProcessingAlertList/search")
    @ResponseBody
    public List<GrafanaSearchResult> getBlackWebAlertListSearch(@RequestBody GrafanaSearch grafanaSearch) {

        List<GrafanaSearchResult> grafanaMetrics = new ArrayList<>();

        GrafanaSearchResult grafanaMetric1 = new GrafanaSearchResult();
        grafanaMetric1.setText("all");
        grafanaMetric1.setValue("all");

        return grafanaMetrics;
    }

    @PostMapping(value = "/getMassiveDataProcessingAlertList/query")
    @ResponseBody
    public List<GrafanaQueryTableItem> getBlackWebAlertListQuery(@RequestBody GrafanaQuery grafanaQuery) {

        List<GrafanaQueryTableItem> grafanaQueryTableResults = new ArrayList<>();

        List<GrafanaColumn> grafanaColumns = new ArrayList<>();
        grafanaColumns.add(new GrafanaColumn("time","string"));
        grafanaColumns.add(new GrafanaColumn("protocol","string"));
        grafanaColumns.add(new GrafanaColumn("src","string"));
        grafanaColumns.add(new GrafanaColumn("dst","string"));
        grafanaColumns.add(new GrafanaColumn("portSrc","string"));
        grafanaColumns.add(new GrafanaColumn("portDst","string"));
        grafanaColumns.add(new GrafanaColumn("packets","number"));
        grafanaColumns.add(new GrafanaColumn("len","string"));


        List<Object[]> rows = new ArrayList<>();

        String target = grafanaQuery.getTargets().get(0).getTarget();
        /* todo ??!???
        List<? extends AnomalyDetectionAlert> mdpAlerts = anomalyDetectionService.getAnomalyDetectionAlert(AnomalyDetectionType.MASSIVE_DATA_PROCESSING);
        for(AnomalyDetectionAlert anomalyDetectionAlert: mdpAlerts){
            MassiveDataProcessingAlert mdpAlert = (MassiveDataProcessingAlert)anomalyDetectionAlert;

            long targetLen = Long.parseLong(target);

            if (mdpAlert.getBytes()<targetLen){
                continue;
            }

            Object[] row = new Object[8];
            row[0] = mdpAlert.getTime();
            row[1] = mdpAlert.getProtocol();
            row[2] = mdpAlert.getSrc();
            row[3] = mdpAlert.getDst();
            row[4] = mdpAlert.getPortSrc();
            row[5] = mdpAlert.getPortDst();
            row[6] = mdpAlert.getPackets();
            row[7] = mdpAlert.getLen();
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
