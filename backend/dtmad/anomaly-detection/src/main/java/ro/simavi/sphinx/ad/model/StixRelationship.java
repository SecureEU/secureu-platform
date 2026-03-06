package ro.simavi.sphinx.ad.model;

public class StixRelationship extends StixObject{

    private String source_ref;

    private String relationship_type;

    private String target_ref;

    public StixRelationship(){};

    public StixRelationship(String source_ref, String relationship_type, String target_ref) {
        this.source_ref = source_ref;
        this.relationship_type = relationship_type;
        this.target_ref = target_ref;
    }

    public StixRelationship(String type, String id, String spec_version, String created, String modified, String source_ref, String relationship_type, String target_ref) {
        super(type, id, spec_version, created, modified);
        this.source_ref = source_ref;
        this.relationship_type = relationship_type;
        this.target_ref = target_ref;
    }

    public String getSource_ref() {
        return source_ref;
    }

    public void setSource_ref(String source_ref) {
        this.source_ref = source_ref;
    }

    public String getRelationship_type() {
        return relationship_type;
    }

    public void setRelationship_type(String relationship_type) {
        this.relationship_type = relationship_type;
    }

    public String getTarget_ref() {
        return target_ref;
    }

    public void setTarget_ref(String target_ref) {
        this.target_ref = target_ref;
    }
}
