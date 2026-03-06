package ro.simavi.sphinx.ad.model;

public class StixIdentity extends StixObject {

    private String name;

    public StixIdentity(){};

    public StixIdentity(String name) {
        this.name = name;
    }

    public StixIdentity(String type, String id, String spec_version, String created, String modified, String name) {
        super(type, id, spec_version, created, modified);
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
