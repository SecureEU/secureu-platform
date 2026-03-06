package ro.simavi.sphinx.ad.model;

public class StixObject {

    private String type;

    private String id;

    private String spec_version;

    private String created;

    private String modified;


    public StixObject() {
    }

    public StixObject(String type, String id, String spec_version, String created, String modified) {
        this.type = type;
        this.id = id;
        this.spec_version = spec_version;
        this.created = created;
        this.modified = modified;
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

    public String getSpec_version() {
        return spec_version;
    }

    public void setSpec_version(String spec_version) {
        this.spec_version = spec_version;
    }

    public String getCreated() {
        return created;
    }

    public void setCreated(String created) {
        this.created = created;
    }

    public String getModified() {
        return modified;
    }

    public void setModified(String modified) {
        this.modified = modified;
    }
}
