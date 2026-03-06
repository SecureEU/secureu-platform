package ro.simavi.sphinx.ad.kernel.event;

import lombok.Getter;
import lombok.Setter;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.util.Bytes;
import ro.simavi.sphinx.ad.kernel.hbase.AdmlHBaseRDD;
import ro.simavi.sphinx.ad.kernel.util.AdmlFlow;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;

@Setter
@Getter
public class AdmlEvent {

    private Integer sensorid = 0;
    private Double signatureId = 0d;
    private Integer priorityid=0;
    private String text = "";
    private HashMap<String, String> data=new HashMap();
    private String ports="";
    private String title="";
    private String username="";
    private String coords="";

    private AdmlFlow flow;

    public AdmlEvent(AdmlFlow flow){
        this.flow = flow;
    }

    private  byte[] formatIPtoBytes(String ip) {
        try {
            // Eca! Snorby doesn't support IPv6 yet. See https://github.com/Snorby/snorby/issues/65
            if (ip.contains(":"))
                return InetAddress.getByName("255.255.6.6").getAddress();
            else
                return InetAddress.getByName(ip).getAddress();
        } catch (Throwable t){
                // Bogus address!
            try {
                return InetAddress.getByName("255.255.1.1").getAddress();
            } catch (UnknownHostException e) {
                return null;
            }
        }
    }


    public void alert() {

        Put put = new Put(Bytes.toBytes(flow.get("flow:id")));
        put.addColumn(Bytes.toBytes("event"), Bytes.toBytes("note"), Bytes.toBytes(text));
        put.addColumn(Bytes.toBytes("event"), Bytes.toBytes("lower_ip"), formatIPtoBytes(flow.getLowerIp()));
        put.addColumn(Bytes.toBytes("event"), Bytes.toBytes("upper_ip"), formatIPtoBytes(flow.getUpperIp()));
        put.addColumn(Bytes.toBytes("event"), Bytes.toBytes("lower_ip_str"), Bytes.toBytes(flow.getLowerIp()));
        put.addColumn(Bytes.toBytes("event"), Bytes.toBytes("upper_ip_str"), Bytes.toBytes(flow.getUpperIp()));
        put.addColumn(Bytes.toBytes("event"), Bytes.toBytes("signature_id"), Bytes.toBytes("%.0f".format(signatureId.toString())));
        put.addColumn(Bytes.toBytes("event"), Bytes.toBytes("time"), Bytes.toBytes(System.currentTimeMillis()));
        put.addColumn(Bytes.toBytes("event"), Bytes.toBytes("ports"), Bytes.toBytes(ports));
        put.addColumn(Bytes.toBytes("event"), Bytes.toBytes("title"), Bytes.toBytes(title));

        if(!username.equals("")) {
            put.addColumn(Bytes.toBytes("event"), Bytes.toBytes("username"), Bytes.toBytes(username));
        }
        if(!coords.equals("")) {
            put.addColumn(Bytes.toBytes("event"), Bytes.toBytes("coords"), Bytes.toBytes(coords));
        }

        AdmlHBaseRDD admlHBaseRDD = AdmlHBaseRDD.getInstance();
        try {
            admlHBaseRDD.getEvents().put(put);
        } catch (IOException e) {
            e.printStackTrace();
        }

        //println(f"ALERT: $text%100s\n\n@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@")
    }
}
