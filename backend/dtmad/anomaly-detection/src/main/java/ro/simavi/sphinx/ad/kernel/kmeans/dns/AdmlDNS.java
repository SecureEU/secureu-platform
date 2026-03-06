package ro.simavi.sphinx.ad.kernel.kmeans.dns;

import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ro.simavi.sphinx.ad.dpi.flow.model.DnsFlow;
import ro.simavi.sphinx.ad.dpi.flow.model.ProtocolFlow;
import ro.simavi.sphinx.ad.handlers.AlertHandler;
import ro.simavi.sphinx.ad.kernel.event.AdmlEvent;
import ro.simavi.sphinx.ad.kernel.event.AdmlSignature;
import ro.simavi.sphinx.ad.kernel.kmeans.AdmlKMeans;
import ro.simavi.sphinx.ad.kernel.kmeans.AdmlKMeansConfig;
import ro.simavi.sphinx.ad.kernel.model.AdmlAlert;
import ro.simavi.sphinx.ad.kernel.model.algorithm.kmeans.DnsKMeansAlgorithmDetails;
import ro.simavi.sphinx.ad.kernel.util.AdmlDateUtil;
import ro.simavi.sphinx.ad.kernel.util.AdmlFlow;

import java.util.ArrayList;
import java.util.List;

public class AdmlDNS extends AdmlKMeans {

    private static final Logger logger = LoggerFactory.getLogger(AdmlDNS.class);

    private boolean test;

    public AdmlDNS(AlertHandler alertHandler, AdmlKMeansConfig admlKMeansConfig, JavaSparkContext sparkContext, Boolean test){
        super(alertHandler, admlKMeansConfig, "DNS", sparkContext); // 8
        this.setAdmlSignatureList(getAdmlSignatureList());
        this.test = test;
    }

    protected List<AdmlSignature> getAdmlSignatureList(){
        List<AdmlSignature> admlSignatureList = new ArrayList<>();
        admlSignatureList.add(new AdmlSignature(3,"Suspicious DNS flow identified by K-Means clustering",2,1,826000001d,826).saveHBase());
        return admlSignatureList;
    }

    public static void run(AlertHandler alertHandler, AdmlKMeansConfig admlKMeansConfig, JavaPairRDD<ImmutableBytesWritable,Result> admlRDD, JavaSparkContext sparkContext, boolean test) {
        AdmlDNS admlDNS = new AdmlDNS(alertHandler, admlKMeansConfig,sparkContext,test);
        admlDNS.kmeans(admlRDD);
    }

    @Override
    protected List<String> getFeatures() {
        return admlKMeansConfig.getFeatures();
    }

    @Override
    protected List<String> getNotNullFeatures() {
        return admlKMeansConfig.getNotNullFeatures();
    }

    protected boolean getFilterCondition(AdmlFlow x){
        // filter:
        //      flow:lower_port==53 or flow:upper_port=53
        //      flow:packets>1
        //      generated in last 100 Minutes
        boolean hasPort =  test || x.get("flow:lower_port").equals("53") || x.get("flow:upper_port").equals("53");
        boolean hasPackets =  test || Double.parseDouble(x.get("flow:packets"))>1;
        boolean isOld = !test && Long.parseLong(x.get("flow:id").split("\\.")[0])>=(System.currentTimeMillis()-6000000);
//        boolean isOld = false;
        boolean isOk = hasPort && hasPackets && !isOld;
//        if (!isOk){
//            logger.info("=======> "+ hasPort +" " + hasPackets + " " + isOld);
//        }
        return isOk;
    }

    @Override
    protected String getHostname(AdmlFlow flow) {
        return flow.get("flow:host_server_name"); //todo: o idee mai buna ar fi: rrname
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
        admlAlert.setAlgorithm(new DnsKMeansAlgorithmDetails(getNumberOfClusters(),getMinDirtyProportion(), getMaxAnomalousClusterProportion()));

        ProtocolFlow dnsFlow = new DnsFlow();
        dnsFlow.setDetectedProtocol("DNS");
        dnsFlow.setPackets(Long.parseLong(admlFlow.get("flow:packets")));
        dnsFlow.setLowerPort(Long.parseLong(admlFlow.get("flow:lower_port")));
        dnsFlow.setUpperPort(Long.parseLong(admlFlow.get("flow:upper_port")));
        dnsFlow.setAvgInterTime(Double.parseDouble(admlFlow.get("flow:avg_inter_time")));
        dnsFlow.setMaxPacketSize(Long.parseLong(admlFlow.get("flow:max_packet_size")));
        dnsFlow.setHostname(admlFlow.get("flow:host_server_name"));
        dnsFlow.setBytes(Long.parseLong(admlFlow.get("flow:bytes")));

        dnsFlow.setLowerIp(admlFlow.getLowerIp());
        dnsFlow.setUpperIp(admlFlow.getUpperIp());

        admlAlert.setProtocolFlow(dnsFlow);
        return admlAlert;
    }

}
