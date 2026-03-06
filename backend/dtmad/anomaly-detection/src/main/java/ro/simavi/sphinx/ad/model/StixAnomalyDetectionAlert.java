package ro.simavi.sphinx.ad.model;

import java.io.Serializable;
import java.util.List;

public class StixAnomalyDetectionAlert implements Serializable {

    private String type;

    private String id;

    private List<StixObject> objects;

    public StixAnomalyDetectionAlert(){};

    public StixAnomalyDetectionAlert(String type, String id, List<StixObject> objects) {
        this.type = type;
        this.id = id;
        this.objects = objects;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<StixObject> getObjects() {
        return objects;
    }

    public void setObjects(List<StixObject> objects) {
        this.objects = objects;
    }
}
