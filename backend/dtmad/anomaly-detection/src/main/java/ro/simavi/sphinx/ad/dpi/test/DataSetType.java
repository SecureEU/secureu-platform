package ro.simavi.sphinx.ad.dpi.test;

public enum DataSetType {

    SURICATA("suricata", new DataSetResult("eve.json", "malware-eve.json","adml.json", "malware-adml.json", "merge-adml.json")),

    NFSTREAM("suricata", new DataSetResult("adml-result.csv", "malware-eve.csv","adml.csv", "malware-adml.csv", "merge-adml.csv"));

    private DataSetResult dataSetResult;

    private String folder;

    DataSetType(String folder, DataSetResult dataSetResult){
        this.folder = folder;
        this.dataSetResult = dataSetResult;
    }

    public DataSetResult getDataSetResult() {
        return dataSetResult;
    }

    public String getFolder() {
        return folder;
    }
}
