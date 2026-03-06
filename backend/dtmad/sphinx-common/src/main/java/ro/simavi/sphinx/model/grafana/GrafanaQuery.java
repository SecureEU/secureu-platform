package ro.simavi.sphinx.model.grafana;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class GrafanaQuery {

    private String app;

    private String interval;

    private String requestId;

    private long startTime;

    private List<GrafanaTarget> targets;

}
