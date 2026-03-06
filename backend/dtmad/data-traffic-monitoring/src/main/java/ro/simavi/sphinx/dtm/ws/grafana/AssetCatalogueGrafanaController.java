package ro.simavi.sphinx.dtm.ws.grafana;

import org.springframework.web.bind.annotation.*;
import ro.simavi.sphinx.dtm.entities.AssetCatalogueEntity;
import ro.simavi.sphinx.dtm.services.AssetCatalogueService;
import ro.simavi.sphinx.model.grafana.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/assetcatalogue")
public class AssetCatalogueGrafanaController {

    private final AssetCatalogueService assetCatalogueService;

    public AssetCatalogueGrafanaController(AssetCatalogueService assetCatalogueService){
        this.assetCatalogueService = assetCatalogueService;
    }

    @GetMapping(value = "/getAssetCatalogue")
    public String  getAssetCatalogue() {
        return "ok";
    }


    @PostMapping(value = "/getAssetCatalogue/search")
    @ResponseBody
    public List<GrafanaSearchResult> getAssetCatalogueSearch(@RequestBody GrafanaSearch grafanaSearch) {
        List<GrafanaSearchResult> grafanaMetrics = new ArrayList<>();

        GrafanaSearchResult grafanaMetric1 = new GrafanaSearchResult();
        grafanaMetric1.setText("All");
        grafanaMetric1.setValue("All");

        grafanaMetrics.add(grafanaMetric1);
        return grafanaMetrics;
    }

    @PostMapping(value = "/getAssetCatalogue/query")
    @ResponseBody
    public List<GrafanaQueryTableItem> getAlertListQuery(@RequestBody GrafanaQuery grafanaQuery) {

        List<GrafanaQueryTableItem> grafanaQueryTableResults = new ArrayList<>();

        List<GrafanaColumn> grafanaColumns = new ArrayList<>();
        grafanaColumns.add(new GrafanaColumn("Physical Address","string"));
        grafanaColumns.add(new GrafanaColumn("Name","string"));
        grafanaColumns.add(new GrafanaColumn("Description","string"));

        List<AssetCatalogueEntity> assetCatalogueEntities = assetCatalogueService.getAssetCatalogueList();
        List<Object[]> rows = new ArrayList<>(assetCatalogueEntities.size());
        for(AssetCatalogueEntity assetCatalogueEntity: assetCatalogueEntities){
            Object[] row = new Object[3];
            row[0] = assetCatalogueEntity.getPhysicalAddress();
            row[1] = assetCatalogueEntity.getName();
            row[2] = assetCatalogueEntity.getDescription();
            rows.add(row);
        }

        GrafanaQueryTableItem grafanaQueryTableResult = new GrafanaQueryTableItem();
        grafanaQueryTableResult.setColumns(grafanaColumns);
        grafanaQueryTableResult.setRows(rows);

        grafanaQueryTableResults.add(grafanaQueryTableResult);

        return grafanaQueryTableResults;

    }

}
