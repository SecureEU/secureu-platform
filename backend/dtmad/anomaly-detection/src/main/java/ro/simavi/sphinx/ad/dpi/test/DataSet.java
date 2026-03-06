package ro.simavi.sphinx.ad.dpi.test;

import java.io.File;

public enum DataSet {

    FOLDER("c:" + File.separator + "dbpcap"),

    CTU_Normal_4_only_DNS_SURICATA(DataSetType.SURICATA, "CTU-Normal-4-only-DNS",
            "https://www.stratosphereips.org/datasets-normal",
            5966, 677481), // {event-count:7752,packets:5966,bytes:677481,flow-count:1785},

    CTU_Normal_4_only_DNS(DataSetType.NFSTREAM, "CTU-Normal-4-only-DNS",
            "https://www.stratosphereips.org/datasets-normal",
            5966, 677481), // {event-count:7752,packets:5966,bytes:677481,flow-count:1785}
            //Time:7131
    CTU_IoT_Malware_Capture_7_1(DataSetType.SURICATA,"CTU-IoT-Malware-Capture-7-1",
            "https://mcfp.felk.cvut.cz/publicDatasets/IoT-23-Dataset/IndividualScenarios/CTU-IoT-Malware-Capture-7-1/", // 2018-07-20-17-31-20-192.168.100.108.pcap
            11508430, 755998808), // {event-count:1379539,packets:27,bytes:93227445,flow-count:9}
            //Time:317176
    CTU_IoT_Malware_Capture_35_1(DataSetType.SURICATA,"CTU-IoT-Malware-Capture-35-1",
            "https://mcfp.felk.cvut.cz/publicDatasets/IoT-23-Dataset/IndividualScenarios/CTU-IoT-Malware-Capture-35-1", // 2018-12-21-15-33-59-192.168.1.196.pcap
            46832850, 3061457724L), //{event-count:2431960,packets:2375,bytes:2403069574,flow-count:1148}
            // Time:550521
    CTU_Malware_Capture_Botnet_45_SURICATA(DataSetType.SURICATA,"CTU-Malware-Capture-Botnet-45",
            "https://mcfp.felk.cvut.cz/publicDatasets/CTU-Malware-Capture-Botnet-45/", // botnet-capture-20110815-rbot-dos.pcap
            256712, 218271666), // {event-count:592,packets:353,bytes:177048473,flow-count:5}
    CTU_Malware_Capture_Botnet_45(DataSetType.NFSTREAM,"CTU-Malware-Capture-Botnet-45",
            "https://mcfp.felk.cvut.cz/publicDatasets/CTU-Malware-Capture-Botnet-45/",
            256712, 218271666), // {event-count:592,packets:353,bytes:177048473,flow-count:5}
            // OBS:  2 dns-flows, one of dns-flows with 334 packets
            // Time:2262
    CICDDoS2019(DataSetType.SURICATA,"CICDDoS2019","https://www.unb.ca/cic/datasets/ddos-2019.html",
            87534461 + 50885954 + 88457343 + 23905529, 48599528755L + 49185972806L + 48584775528L + 13221372195L), // {event-count:34067437,packets:103710,bytes:111366414463,flow-count:31944}
            // 818 files in 4 folders
            // Time:8176875
    MaliciousDoH_dns2tcp_Pcaps(DataSetType.SURICATA,"DoHBrw-2020"+File.separator+"MaliciousDoH-dns2tcp-Pcaps",
            "https://www.unb.ca/cic/datasets/dohbrw-2020.html",
            3188915 + 7012091 + 13170057 + 16054405, 395216029 + 1392062590 + 1713259279 + 1994672084), // {event-count:211615,packets:90404,bytes:5495209982,flow-count:90404}
            // 2198 files in 4 folders
            // OBS: 0 dns or http, so ignorable, 0 packets from/to port 53,80 or 81, but 443 (https) exists
            // Time:46143
    MaliciousDoH_dnscat2_Pcaps(DataSetType.SURICATA,"DoHBrw-2020"+File.separator+"MaliciousDoH-dnscat2-Pcaps",
            "https://www.unb.ca/cic/datasets/dohbrw-2020.html",
            830856 + 5823352 + 4356513, 110016954 + 1001459516 + 764731198); // {event-count:7302,packets:3631,bytes:1876207668,flow-count:3631}
            // 1800 files in 3 folders
            // OBS: 0 dns or http, 0 packets from/to port 53,80 or 81, but 443 (https) exists
            // Time:6247
    String name;
    String description;
    long packets;
    long bytes;
    DataSetType dataSetType;

    DataSet(String name){
       this.name = name;
    }

    DataSet(DataSetType dataSetType, String name, String description){
        this.name = name;
        this.description = description;
        this.dataSetType = dataSetType;
    }

    DataSet(DataSetType dataSetType, String name, String description, long packets, long bytes){
        this.name = name;
        this.description = description;
        this.packets = packets;
        this.bytes = bytes;
        this.dataSetType = dataSetType;
    }

    public void setName(String name){
        this.name = name;
    }

    public String getName(){
        return this.name;
    }

    public String getDescription() {
        return description;
    }

    public long getPackets() {
        return packets;
    }

    public long getBytes() {
        return bytes;
    }

    public DataSetType getDataSetType() {
        return dataSetType;
    }
}
