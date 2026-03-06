package ro.simavi.sphinx.dtm.model.enums;

public enum ConfigCode {

   // TCP_ANALYSIS_FLAG_ALERT_ENABLE("tcp.analysis.flag.alert.enable"),

    MASSIVE_DATA_PROCESSING_ALERT_ENABLE("massive.data.processing.alert.enable"),

    MASSIVE_DATA_PROCESSING_ALERT_THRESHOLD("massive.data.processing.alert.threshold"),

    PORT_DISCOVERY_ALERT_ENABLE("port.discovery.alert.enable"),

    BLACKWEB_ALERT_ENABLE("blackweb.alert.enable");

    private String code;

    ConfigCode(String code){
        this.code = code;
    }

    public String getCode(){
        return code;
    }
}
