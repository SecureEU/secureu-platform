package ro.simavi.sphinx.ad.kernel.test;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaSparkContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ro.simavi.sphinx.ad.kernel.enums.MLAgorithm;

public class AdmlMain{

    private static final Logger logger = LoggerFactory.getLogger(AdmlMain.class);

    private static MLAgorithm[] mlAgorithms = new MLAgorithm[]{MLAgorithm.KMEANS_DNS, MLAgorithm.KMEANS_HTTP};

    private Boolean test;

    public  AdmlMain(MLAgorithm[] mlAgorithms, boolean test){
        this.mlAgorithms = mlAgorithms;
        this.test = test;
    }

    public static void main(String arg[]){
        AdmlMain adml = new AdmlMain(mlAgorithms, Boolean.FALSE);
        adml.execute();
    }

    public void execute(){

        SparkConf sparkConf = new SparkConf()
            .setMaster("local[*]")
            .setAppName("AD-ML")
            .set("spark.executor.memory", "4g")
            .set("spark.default.parallelism", "160"); // 160

        JavaSparkContext sparkContext = new JavaSparkContext(sparkConf);

//        AdmlAlgorithm admlAlgorithm = new AdmlAlgorithm(sparkContext, null);
//        admlAlgorithm.setAgorithms(mlAgorithms);
//        admlAlgorithm.setTest(this.test);
//        admlAlgorithm.setAlertHandler(new PrintAlertHandler());
//        admlAlgorithm.start();

        // ============================ Run algorithms for SFlows ============================

        //*todo*/ //JavaPairRDD admlRDDSFlow = admlHBaseRDD.connectSFlow(sparkContext);
        //*todo*/ //AdlmSFlow.run(admlRDDSFlow,sparkContext);

        //*todo*/ //JavaPairRDD admlRDDHistograms = admlHBaseRDD.connectHistograms(sparkContext);
        //*todo*/ //AdlmSFlowHistograms.run(admlRDDHistograms,sparkContext);

    }

}
