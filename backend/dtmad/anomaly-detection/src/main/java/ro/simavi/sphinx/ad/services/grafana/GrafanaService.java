package ro.simavi.sphinx.ad.services.grafana;

import ro.simavi.sphinx.model.grafana.GrafanaDatasource;

import java.util.List;

public interface GrafanaService {
    List<GrafanaDatasource> getDataSources();

    String getDataSourcesString();

    String importDataSource(GrafanaDatasource[] grafanaDatasourceList);
}
