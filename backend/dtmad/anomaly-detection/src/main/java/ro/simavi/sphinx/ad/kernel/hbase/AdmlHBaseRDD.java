package ro.simavi.sphinx.ad.kernel.hbase;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.*;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.mapreduce.TableInputFormat;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import ro.simavi.sphinx.ad.SpringContext;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AdmlHBaseRDD {

    private static final Logger logger = LoggerFactory.getLogger(AdmlHBaseRDD.class);

    private Connection connection;

    private boolean ok;

    private Admin admin;

    private Configuration configuration;

    private Table admlSensorTable;
    private Table admlFlowsTable;
    private Table admlAuthrecordsTable;
    private Table admlSflowsTable;
    private Table admlSignaturesTable;
    private Table admlEventsTable;
    private Table admlReputationTable;
    private Table admlMynetsTable;
    private Table admlHistograms;

    private static AdmlHBaseRDD admlHBaseRDD = null;

    private List<String> columns;
    List<String> columnsSFlow;

    private AdmlHBaseRDD() {
        ok = Boolean.FALSE;
        try {
            boolean connected = connect();
            if (connected) {
                init();
                create();
                instantiate();
                insert();
                ok = Boolean.TRUE;
            }
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
    }

    public static AdmlHBaseRDD getInstance(){
        if (admlHBaseRDD==null){
            admlHBaseRDD = new AdmlHBaseRDD();
        }
        return admlHBaseRDD;
    }


    public boolean truncateAdmlFlows(String tableString){
        try {

            TableName tableName = TableName.valueOf(tableString);

            if (!admin.tableExists(tableName)) {
                logger.debug("Attempted to clear table {} before it exists (noop)", tableString);
                return false;
            }

            if (!admin.isTableDisabled(tableName)) {
                admin.disableTable(tableName);
            }

            if (!admin.isTableDisabled(tableName)){
                throw new RuntimeException("Unable to disable table " + tableName);
            }

            logger.info("Truncating table {}", tableName);
            admin.truncateTable(tableName, true /* preserve splits */);

            try {
                admin.enableTable(tableName);
                return true;
            } catch (TableNotDisabledException e) {
                // This triggers seemingly every time in testing with 1.0.2.
                logger.debug("Table automatically reenabled by truncation: {}", tableName, e);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    /*  Create DB scheme and insert initial data */
    private void create() throws IOException {
        /*
        create 'adml_flows','flow','event'
        create 'adml_sflows','flow'
        create 'adml_events','event'
        create 'adml_sensor','sensor'
        create 'adml_signatures','signature'
        create 'adml_mynets','net'
        create 'adml_reputation','rep'
        create 'adml_histograms','info','values','labels'
        create 'adml_clusters','info'
        create 'adml_cluster_members','info','member','cluster'
        create 'adml_inventory','info'
        create 'adml_authrecords','auth'
        */
        createTable("adml_flows", new String[]{"flow","event"});
        createTable("adml_sflows", new String[]{"flow"});
        createTable("adml_events", new String[]{"event"});
        createTable("adml_sensor", new String[]{"sensor"});
        createTable("adml_signatures", new String[]{"signature"});

        // sflow
            createTable("adml_mynets", new String[]{"net"});
            createTable("adml_reputation", new String[]{"rep"});
            createTable("adml_histograms", new String[]{"info","values","labels"});
            createTable("adml_clusters", new String[]{"info"});
            createTable("adml_cluster_members", new String[]{"info","member","cluster"});
            createTable("adml_inventory", new String[]{"info"});
        createTable("adml_authrecords", new String[]{"auth"});
    }

    private void createTable(String name, String[] familyList) throws IOException {

        if (!admin.isTableAvailable(TableName.valueOf(name))) {
            logger.error("Table "+name+" does not exist. Try to create...");
        }else{
            return;
        }

        final TableName tableName = TableName.valueOf(name);
        TableDescriptorBuilder tableDescBuilder = TableDescriptorBuilder.newBuilder(tableName);
        for(String family:familyList ) {
            ColumnFamilyDescriptorBuilder columnDescBuilder = ColumnFamilyDescriptorBuilder
                    .newBuilder(Bytes.toBytes(family));
                 //   .setBlocksize(32 * 1024)
                 //   .setCompressionType(Compression.Algorithm.SNAPPY).setDataBlockEncoding(DataBlockEncoding.NONE);
            tableDescBuilder.setColumnFamily(columnDescBuilder.build());
        }

        admin.createTable(tableDescBuilder.build());
    }

    private void instantiate() throws IOException {
        admlFlowsTable = connection.getTable(TableName.valueOf("adml_flows"));
        admlSflowsTable = connection.getTable(TableName.valueOf("adml_sflows"));
        admlEventsTable = connection.getTable(TableName.valueOf("adml_events"));
        admlSensorTable = connection.getTable(TableName.valueOf("adml_sensor"));
        admlSignaturesTable = connection.getTable(TableName.valueOf("adml_signatures"));
        admlMynetsTable           = connection.getTable(TableName.valueOf("adml_mynets"));
        admlReputationTable       = connection.getTable(TableName.valueOf("adml_reputation"));
        admlHistograms       = connection.getTable(TableName.valueOf("adml_histograms"));
        //adml_clusters         = connection.getTable(TableName.valueOf("adml_clusters"));
        //adml_cluster_members  = connection.getTable(TableName.valueOf("adml_cluster_members"));
        //adml_inventory        = connection.getTable(TableName.valueOf("adml_inventory"));
        admlAuthrecordsTable = connection.getTable(TableName.valueOf("adml_authrecords"));
    }

    private void insert(){
        /*
        String NETPREFIXES="10.1.,100.100.";
        String nets[] = NETPREFIXES.split(",");
        for(String net:nets){
            put(mynets,net,"net","description","Desc"+net);
            put(mynets,net,"net","prefix",net);
        }
        */
    }

//    private void put(Table table, String rowKey, String columnFamily, String qualifier, String value) throws IOException {
//        Put p = new Put(Bytes.toBytes(rowKey));
//        p.addColumn(Bytes.toBytes(columnFamily), Bytes.toBytes(qualifier),Bytes.toBytes(value));
//        table.put(p);
//    }

    private boolean connect(){
        Environment environment = SpringContext.getBean(Environment.class);
        String hbaseZookeeperQuorum = environment.getProperty("hbase.zookeeper.quorum");
        String clientPortString = environment.getProperty("hbase.zookeeper.property.clientPort");
        Integer clientPort = 2181;
        try{
            clientPort = Integer.parseInt(clientPortString);
        }catch (Exception e){

        }

        configuration = HBaseConfiguration.create();
        configuration.set("hbase.zookeeper.quorum", hbaseZookeeperQuorum);
        configuration.set("zookeeper.session.timeout", "1800000");
        configuration.set("hbase.rpc.timeout", "1800000");
        configuration.setInt("hbase.zookeeper.property.clientPort", clientPort);
        configuration.setInt("hbase.client.scanner.timeout.period", 1800000);

        try {
            HBaseAdmin.available(configuration);
        } catch (MasterNotRunningException e) {
            logger.error(e.getMessage());
            return false;
        } catch (ZooKeeperConnectionException e){
            logger.error(e.getMessage());
            return false;
        } catch (IOException e ){
            logger.error(e.getMessage());
            return false;
        } catch (Exception e){
            logger.error(e.getMessage());
            return false;
        }

        return true;
    }

    private void init() throws IOException{
        columns = new ArrayList<>(Arrays.asList(
                "flow:lower_ip", // K-means-DNS,
                "flow:upper_ip", // K-means-DNS,
                "flow:lower_name",
                "flow:upper_name",
                "flow:lower_port", // K-means
                "flow:upper_port", // K-means
                "flow:protocol",
                "flow:vlan_id",
                "flow:last_seen",
                "flow:bytes", // K-means-DNS+HTTP,
                "flow:packets", // K-means-DNS+HTTP,
                "flow:flow_duration", // K-means-DNS+HTTP,
                "flow:first_seen",
                "flow:max_packet_size", // K-means-DNS+HTTP,
                "flow:min_packet_size", // K-means-DNS+HTTP,
                "flow:avg_packet_size", // K-means-DNS+HTTP,
                "flow:payload_bytes",
                "flow:payload_first_size",
                "flow:payload_avg_size",
                "flow:payload_min_size",
                "flow:payload_max_size",
                "flow:packets_without_payload",  // K-means-DNS+HTTP,  // Number of pkts with empty payload
                "flow:detection_completed",
                "flow:detected_protocol", // K-means,
                "flow:host_server_name", // K-means,

                "flow:inter_time_stddev",
                "flow:packet_size_stddev",
                "flow:avg_inter_time", // K-means-DNS+HTTP,

                "flow:packet_size-0", // K-means-DNS+HTTP,
                "flow:inter_time-0",  // K-means-DNS+HTTP,
                "flow:packet_size-1", // K-means-DNS+HTTP,
                "flow:inter_time-1",  // K-means-HTTP
                "flow:packet_size-2", // K-means-HTTP
                "flow:inter_time-2",  // K-means-HTTP
                "flow:packet_size-3", // K-means-HTTP
                "flow:inter_time-3",  // K-means-HTTP
                "flow:packet_size-4", // K-means-HTTP
                "flow:inter_time-4", //  K-means-HTTP

                "flow:detected_os",

                "flow:dns_num_queries", // protos.dns.num_queries // K-means-DNS,
                "flow:dns_num_answers", // protos.dns.num_answers // K-means-DNS,
                "flow:dns_ret_code", // protos.dns.ret_code // K-means-DNS,
              //  "flow:dns_bad_packet", // protos.dns.bad_packet // K-means-DNS, deprecated
                "flow:dns_query_type", // protos.dns.query_type // K-means-DNS,
                "flow:dns_query_class", // protos.dns.query_class
                "flow:dns_rsp_type", //  protos.dns.dns_rsp_type // K-means-DNS,

                "event:sensor_id", //  [Unified2 IDS Event]: sensor id
                "event:event_id", // [Unified2 IDS Event]: event id
                "event:event_second",  // [Unified2 IDS Event]: event second
                "event:event_microsecond",  // [Unified2 IDS Event]:  event microsecond
                "event:signature_id",  // [Unified2 IDS Event]: signature id
                "event:generator_id",  // [Unified2 IDS Event]: generator id
                "event:classification_id",  // [Unified2 IDS Event]:  classification id
                "event:priority_id",  //  K-means [Unified2 IDS Event]:  priority id

                "flow:http_method", //K-means HTTP
                "flow:http_url",
                "flow:http_content_type"));

        columnsSFlow = new ArrayList<>(
                Arrays.asList("flow:IPprotocol",
                        "flow:dstIP",
                        "flow:dstPort",
                        "flow:packetSize",
                        "flow:samplingRate",
                        "flow:srcIP",
                        "flow:srcPort",
                        "flow:tcpFlags",
                        "flow:timestamp"));

        //configuration.addResource("C:\\tools\\hbase-2.3.1\\conf\\hbase-site.xml");
        connection = ConnectionFactory.createConnection(configuration);

        admin = connection.getAdmin();

    }

    private JavaPairRDD connect(String table, JavaSparkContext sparkContext) throws IOException {

        configuration.set(TableInputFormat.INPUT_TABLE, table);
        configuration.set("zookeeper.session.timeout", "600000");
        configuration.setInt("hbase.client.scanner.timeout.period", 600000);

        JavaPairRDD hBaseRDD = sparkContext.newAPIHadoopRDD(configuration,TableInputFormat.class,
                org.apache.hadoop.hbase.io.ImmutableBytesWritable.class,
                org.apache.hadoop.hbase.client.Result.class);

        return hBaseRDD;
    }

    public JavaPairRDD connectFlows(JavaSparkContext sparkContext) throws IOException {
        String table = "adml_flows";

        configuration.set(TableInputFormat.INPUT_TABLE, table);
        configuration.set("zookeeper.session.timeout", "1800000");
        configuration.setInt("hbase.client.scanner.timeout.period", 1800000);
        configuration.set("hbase.rpc.timeout", "1800000");

        JavaPairRDD hBaseRDD = sparkContext.newAPIHadoopRDD(configuration, TableInputFormat.class,
                org.apache.hadoop.hbase.io.ImmutableBytesWritable.class,
                org.apache.hadoop.hbase.client.Result.class);

        return hBaseRDD;
    }

    public JavaPairRDD connectSFlow(JavaSparkContext sparkContext) throws IOException {
        return connect("adml_sflows",sparkContext);
    }

    public JavaPairRDD connectHistograms(JavaSparkContext sparkContext) throws IOException {
        return connect("adml_histograms",sparkContext);
    }

    public JavaPairRDD connectAuth(JavaSparkContext sparkContext) throws IOException {
        return connect("adml_authrecords",sparkContext);
    }

    public Table getSensor(){
        return admlSensorTable;
    }

    public Table getFlows(){
        return admlFlowsTable;
    }

    public Table getAuthrecords() {
        return admlAuthrecordsTable;
    }

    public Table getSignatures() {
        return admlSignaturesTable;
    }

    public Table getEvents() {
        return admlEventsTable;
    }

    public Table getSflowsTable() {
        return admlSflowsTable;
    }

    public Table getAdmlReputationTable() {
        return admlReputationTable;
    }

    public Table getAdmlMynetsTable(){
        return admlMynetsTable;
    }

    public Table getAdmlHistograms(){
        return admlHistograms;
    }
    public void close() {
        try {
            if (admin!=null) {
                admin.close();
            }
            if (connection!=null) {
                connection.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public List<String> getColumns(){
        return columns;
    }

    public boolean isOk(){
        return ok;
    }
}
