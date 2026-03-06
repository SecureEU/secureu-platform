package ro.simavi.sphinx.model.grafana;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class GrafanaQueryTimeSeriesItem {

    private String target;

    private List<Object[]> datapoints;

}
