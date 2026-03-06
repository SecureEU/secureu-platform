package ro.simavi.sphinx.ad.dpi.test;


import ro.simavi.sphinx.ad.kernel.test.AdmlMain;
import ro.simavi.sphinx.ad.kernel.enums.MLAgorithm;
import ro.simavi.sphinx.ad.kernel.hbase.AdmlHBaseRDD;

import java.util.Date;

public class DnsKmeansMain {

    public void test(){
        long start = new Date().getTime();

        boolean justRunAlgorithm = true;

        if (!justRunAlgorithm) {
            // prepare dates
            DataSetUtil.mergeFlows(DataSet.CTU_Normal_4_only_DNS, DataSet.CTU_Malware_Capture_Botnet_45, DataSet.CTU_Normal_4_only_DNS);

            // clear hbase
            AdmlHBaseRDD admlHBaseRDD = AdmlHBaseRDD.getInstance();
            if (!admlHBaseRDD.isOk()) {
                System.out.println("==================================HBASE PROBLEM!!!!!!!!!!!!!!!!!================================");
                return;
            }
            admlHBaseRDD.truncateAdmlFlows("adml_flows");

            // read from merge file and save in hbase
            DataSetUtil.saveMergeFlowsToHBase(DataSet.CTU_Normal_4_only_DNS);
        }

        // run dns kmeans
        AdmlMain adml = new AdmlMain(new MLAgorithm[]{MLAgorithm.KMEANS_DNS}, true);
        adml.execute();

        long end = new Date().getTime();
        System.out.println("Time:"+(end-start));
    }

    public static void main(String arg[]){
        DnsKmeansMain dnsKmeansMain = new DnsKmeansMain();
        dnsKmeansMain.test();
    }
}
