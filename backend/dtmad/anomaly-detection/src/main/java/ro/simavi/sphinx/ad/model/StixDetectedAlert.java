package ro.simavi.sphinx.ad.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StixDetectedAlert extends StixObject{

    private HashMap details;


    public StixDetectedAlert(HashMap details) {
        this.details = details;
    }

    public StixDetectedAlert(String type, String id, String spec_version, String created, String modified, HashMap details) {
        super(type, id, spec_version, created, modified);
        this.details = details;
    }

    public HashMap getDetails() {
        return details;
    }

    public void setDetails(HashMap details) {
        this.details = details;
    }
}
