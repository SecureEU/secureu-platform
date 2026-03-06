package ro.simavi.sphinx.ad.kernel.kmeans;

import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.mllib.clustering.KMeans;
import org.apache.spark.mllib.clustering.KMeansModel;
import org.apache.spark.mllib.linalg.Vector;
import org.apache.spark.mllib.linalg.Vectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ro.simavi.sphinx.ad.handlers.AlertHandler;
import ro.simavi.sphinx.ad.kernel.event.AdmlEvent;
import ro.simavi.sphinx.ad.kernel.event.AdmlSignature;
import ro.simavi.sphinx.ad.kernel.hbase.AdmlHBaseRDD;
import ro.simavi.sphinx.ad.kernel.model.AdmlAlert;
import ro.simavi.sphinx.ad.kernel.util.AdmlFlow;
import ro.simavi.sphinx.ad.kernel.util.AdmlStatistics;
import scala.Tuple2;
import scala.Tuple3;
import scala.Tuple4;

import java.io.Serializable;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Stream;

public abstract class AdmlKMeans implements Serializable {

    private static final Logger logger = LoggerFactory.getLogger(AdmlKMeans.class);

    protected AdmlKMeansConfig admlKMeansConfig;

    private String name;

    private AdmlStatistics admlStatistics;

    private List<AdmlSignature> admlSignatureList;

    private static JavaSparkContext sparkContext;

    protected AlertHandler alertHandler;

    public AdmlKMeans(AlertHandler alertHandler, AdmlKMeansConfig admlKMeansConfig, String name, JavaSparkContext sparkContext){
        this.admlKMeansConfig = admlKMeansConfig;
        this.name = name;
        this.admlStatistics = new AdmlStatistics();
        this.sparkContext = sparkContext;
        this.alertHandler = alertHandler;
    }

    protected List<String> getFeatures() {
        return admlKMeansConfig.getFeatures();
    }

    protected List<String> getNotNullFeatures(){
        return admlKMeansConfig.getNotNullFeatures();
    }

    protected abstract boolean getFilterCondition(AdmlFlow admlFlow);

    protected abstract List<AdmlSignature> getAdmlSignatureList();

    protected abstract String getHostname(AdmlFlow flow);

    protected void kmeans(JavaPairRDD<ImmutableBytesWritable,Result> admlRDD) {
        AdmlHBaseRDD admlHBaseRDD = AdmlHBaseRDD.getInstance();
        List<String> columns = admlHBaseRDD.getColumns();

        logger.info("-1. Before filtering AdmlRDD..."+ admlRDD.count());

        logger.info("0. Filtering AdmlRDD...");
        JavaRDD<AdmlFlow> rdd = getJavaRDDAdmlFlow(admlRDD, columns);

        logger.info("1. Counting AdmlRDD...");
        long rddTotalSize = rdd.count();

        logger.info("2. Filtered AdmlRDD has "+rddTotalSize+" rows!");
        if (rddTotalSize==0){
            logger.error("2. Filtered AdmlRDD has "+rddTotalSize+" rows!");
        }
        if(rddTotalSize==0) {
//            if (alertHandler!=null){
//                AdmlAlert admlAlert = new AdmlAlert();
//                admlAlert.setTotalFlows(rddTotalSize);
//                alertHandler.onAlert(admlAlert);
//            }
            return;
        }

        logger.info("3. Calculating some variables to normalize data...");
        logger.info("4. Normalizing data...");
        JavaPairRDD<Tuple4<String, Integer, String, AdmlFlow>,Vector> labelAndData = getLabelAndData(rdd);

        logger.info("5. Estimating model...");
        JavaRDD<Vector> data = labelAndData.values().cache();
        long vectorCount = data.count();
        logger.info("Number of vectors: "+vectorCount);
        KMeans kmeans = new KMeans(); // maxIteration = 20, EUCLIDEAN distance
        kmeans.setK(this.getNumberOfClusters());
        KMeansModel model = kmeans.run(data.rdd());

        logger.info("6. Predicting points (ie, find cluster for each point)...");

        JavaRDD<Tuple3<Integer,Tuple4,Vector>> clusterLabel = labelAndData.map((item)->{
            Tuple4 label = item._1();
            Vector datum = item._2();
            int cluster = model.predict(datum);
            return new Tuple3<>(cluster, label, datum);
        });

        generateHistogram(clusterLabel, model, rddTotalSize);
    }

    protected void setAdmlSignatureList(List<AdmlSignature> admlSignatureList) {
        this.admlSignatureList = admlSignatureList;
    }

    private JavaRDD<Map<Tuple2<Integer, String>,Tuple2<Double, Integer>>> getClusterLabelCountRDD(JavaRDD<Tuple3<Integer,Tuple4,Vector>> clusterLabel){
        JavaRDD<Map<Tuple2<Integer, String>,Tuple2<Double, Integer>>> clusterLabelCountRDD = clusterLabel.map(item->{
            int cluster = item._1();
            Tuple4<String, Integer, String, AdmlFlow> label = item._2();
            String detectedProtocol = label._1();
            Integer priorityId = label._2();
            // Vector datum = item._3();
            Map<Tuple2<Integer, String>,Tuple2<Double, Integer>> map = new HashMap();
            map.put(new Tuple2(cluster, detectedProtocol), new Tuple2(priorityId.doubleValue(),1));
            return map;
        });
        return clusterLabelCountRDD;
    }
//
//    private JavaRDD<Map<Tuple2<Integer, String>,Tuple2<Double, Integer>>> getClusterLabelCountRDDMockUp(){
//        // test
//        Map<Tuple2<Integer, String>,Tuple2<Double, Integer>> map1 = new HashMap();
//        map1.put(new Tuple2<>(0,"1"),new Tuple2<>(4d,1));
//
//        Map<Tuple2<Integer, String>,Tuple2<Double, Integer>> map2 = new HashMap();
//        map2.put(new Tuple2<>(0,"2"),new Tuple2<>(2d,1));
//
//        Map<Tuple2<Integer, String>,Tuple2<Double, Integer>> map3 = new HashMap();
//        map3.put(new Tuple2<>(0,"1"),new Tuple2<>(3d,1));
//
//        Map<Tuple2<Integer, String>,Tuple2<Double, Integer>> map4 = new HashMap();
//        map4.put(new Tuple2<>(0,"1"),new Tuple2<>(13d,1));
//
//        List<Map<Tuple2<Integer, String>,Tuple2<Double, Integer>>> list = new ArrayList();
//        list.add(map1);
//        list.add(map2);
//        list.add(map3);
//        list.add(map4);
//
//        return sparkContext.parallelize(list);
//    }

    private Map<Tuple2<Integer, String>,Tuple2<Double, Integer>> getClusterLabelCount(JavaRDD<Tuple3<Integer,Tuple4,Vector>> clusterLabel){
        JavaRDD<Map<Tuple2<Integer, String>,Tuple2<Double, Integer>>> clusterLabelCountRDD = getClusterLabelCountRDD(clusterLabel);
        //clusterLabelCountRDD = getClusterLabelCountRDDMockUp();

        List<Map<Tuple2<Integer, String>,Tuple2<Double, Integer>>> clusterLabelCountList = clusterLabelCountRDD.collect();

        Map<Tuple2<Integer, String>,Tuple2<Double, Integer>> result = new HashMap();

        clusterLabelCountList.forEach(item->{
            // /: => foldLeft () [b./:(0) ~ b.foldLeft(0)]

            item.entrySet().forEach((i)->{
                Tuple2 key = i.getKey();
                Tuple2<Double,Integer> newValue = i.getValue();

                Tuple2<Double,Integer> oldValue = result.get(key);
                if (oldValue==null){
                    result.put(key, newValue);
                }else{
                    int oldCcount = oldValue._2;
                    int newCount = newValue._2; // =1;
                    int count = oldCcount + newCount;
                    Double avg = (oldValue._1*oldCcount + newValue._1 * newCount)/count; // medie secventiala
                    result.put(key, new Tuple2(avg, count));
                }
            });

        });

        return result;
    }

    private void generateHistogram( JavaRDD<Tuple3<Integer,Tuple4,Vector>> clusterLabel, KMeansModel model, long rddTotalSize ){
        logger.info("7. Generating histogram...");

        Map<Tuple2<Integer, String>,Tuple2<Double, Integer>> clusterLabelCount  = getClusterLabelCount(clusterLabel);

        final List<AdmlAlert> admlAlerts = new ArrayList<>();

        logger.info("######################################################################################");
        logger.info("######################################################################################");
        logger.info("######################################################################################");
        logger.info("######################################################################################");
        logger.info("HTTP K-Means Clustering");
        logger.info("Centroids");
        String centroids = mkString(model.clusterCenters(),",\n");

        displayClusterLabelCount(clusterLabelCount);

        double thr = this.getMaxAnomalousClusterProportion() * rddTotalSize; // 0.05

        logger.info("Selecting cluster to be tainted..."); // selectare cluster cu anomalii

        Supplier<Stream<Tuple2>> taintedArray = ()-> clusterLabelCount.entrySet().stream()
                .filter(x -> {
                    Tuple2<Double, Integer> value = x.getValue();
                    double avg = value._1;
                    int count = value._2;
                    return (Double.valueOf(count) < thr && Double.valueOf(avg) >=  this.getMinDirtyProportion() );
                }).map(x->{
                    return x.getKey();
                });

        taintedArray.get().parallel().forEach(tainted -> {

            logger.info("######################################################################################");
            logger.info("Tainted flows of: " + tainted.toString());

            logger.info("Generating events into HBase...");

            JavaRDD<AdmlAlert> admlAlertJavaRDD = clusterLabel.filter(
                    item -> {
                        int cluster = item._1();
                        Tuple4 tuple4 = item._2();
                        Object group = tuple4._1(); // protocol
                        Object tagged = tuple4._2(); // prioritate // todo ==> de investigat daca aici e priotitatea
                        Tuple2 tuple2FromClusterLabel = new Tuple2<>(cluster, group);
                        // todo: de investigat ==> && tagged.equals(0)
                        //return tuple2FromClusterLabel.equals(tainted) && tagged.equals(0);
                        return tuple2FromClusterLabel.equals(tainted);
                    }
            ).map(item -> { // (cluster,(group,tagged,hostname,flow),datum)
                int cluster = item._1();
                Tuple4 tuple4 = item._2();//1582454776164203.20110815141810
                Vector datum = item._3();
                AdmlFlow flow = (AdmlFlow) tuple4._4();
                Object group = tuple4._1();

                AdmlEvent event = new AdmlEvent(flow);
                event.getData().put("centroids", centroids);
                event.getData().put("vector", datum.toString());
                event.getData().put("clusterLabel", "(" + cluster + "," + group + ")");
                event.getData().put("hostname", this.getHostname(flow));

                logger.info("++++++++++++++++++++++++++++++++"+cluster+" "+ flow.get("flow:id"));
                kmeansPopulate(event, admlSignatureList).alert();

                if (this.alertHandler!=null) {
                    AdmlAlert admlAlert = getAdmlAlert(event, rddTotalSize);
                    return admlAlert;
                    //admlAlerts.add(admlAlert);
                    //this.alertHandler.onAlert(admlAlert);
                }
                return null;
            });

            Iterator<AdmlAlert> iterator = admlAlertJavaRDD.toLocalIterator();
            while(iterator.hasNext()){
                AdmlAlert admlAlert = iterator.next();
                if (admlAlert!=null) {
                    this.alertHandler.onAlert(admlAlert);
                }
            }

            logger.info("######################################################################################");
            logger.info("######################################################################################");
            logger.info("######################################################################################");
            logger.info("######################################################################################");

        });

        if(taintedArray.get().count()==0) {
            logger.info("No flow matched!");
        }else{
            logger.info("############################### " + taintedArray.get().count() +"#######################################################");
        }

    }

    protected abstract AdmlAlert getAdmlAlert(AdmlEvent event, long totalFlows);

    protected JavaPairRDD<Tuple4<String, Integer, String, AdmlFlow>,Vector> getLabelAndData(JavaRDD<AdmlFlow> rdd){
        List<String> features = getFeatures();

        // toate featurile au valori de tip Double; si o pune intr-o matrice, unde pe fiecare linie sunt featurile unui pcap: [(p1,p2,...,pN)], unde p1,...,pN = [f1,..,fM] => o matrice de m linii si n coloane
        // se construieste matricea
        JavaRDD<ArrayList<Double>> httpRDDcount = rdd.map(flow ->{
            ArrayList<Double> list = new ArrayList<Double>();
            for(String feature:features){
                try {
                    list.add(Double.parseDouble(flow.get(feature)));
                }catch (Exception e) {
                    list.add(0d);
                }
            }
            return list;
        }).cache();

        // se calculeaza suma pe coloane si rezulta  line suma
        // x1+x2+....xN
        long n = rdd.count();

        List<Double> sums = admlStatistics.getSums(httpRDDcount); // x1+....xN
        List<Double> sumSquares = admlStatistics.getSquares(httpRDDcount); // // x1*x1+x2*x2+....+xN*xN
        List<Double> stdevs = admlStatistics.getStdev(httpRDDcount, sums, sumSquares, n); new ArrayList<>();

        logger.info("4. Normalizing data...");
        //JavaRDD<Tuple2<Tuple4,Vector>> labelAndData =
        JavaPairRDD<Tuple4<String, Integer, String, AdmlFlow>,Vector> labelAndData =
                rdd.mapToPair(flow->{

                    //features.map { feature => flow.get(feature).toDouble }
                    ArrayList<Double> doubleList = new ArrayList<Double>();
                    for(String feature:features){
                        try {
                            doubleList.add(Double.parseDouble(flow.get(feature)));
                        }catch (Exception e) {
                            doubleList.add(0d);
                        }
                    }
                    double [] list = doubleList.stream().mapToDouble(Double::doubleValue).toArray();
                    Vector vector =  admlStatistics.normalize(Vectors.dense(list).toArray(), sums, stdevs, n);

                    Tuple4<String, Integer, String, AdmlFlow> tuple4 = new Tuple4<String, Integer, String, AdmlFlow>(
                            flow.get("flow:detected_protocol"),
                            (flow.get("event:priority_id")!=null && flow.get("event:priority_id").equals("1"))? 1:0 ,
                            flow.get("flow:host_server_name"),
                            flow);

                    return new Tuple2<Tuple4<String, Integer, String, AdmlFlow>,Vector>(tuple4, vector);

                });

        return labelAndData;

    }

    protected JavaRDD<AdmlFlow> getJavaRDDAdmlFlow(JavaPairRDD<ImmutableBytesWritable,Result> admlRDD, List<String> columns) {

        JavaRDD<AdmlFlow> rdd =
                admlRDD.map(f -> {

                    ImmutableBytesWritable id = f._1;
                    Result result = f._2;

                    Map<String, String> map = new HashMap();
                    map.put("flow:id", Bytes.toString(id.get()));
                    for (String column : columns) {
                        String[] columnSplit = column.split(":");
                        byte[] ret = result.getValue(Bytes.toBytes(columnSplit[0]), Bytes.toBytes(columnSplit[1]));
                        map.put(column, Bytes.toString(ret));
                    }

                    for (String notNullFeature : getNotNullFeatures()) {
                        this.init(map, notNullFeature, "0");
                    }

                    byte[] lowerIp = result.getValue(Bytes.toBytes("flow"), Bytes.toBytes("lower_ip"));
                    byte[] upperIp = result.getValue(Bytes.toBytes("flow"), Bytes.toBytes("upper_ip"));

                    return new AdmlFlow(map, Bytes.toString(lowerIp), Bytes.toString(upperIp));

                }).filter(x -> {
                    return getFilterCondition(x);
                }).cache();

        return rdd;
    }

    protected void init(Map<String,String> map, String key, String value){
        if(map.get(key)==null) {
            map.put(key,value);
        }
    }

    protected AdmlEvent kmeansPopulate(AdmlEvent event, List<AdmlSignature> admlSignatureList){
        String centroids = event.getData().get("centroids");
        String vector = event.getData().get("vector");
        String clusterLabel = event.getData().get("clusterLabel");
        String hostname = event.getData().get("hostname");

        event.setText( "This flow was detected by AdmlKmeans as an anormal activity. Method: k-means clustering with k="+this.getNumberOfClusters());
        //"Centroids:\n"+centroids+"\n"+
        //"Vector: "+vector+"\n"+
        //"(cluster,label nDPI): "+clusterLabel+"\n";

        event.setSignatureId(admlSignatureList.get(0).getId());

        return event;
    }

    private void displayClusterLabelCount(Map<Tuple2<Integer, String>,Tuple2<Double, Integer>> clusterLabelCount){

        clusterLabelCount.entrySet().stream().map( //  (z,(key:(Int,String),(avg,count)))
                item ->{
                    Tuple2 key = item.getKey();
                    Tuple2 value = item.getValue();
                    int cluster = (Integer) key._1();
                    String label = (String)key._2();
                    int count = (Integer) value._1();
                    double avg = (Double)value._2();
                    logger.info("Cluster: "+cluster+"\t\tLabel: "+label+"\t\tCount: "+count+"\t\tAvg: "+avg);
                    return 0;
                }
        );
    }

    private String mkString(Vector[] vectors, String separator){
        StringBuilder stringBuilder = new StringBuilder();
        for(int i=0;i<vectors.length;i++){
            stringBuilder.append(vectors[i].toString()).append(separator);
        }
        return stringBuilder.toString();
    }

    public int getNumberOfClusters() {
        return admlKMeansConfig.getNumberOfClusters();
    }

    public Double getMaxAnomalousClusterProportion() {
        return admlKMeansConfig.getMaxAnomalousClusterProportion();
    }

    public Double getMinDirtyProportion() {
        return admlKMeansConfig.getMinDirtyProportion();
    }
}
