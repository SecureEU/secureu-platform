package ro.simavi.sphinx.ad.kernel.enums;

public enum AdmlReputationEnum {

    BIG_PROVIDERS("BigProvider"),
    PROXY_SERVER("ProxyServer"),
    TTALKER("TTalker"),
    OS_REPO("OSRepo"),
    MX("MX");

    String name;

    AdmlReputationEnum(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
