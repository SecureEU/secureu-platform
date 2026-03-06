package ro.simavi.sphinx.ad.kernel.kmeans.http;

import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ro.simavi.sphinx.ad.dpi.flow.model.HttpFlow;
import ro.simavi.sphinx.ad.dpi.flow.model.ProtocolFlow;
import ro.simavi.sphinx.ad.handlers.AlertHandler;
import ro.simavi.sphinx.ad.kernel.event.AdmlEvent;
import ro.simavi.sphinx.ad.kernel.event.AdmlSignature;
import ro.simavi.sphinx.ad.kernel.kmeans.AdmlKMeans;
import ro.simavi.sphinx.ad.kernel.kmeans.AdmlKMeansConfig;
import ro.simavi.sphinx.ad.kernel.model.AdmlAlert;
import ro.simavi.sphinx.ad.kernel.model.algorithm.kmeans.HttpKmeansAlgorithmDetails;
import ro.simavi.sphinx.ad.kernel.util.AdmlDateUtil;
import ro.simavi.sphinx.ad.kernel.util.AdmlFlow;

import java.util.ArrayList;
import java.util.List;

/*
    // https://github.com/isikerhan/kmeans-spark/blob/master/src/main/java/com/isikerhan/sparkexamples/ml/kmeans/KMeans.java
    // https://tmilinovic.wordpress.com/2014/11/10/detecting-abnormal-data-using-k-means-clustering-2/
    // https://www.slideshare.net/cloudera/anomaly-detection-with-apache-spark-2
    // https://github.com/openimaj/openimaj/blob/master/core/core-math/src/main/java/org/openimaj/math/statistics/normalisation/ZScore.java

    //https://www.unb.ca/cic/datasets/ids-2018.html

 */
public class AdmlHTTP extends AdmlKMeans {

    private static final Logger logger = LoggerFactory.getLogger(AdmlHTTP.class);

    private boolean test;

    public AdmlHTTP(AlertHandler alertHandler, AdmlKMeansConfig admlKMeansConfig, JavaSparkContext sparkContext, boolean test){
        super(alertHandler, admlKMeansConfig, "HTTP", sparkContext);
        this.setAdmlSignatureList(getAdmlSignatureList());
        this.test = test;
    }

    protected List<AdmlSignature> getAdmlSignatureList(){
        List<AdmlSignature> admlSignatureList = new ArrayList<>();
        admlSignatureList.add(new AdmlSignature(3,"AD: Suspicious HTTP flow identified by K-Means clustering",2,1,826000101d,826).saveHBase());
        return admlSignatureList;
    }

    public static void run(AlertHandler alertHandler, AdmlKMeansConfig admlKMeansConfig,  JavaPairRDD<ImmutableBytesWritable,Result> admlRDD, JavaSparkContext sparkContext, Boolean test) {
        AdmlHTTP admlHTTP = new AdmlHTTP(alertHandler, admlKMeansConfig, sparkContext, test);
        admlHTTP.kmeans(admlRDD);
    }

    protected boolean getFilterCondition(AdmlFlow x){
        // filter:
        //      flow:lower_port==80 or flow:upper_port=80
        //      flow:lower_port==81 or flow:upper_port=81
        //      flow:packets>1
        //      generated in last 100 Minutes
        boolean hasPort = false;
        try {
            hasPort = x.get("flow:lower_port").equals("80") || x.get("flow:upper_port").equals("80") ||
                    x.get("flow:lower_port").equals("81") || x.get("flow:upper_port").equals("81");
        }catch (Exception e){
            logger.error("flow:lower_port null");
        }
        boolean hasPackets = false;
        try {
            hasPackets = Double.parseDouble(x.get("flow:packets")) > 1;
        }catch (Exception e){
            logger.error("flow:packets null");
        }

        boolean isOld = !test && Long.parseLong(x.get("flow:id").split("\\.")[0])>=(System.currentTimeMillis()-6000000);
        // todo: trebuie in id sa se puna si timestamp-ul, de forma: <timestamp>.<flowId>; acum e doar <flowId>
//        boolean isOld = false;
        boolean isOk = hasPort && hasPackets && !isOld;
//        if (!isOk){
//            logger.info("=======> "+ hasPort +" " + hasPackets + " " + isOld);
//        }
        return isOk;
    }

    protected String getHostname(AdmlFlow flow){
        return flow.get("flow:host_server_name")+"/"+flow.get("flow:http_url");
    }

    @Override
    protected AdmlAlert getAdmlAlert(AdmlEvent event, long totalFlows) {

        AdmlFlow admlFlow = event.getFlow();

        AdmlAlert admlAlert = new AdmlAlert();
        admlAlert.setTitle(event.getTitle());
        admlAlert.setText(event.getText());
        admlAlert.setFlowId(admlFlow.get("flow:id"));
        admlAlert.setTotalFlows(totalFlows);
        admlAlert.setUsername(event.getUsername());
        admlAlert.setCoords(event.getCoords());
        admlAlert.setTimestamp(AdmlDateUtil.getTimestamp());
        admlAlert.setAlgorithm(new HttpKmeansAlgorithmDetails(getNumberOfClusters(),getMinDirtyProportion(), getMaxAnomalousClusterProportion()));

        ProtocolFlow httpFlow = new HttpFlow();
        // ProtocolFlow httpFlow = new HttpFlow();
        httpFlow.setDetectedProtocol("HTTP");
        httpFlow.setPackets(Long.parseLong(admlFlow.get("flow:packets")));
        httpFlow.setLowerPort(Long.parseLong(admlFlow.get("flow:lower_port")));
        httpFlow.setUpperPort(Long.parseLong(admlFlow.get("flow:upper_port")));
        httpFlow.setAvgInterTime(Double.parseDouble(admlFlow.get("flow:avg_inter_time")));
        httpFlow.setMaxPacketSize(Long.parseLong(admlFlow.get("flow:max_packet_size")));
        httpFlow.setHostname(admlFlow.get("flow:host_server_name"));
        httpFlow.setBytes(Long.parseLong(admlFlow.get("flow:bytes")));
        httpFlow.setLowerIp(admlFlow.getLowerIp());
        httpFlow.setUpperIp(admlFlow.getUpperIp());

        admlAlert.setProtocolFlow(httpFlow);
       // ((HttpFlow) httpFlow).setHostServerName(admlFlow.get("flow:host_server_name"));

        return admlAlert;
    }

}
