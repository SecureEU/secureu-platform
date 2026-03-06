package ro.simavi.sphinx.ad.kernel.enums;

public enum KmeansAlgorithm implements AdmlAlgorithmEnum{

    HTTP("http"),
    DNS("dns");

    String name;

    KmeansAlgorithm(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
