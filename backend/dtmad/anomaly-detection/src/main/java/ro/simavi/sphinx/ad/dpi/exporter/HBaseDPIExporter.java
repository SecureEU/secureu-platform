package ro.simavi.sphinx.ad.dpi.exporter;

import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.util.Bytes;
import ro.simavi.sphinx.ad.dpi.flow.model.DnsFlow;
import ro.simavi.sphinx.ad.dpi.flow.model.HttpFlow;
import ro.simavi.sphinx.ad.dpi.flow.model.ProtocolFlow;
import ro.simavi.sphinx.ad.kernel.hbase.AdmlHBaseRDD;

import java.io.IOException;
import java.time.LocalDateTime;

public class HBaseDPIExporter implements DPIExporter{

    @Override
    public void save(ProtocolFlow protocolFlow) {
        /* todo 4: de salvat protocolFlow in HBase in tabela: 'adml_flows'. legat de coloane se gasesc detalii in clasa 'AdmlHBaseRDD' */

        Table table = AdmlHBaseRDD.getInstance().getFlows();

            try {
                String id = String.valueOf(protocolFlow.getId());

                byte[] columnFamily = Bytes.toBytes("flow");
                byte[] eventColumnFamily = Bytes.toBytes("event");
                Put putFlow = new Put(Bytes.toBytes(id));

                if (protocolFlow.getDetectedProtocol().startsWith("DNS") || protocolFlow.getDetectedProtocol().startsWith("HTTP")){

                // common
                    putFlow.addColumn(columnFamily, Bytes.toBytes("avg_packet_size"),Bytes.toBytes(String.valueOf(protocolFlow.getAvgPacketSize())));
                    putFlow.addColumn(columnFamily, Bytes.toBytes("packets_without_payload"),Bytes.toBytes(String.valueOf(protocolFlow.getPacketsWithoutPayload())));
                    putFlow.addColumn(columnFamily, Bytes.toBytes("avg_inter_time"),Bytes.toBytes(String.valueOf(protocolFlow.getAvgInterTime())));
                    putFlow.addColumn(columnFamily, Bytes.toBytes("flow_duration"),Bytes.toBytes(String.valueOf(protocolFlow.getFlowDuration())));
                    putFlow.addColumn(columnFamily, Bytes.toBytes("max_packet_size"),Bytes.toBytes(String.valueOf(protocolFlow.getMaxPacketSize())));
                    putFlow.addColumn(columnFamily, Bytes.toBytes("bytes"),Bytes.toBytes(String.valueOf(protocolFlow.getBytes())));
                    putFlow.addColumn(columnFamily, Bytes.toBytes("packets"),Bytes.toBytes(String.valueOf(protocolFlow.getPackets())));
                    putFlow.addColumn(columnFamily, Bytes.toBytes("min_packet_size"),Bytes.toBytes(String.valueOf(protocolFlow.getMinPacketSize())));
                    putFlow.addColumn(columnFamily, Bytes.toBytes("lower_port"),Bytes.toBytes(String.valueOf(protocolFlow.getLowerPort())));
                    putFlow.addColumn(columnFamily, Bytes.toBytes("lower_ip"),Bytes.toBytes(String.valueOf(protocolFlow.getLowerIp())));
                    putFlow.addColumn(columnFamily, Bytes.toBytes("upper_ip"),Bytes.toBytes(String.valueOf(protocolFlow.getUpperIp())));
                    putFlow.addColumn(columnFamily, Bytes.toBytes("upper_port"),Bytes.toBytes(String.valueOf(protocolFlow.getUpperPort())));

                    putFlow.addColumn(eventColumnFamily, Bytes.toBytes("priority_id"),Bytes.toBytes(String.valueOf("1"))); //?

                    if (protocolFlow.getDetectedProtocol().startsWith("DNS")){
                        putFlow.addColumn(columnFamily, Bytes.toBytes("detected_protocol"),Bytes.toBytes(String.valueOf("dns")));
                        putFlow.addColumn(columnFamily, Bytes.toBytes("packet_size-1"),Bytes.toBytes(String.valueOf(protocolFlow.getPacketSize1())));
                        putFlow.addColumn(columnFamily, Bytes.toBytes("packet_size-0"),Bytes.toBytes(String.valueOf(protocolFlow.getPacketSize0())));
                        putFlow.addColumn(columnFamily, Bytes.toBytes("inter_time-0"),Bytes.toBytes(String.valueOf(protocolFlow.getInterTime0())));
                        putFlow.addColumn(columnFamily, Bytes.toBytes("dns_num_queries"),Bytes.toBytes(String.valueOf(((DnsFlow) protocolFlow).getNumQueries())));
                        putFlow.addColumn(columnFamily, Bytes.toBytes("dns_num_answers"),Bytes.toBytes(String.valueOf(((DnsFlow) protocolFlow).getNumAnswers())));
                        putFlow.addColumn(columnFamily, Bytes.toBytes("dns_ret_code"),Bytes.toBytes(String.valueOf(((DnsFlow) protocolFlow).getRetCode())));
                        //   put(admlFlowsTable,id,"flow","dns_bad_packet",String.valueOf(dnsFlow.getBadPacket()));  -- nu se foloseste
                        putFlow.addColumn(columnFamily, Bytes.toBytes("dns_query_type"),Bytes.toBytes(String.valueOf(((DnsFlow) protocolFlow).getQueryType())));
                        putFlow.addColumn(columnFamily, Bytes.toBytes("dns_rsp_type"),Bytes.toBytes(String.valueOf(((DnsFlow) protocolFlow).getRspType())));
                        putFlow.addColumn(columnFamily, Bytes.toBytes("host_server_name"),Bytes.toBytes(String.valueOf(protocolFlow.getHostname())));
                    }
                    if (protocolFlow.getDetectedProtocol().startsWith("HTTP")){
                        putFlow.addColumn(columnFamily, Bytes.toBytes("detected_protocol"),Bytes.toBytes(String.valueOf("http")));
                        putFlow.addColumn(columnFamily, Bytes.toBytes("inter_time-0"),Bytes.toBytes(String.valueOf(protocolFlow.getInterTime0())));
                        putFlow.addColumn(columnFamily, Bytes.toBytes("inter_time-1"),Bytes.toBytes(String.valueOf(protocolFlow.getInterTime1())));
                        putFlow.addColumn(columnFamily, Bytes.toBytes("inter_time-2"),Bytes.toBytes(String.valueOf(protocolFlow.getInterTime2())));
                        putFlow.addColumn(columnFamily, Bytes.toBytes("inter_time-3"),Bytes.toBytes(String.valueOf(protocolFlow.getInterTime3())));
                        putFlow.addColumn(columnFamily, Bytes.toBytes("inter_time-4"),Bytes.toBytes(String.valueOf(protocolFlow.getInterTime4())));
                        putFlow.addColumn(columnFamily, Bytes.toBytes("packet_size-0"),Bytes.toBytes(String.valueOf(protocolFlow.getPacketSize0())));
                        putFlow.addColumn(columnFamily, Bytes.toBytes("packet_size-1"),Bytes.toBytes(String.valueOf(protocolFlow.getPacketSize1())));
                        putFlow.addColumn(columnFamily, Bytes.toBytes("packet_size-2"),Bytes.toBytes(String.valueOf(protocolFlow.getPacketSize2())));
                        putFlow.addColumn(columnFamily, Bytes.toBytes("packet_size-3"),Bytes.toBytes(String.valueOf(protocolFlow.getPacketSize3())));
                        putFlow.addColumn(columnFamily, Bytes.toBytes("packet_size-4"),Bytes.toBytes(String.valueOf(protocolFlow.getPacketSize4())));
                        putFlow.addColumn(columnFamily, Bytes.toBytes("http_method"), Bytes.toBytes(String.valueOf(((HttpFlow) protocolFlow).getMethod())));
                        putFlow.addColumn(columnFamily, Bytes.toBytes("http_url"), Bytes.toBytes(String.valueOf(((HttpFlow) protocolFlow).getUrl())));
                        putFlow.addColumn(columnFamily, Bytes.toBytes("host_server_name"),Bytes.toBytes(String.valueOf(((HttpFlow) protocolFlow).getHostServerName())));
                    }
                }
                table.put(putFlow);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }

    @Override
    public void saveSflow(ProtocolFlow protocolFlow) {
        Table table = AdmlHBaseRDD.getInstance().getSflowsTable();

        try {
            String id = String.valueOf(protocolFlow.getId());
            byte[] columnFamily = Bytes.toBytes("flow");
            Put putFlow = new Put(Bytes.toBytes(id));

            putFlow.addColumn(columnFamily, Bytes.toBytes("IPprotocol"),Bytes.toBytes(String.valueOf(protocolFlow.getIpProtocol())));
            putFlow.addColumn(columnFamily, Bytes.toBytes("dstIP"),Bytes.toBytes(protocolFlow.getUpperIp()));
            putFlow.addColumn(columnFamily, Bytes.toBytes("dstPort"),Bytes.toBytes(String.valueOf(protocolFlow.getUpperPort())));
            putFlow.addColumn(columnFamily, Bytes.toBytes("packetSize"),Bytes.toBytes(String.valueOf(protocolFlow.getAvgPacketSize())));
            putFlow.addColumn(columnFamily, Bytes.toBytes("samplingRate"),Bytes.toBytes("1"));
            putFlow.addColumn(columnFamily, Bytes.toBytes("srcIP"),Bytes.toBytes(protocolFlow.getLowerIp()));
            putFlow.addColumn(columnFamily, Bytes.toBytes("srcPort"),Bytes.toBytes(String.valueOf(protocolFlow.getLowerPort())));
            putFlow.addColumn(columnFamily, Bytes.toBytes("tcpFlags"),Bytes.toBytes(String.valueOf(protocolFlow.getFlags())));
            putFlow.addColumn(columnFamily, Bytes.toBytes("timestamp"),Bytes.toBytes(String.valueOf(protocolFlow.getTimeStamp2())));

            table.put(putFlow);

        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void saveSflow(String[] csvLine) {
        Table table = AdmlHBaseRDD.getInstance().getSflowsTable();

        try {
            String id = csvLine[0];
            LocalDateTime dataFlow = LocalDateTime.now();

            byte[] columnFamily = Bytes.toBytes("flow");
            Put putFlow = new Put(Bytes.toBytes(id));
            putFlow.addColumn(columnFamily, Bytes.toBytes("IPprotocol"),Bytes.toBytes(csvLine[10]));
            putFlow.addColumn(columnFamily, Bytes.toBytes("dstIP"),Bytes.toBytes(csvLine[6]));
            putFlow.addColumn(columnFamily, Bytes.toBytes("dstPort"),Bytes.toBytes(csvLine[9]));
            putFlow.addColumn(columnFamily, Bytes.toBytes("packetSize"),Bytes.toBytes(csvLine[29]));
            putFlow.addColumn(columnFamily, Bytes.toBytes("samplingRate"),Bytes.toBytes("1"));
            putFlow.addColumn(columnFamily, Bytes.toBytes("srcIP"),Bytes.toBytes(csvLine[2]));
            putFlow.addColumn(columnFamily, Bytes.toBytes("srcPort"),Bytes.toBytes(csvLine[5]));
            putFlow.addColumn(columnFamily, Bytes.toBytes("tcpFlags"),Bytes.toBytes(csvLine[89]));
            putFlow.addColumn(columnFamily, Bytes.toBytes("timestamp"),Bytes.toBytes(String.valueOf(dataFlow)));
            table.put(putFlow);

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

//    private void put(Table table, String rowKey, String columnFamily, String column, String value) throws IOException {
//        Put putFlow = new Put(Bytes.toBytes(rowKey));
//        putFlow.addColumn(Bytes.toBytes(columnFamily), Bytes.toBytes(column),Bytes.toBytes(value));
//        table.put(putFlow);
//    }

    @Override
    public void endProcess() {

    }

}
