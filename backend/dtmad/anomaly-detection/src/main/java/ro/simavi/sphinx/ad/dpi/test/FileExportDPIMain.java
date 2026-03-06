package ro.simavi.sphinx.ad.dpi.test;

import java.util.Date;

public class FileExportDPIMain {

    public static void main(String arg[]){
        long start = new Date().getTime();

        DataSetUtil.exportToAdmlEve(DataSet.CTU_Normal_4_only_DNS);
        //DataSetUtil.exportToAdmlEve(DataSet.CTU_IoT_Malware_Capture_7_1);
        //DataSetUtil.exportToAdmlEve(DataSet.CTU_IoT_Malware_Capture_35_1);
        //DataSetUtil.exportToAdmlEve(DataSet.CTU_Malware_Capture_Botnet_45);
        //DataSetUtil.exportToAdmlEve(DataSet.CICDDoS2019);
        //DataSetUtil.exportToAdmlEve(DataSet.MaliciousDoH_dns2tcp_Pcaps);
        //DataSetUtil.exportToAdmlEve(DataSet.MaliciousDoH_dnscat2_Pcaps);

        long end = new Date().getTime();
        System.out.println("Time:"+(end-start));
    }
}

