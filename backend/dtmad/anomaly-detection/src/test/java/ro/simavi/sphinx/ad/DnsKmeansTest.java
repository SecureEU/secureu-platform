package ro.simavi.sphinx.ad;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import ro.simavi.sphinx.ad.dpi.test.DataSet;
import ro.simavi.sphinx.ad.dpi.test.DataSetUtil;
import ro.simavi.sphinx.ad.handlers.AlertHandler;
import ro.simavi.sphinx.ad.handlers.SaveToListAlertHandler;
import ro.simavi.sphinx.ad.kernel.enums.MLAgorithm;
import ro.simavi.sphinx.ad.kernel.hbase.AdmlHBaseRDD;
import ro.simavi.sphinx.ad.kernel.model.AdmlAlert;
import ro.simavi.sphinx.ad.kernel.util.AdmlAlgorithm;
import ro.simavi.sphinx.ad.services.AdConfigService;
import ro.simavi.sphinx.model.ConfigModel;

import java.util.Date;
import java.util.List;
import java.util.Map;

@RunWith(SpringRunner.class)
@SpringBootTest
public class DnsKmeansTest {

    @Autowired
    private AdmlAlgorithm admlAlgorithm;

    @Autowired
    private AdConfigService addConfigService;

    public Map<String, ConfigModel> getFilterParameter() {
        return addConfigService.getConfigs();
    }


    @Test
    public void dnsKmennsMean(){
        long start = new Date().getTime();

        boolean justRunAlgorithm = true;

        String valueDnsKmeans = getFilterParameter().get("ad.kmeans.dns.algorithm").getValue();
        String valueHttpKmeans = getFilterParameter().get("ad.kmeans.http.algorithm").getValue();

        if (!justRunAlgorithm) {
            // prepare dates
            DataSetUtil.mergeFlows(DataSet.CTU_Normal_4_only_DNS, DataSet.CTU_Malware_Capture_Botnet_45, DataSet.CTU_Normal_4_only_DNS);

            // clear hbase
            AdmlHBaseRDD admlHBaseRDD = AdmlHBaseRDD.getInstance();
            if (!admlHBaseRDD.isOk()) {
                return;
            }
            admlHBaseRDD.truncateAdmlFlows("adml_flows");

            // read from merge file and save in hbase
            DataSetUtil.saveMergeFlowsToHBase(DataSet.CTU_Normal_4_only_DNS);
        }

//        // run dns kmeans
//        if(valueDnsKmeans.equals("false")){
//            admlAlgorithm.setAgorithms(new MLAgorithm[]{MLAgorithm.KMEANS_DNS});
//            admlAlgorithm.setTest(Boolean.TRUE);
//            admlAlgorithm.execute(new SaveToListAlertHandler(),Boolean.TRUE);
//        }

        long end = new Date().getTime();
        System.out.println("Time:"+(end-start));
    }
}
