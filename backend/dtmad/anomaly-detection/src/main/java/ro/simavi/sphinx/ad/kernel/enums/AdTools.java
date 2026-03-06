package ro.simavi.sphinx.ad.kernel.enums;

public enum AdTools {

    SFLOW("sflow"),

    KMEANS("kmeans");

    String name;

    AdTools(String name){
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
