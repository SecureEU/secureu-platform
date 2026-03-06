package ro.simavi.sphinx.ad.kernel.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.util.Bytes;
import ro.simavi.sphinx.ad.kernel.hbase.AdmlHBaseRDD;

import java.io.IOException;
import java.io.Serializable;

// din java 14 => record in loc de class
@Getter
@AllArgsConstructor
public final class AdmlSignature implements Serializable {

    //Example: 3,"AD: Suspicious DNS flow identified by K-Means clustering",2,1,826000001,826

    private Integer clazz;
    private String name;
    private Integer priority;
    private Integer revision;
    private Double id;
    private Integer groupId;

    public AdmlSignature saveHBase() {
        Get get = new Get(Bytes.toBytes("%.0f".format(id.toString())));

        AdmlHBaseRDD admlHBaseRDD = AdmlHBaseRDD.getInstance();

        try {
            if(!admlHBaseRDD.getSignatures().exists(get)) { //? admlHBaseRDD.getSensor().exists(get)
                Put put = new Put(Bytes.toBytes("%.0f".format(id.toString())));

                put.addColumn(Bytes.toBytes("signature"), Bytes.toBytes("id"), Bytes.toBytes("%.0f".format(id.toString())));
                put.addColumn(Bytes.toBytes("signature"), Bytes.toBytes("class"), Bytes.toBytes(clazz.toString()));
                put.addColumn(Bytes.toBytes("signature"), Bytes.toBytes("name"), Bytes.toBytes(name));
                put.addColumn(Bytes.toBytes("signature"), Bytes.toBytes("priority"), Bytes.toBytes(priority.toString()));
                put.addColumn(Bytes.toBytes("signature"), Bytes.toBytes("revision"), Bytes.toBytes(revision.toString()));
                put.addColumn(Bytes.toBytes("signature"), Bytes.toBytes("group_id"), Bytes.toBytes(groupId.toString()));

                admlHBaseRDD.getSignatures().put(put);

            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return this;
    }
}
