package ro.simavi.sphinx.dtm.util;

import lombok.Getter;
import ro.simavi.sphinx.dtm.entities.enums.ProcessType;

@Getter
public enum DtmTools {

    TSHARK("tshark", ProcessType.TSHARK, new String[]{"fields",
            "logDir","logFiles","logDuration","persistDir",
            "sftpHost", "sftpUsername", "sftpPassword", "sftpPort", "sftpPrivateKeyPath", "sftpUploadDirectory",
            "pcap","source", "excludeIP"}, Boolean.TRUE, -88L),

    SURICATA("suricata", ProcessType.SURICATA, new String[]{"log", "yaml", "networkInterfaceDiscoveyAlgorithm","source", "excludeIP"}, Boolean.TRUE, -77L),

    LOGSTASH("logstash", null, new String[]{"suricataConf","nfstreamConf"}, Boolean.TRUE, -90L),

    NFSTREAM("nfstream", null, new String[]{"source", "csv","pythonFile", "collectFunction"}, Boolean.TRUE, -91L);

    private String name;

    private String[] properties;

    private ProcessType processType;

    private boolean oneProcess = Boolean.FALSE;

    private Long oneProcessPid;

    DtmTools(String name, ProcessType processType, boolean oneProcess) {
        this.name = name;
        this.processType = processType;
        this.oneProcess = oneProcess;
    }

    DtmTools(String name, ProcessType processType, String[] properties, boolean oneProcess, Long oneProcessPid) {
        this.name = name;
        this.processType = processType;
        this.properties = properties;
        this.oneProcess = oneProcess;
        this.oneProcessPid = oneProcessPid;
    }


}
