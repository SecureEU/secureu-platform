package ro.simavi.sphinx.ad.kernel.util;

import com.typesafe.config.Config;
import org.apache.spark.SparkContext;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import ro.simavi.sphinx.ad.SpringContext;
import ro.simavi.sphinx.ad.configuration.AdConfigProps;
import ro.simavi.sphinx.ad.handlers.AlertHandler;
import ro.simavi.sphinx.ad.kernel.enums.AdTools;
import ro.simavi.sphinx.ad.kernel.enums.MLAgorithm;
import ro.simavi.sphinx.ad.kernel.hbase.AdmlHBaseRDD;
import ro.simavi.sphinx.ad.kernel.initiate.AdmlInitiate;
import ro.simavi.sphinx.ad.kernel.kmeans.DnsKMeansConfigBuilder;
import ro.simavi.sphinx.ad.kernel.kmeans.HttpKMeansConfigBuilder;
import ro.simavi.sphinx.ad.kernel.kmeans.dns.AdmlDNS;
import ro.simavi.sphinx.ad.kernel.kmeans.http.AdmlHTTP;
import ro.simavi.sphinx.ad.kernel.model.AdmlAlert;
import ro.simavi.sphinx.ad.kernel.prepare.AdmlPrepare;
//import ro.simavi.sphinx.ad.scala.kernel.util.SFlowExecute;
import ro.simavi.sphinx.ad.services.AdConfigService;
import ro.simavi.sphinx.ad.services.MessagingSystemService;
import ro.simavi.sphinx.model.ConfigModel;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class AdmlAlgorithm extends Thread implements Serializable {

    private static final Logger logger = LoggerFactory.getLogger(AdmlAlgorithm.class);

    private final JavaSparkContext sparkContext;

    private HashMap<MLAgorithm, Boolean> agorithmBooleanHashMap;

    private AdmlHBaseRDD admlHBaseRDD;

    private final AdConfigService adConfigService;

    private boolean test = Boolean.FALSE;

    private AlertHandler alertHandler;

    @Autowired
    private SparkContext scalaSparkContext;

    @Autowired
    private MessagingSystemService messagingSystemService;

    @Autowired
    private AdConfigProps adConfigProps;

    public AdmlAlgorithm(JavaSparkContext sparkContext, AdConfigService adConfigService){
        this.sparkContext = sparkContext;
        this.adConfigService = adConfigService;

        this.updateAlgorithms();
    }

    private void updateAlgorithms(){
        Map<String, ConfigModel> configModelMap = adConfigService.getConfigs();

        String dnsKmeansDisable = configModelMap.get("ad.kmeans.dns.algorithm").getValue(); // isDisable
        String httpKmeansDisable = configModelMap.get("ad.kmeans.http.algorithm").getValue();

        List<MLAgorithm> mlAgorithmsList = new ArrayList<>();
        if(dnsKmeansDisable.equals("false")){
            mlAgorithmsList.add(MLAgorithm.KMEANS_DNS);
        }
        if (httpKmeansDisable.equals("false")){
            mlAgorithmsList.add(MLAgorithm.KMEANS_HTTP);
        }

        MLAgorithm[] mlAgorithms = new MLAgorithm[mlAgorithmsList.size()];
        mlAgorithms = mlAgorithmsList.toArray(mlAgorithms);

        this.setAgorithms(mlAgorithms);
    }

    public void setTest(Boolean test){
        this.test = test;
    }

    public void setAlertHandler(AlertHandler alertHandler){
        this.alertHandler = alertHandler;
    }

    public void setAgorithms(MLAgorithm[] mlAgorithms){
        this.agorithmBooleanHashMap = new HashMap<>();
        for(MLAgorithm mlAgorithm: mlAgorithms){
            agorithmBooleanHashMap.put(mlAgorithm, Boolean.TRUE);
        }
    }

    private boolean isEnable(MLAgorithm mlAgorithm){
        Boolean enable =  agorithmBooleanHashMap.get(mlAgorithm);
        if (enable==null){
            return Boolean.FALSE;
        }
        return enable;
    }

    public void run(){
        execute(this.alertHandler, this.test);

        if (this.test) {
            // Stop Spark
            //sparkContext.stop();

            // Close the HBase Connection
            //getAdmlHBaseRDD().close();
        }
    }

    public synchronized List<AdmlAlert> execute(AlertHandler alertHandler, boolean test){
        try {
            admlHBaseRDD = AdmlHBaseRDD.getInstance();
            // Get the HBase RDD
            if (!admlHBaseRDD.isOk()){
                return new ArrayList<>();
            }

            // AdmlRDD
            JavaPairRDD admlRDD = admlHBaseRDD.connectFlows(sparkContext);

            // Initiate AD-ML
            AdmlInitiate.initiate();

            // Prepare the data
            AdmlPrepare.prepare(admlRDD);

            this.updateAlgorithms(); //probabil ar trebuie cache-uit si actualizat doar la nevoie....
            if (isEnable(MLAgorithm.KMEANS_DNS)) {
                // Run algorithms for DNS protocol
                AdmlDNS.run(alertHandler, new DnsKMeansConfigBuilder(adConfigService).getAdmlKMeansConfig(), admlRDD, sparkContext, test);
            }

            if (isEnable(MLAgorithm.KMEANS_HTTP)) {
                // Run algorithms for HTTP protocol
                AdmlHTTP.run(alertHandler, new HttpKMeansConfigBuilder(adConfigService).getAdmlKMeansConfig(), admlRDD, sparkContext, test);
            }

            // ============================ Run algorithms for SFlows ============================

            Environment environment = SpringContext.getBean(Environment.class);
            String hbaseZookeeperQuorum = environment.getProperty("hbase.zookeeper.quorum");
            String clientPortString = environment.getProperty("hbase.zookeeper.property.clientPort");
            Integer clientPort = 2181;
            try{
                clientPort = Integer.parseInt(clientPortString);
            }catch (Exception e){

            }

            Map<String, Object> map = adConfigProps.getMap(AdTools.SFLOW);
            Config config = adConfigService.getMergeConfig(map);

//           SFlowExecute sFlowExecute = new SFlowExecute();
//           sFlowExecute.run(config, scalaSparkContext, messagingSystemService, hbaseZookeeperQuorum, clientPort);

            return alertHandler.getList();
        } catch (IOException e) {
           logger.error(e.getMessage());
        }
        return new ArrayList<>();
    }

    public List<AdmlAlert> execute(AlertHandler alertHandler) {
       return execute(alertHandler, Boolean.TRUE);
    }

    public AdmlHBaseRDD getAdmlHBaseRDD(){
        return admlHBaseRDD;
    }


}
