package ro.simavi.sphinx.model.grafana;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class GrafanaQueryTableItem {

    private String type = "table";

    private List<GrafanaColumn> columns;

    private List<Object[]> rows;

}
