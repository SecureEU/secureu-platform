package ro.simavi.sphinx.ad.kernel.enums;

public enum AdmlGeneralEnum {

    BIG_PROVIDERS("bigProviders"),
    GENERAL("general"),
    MYNETS("mynets"),
    ALERT("alert");

    String name;

    AdmlGeneralEnum(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
