package ro.simavi.sphinx.ad.kernel.initiate;

import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.util.Bytes;
import ro.simavi.sphinx.ad.kernel.hbase.AdmlHBaseRDD;

import java.io.IOException;

public class AdmlInitiate {

    private static String sensor_description="ADLM IDS";
    private static String sensor_hostname="admlhostname";


    public static void initiate(){

        Get get = new Get(Bytes.toBytes("1"));

        try {
            AdmlHBaseRDD admlHBaseRDD = AdmlHBaseRDD.getInstance();
            if(!admlHBaseRDD.getSensor().exists(get)) {
                Put put = new Put(Bytes.toBytes("1"));
                put.addColumn(Bytes.toBytes("sensor"), Bytes.toBytes("description"), Bytes.toBytes(sensor_description));
                put.addColumn(Bytes.toBytes("sensor"), Bytes.toBytes("hostname"), Bytes.toBytes(sensor_hostname));
                admlHBaseRDD.getSensor().put(put);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }


    }
}
