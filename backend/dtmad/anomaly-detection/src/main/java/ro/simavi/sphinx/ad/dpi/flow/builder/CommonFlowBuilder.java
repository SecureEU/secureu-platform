package ro.simavi.sphinx.ad.dpi.flow.builder;

import ro.simavi.sphinx.model.event.NFstreamModel;

import java.util.HashMap;

public abstract class CommonFlowBuilder implements FlowBuilder {

    public static HashMap mapHttp;
    public static int i = 0;

    public static String INTER_TIME_0 = "interTime0";
    public static String INTER_TIME_1 = "interTime1";
    public static String INTER_TIME_2 = "interTime2";
    public static String INTER_TIME_3 = "interTime3";
    public static String INTER_TIME_4 = "interTime4";

    public static String PACKET_SIZE_0 = "packetSize0";
    public static String PACKET_SIZE_1 = "packetSize1";
    public static String PACKET_SIZE_2 = "packetSize2";
    public static String PACKET_SIZE_3 = "packetSize3";
    public static String PACKET_SIZE_4 = "packetSize4";

    protected long getPacketsWithoutPayload(NFstreamModel nfstreamModel){
        double bytesToClient = Double.parseDouble(nfstreamModel.getSrcToDstBytes());
        double bytesToServer = Double.parseDouble(nfstreamModel.getDstToSrcBytes());
        if (bytesToClient == 0 || bytesToServer == 0) {
            return 1;
        }
        return 0;
    }

    protected String splitParanthese(String kmeansModel){
        String inter_time = kmeansModel;
        String[] arrSplitParantheses1 = inter_time.split("\\[");
        String[] arrSplitParantheses2 = arrSplitParantheses1[1].split("]");
        String splitFinal = arrSplitParantheses2[0];

        return splitFinal;
    }

    protected HashMap getinterTime(NFstreamModel nfstreamModel){

        long inter_time_0 = 0;
        long inter_time_1 = 0;
        long inter_time_2 = 0;
        long inter_time_3 = 0;
        long inter_time_4 = 0;

        String withParanthese = nfstreamModel.getSplt_piat_ms();
        String result = splitParanthese(withParanthese);

        String[] splitTime = result.split(", ");
        for(int i=0;i<splitTime.length; i++){
            if(splitTime[i].equals("-1")){
                splitTime[i] = "0";
            }
        }

        inter_time_0 = Long.parseLong(splitTime[1]);
        inter_time_1 = Long.parseLong(splitTime[2]);
        inter_time_2 = Long.parseLong(splitTime[3]);
        inter_time_3 = Long.parseLong(splitTime[4]);
        inter_time_4 = Long.parseLong(splitTime[5]);

        mapHttp = new HashMap();
        mapHttp.put(INTER_TIME_0, inter_time_0);
        mapHttp.put(INTER_TIME_1, inter_time_1);
        mapHttp.put(INTER_TIME_2, inter_time_2);
        mapHttp.put(INTER_TIME_3, inter_time_3);
        mapHttp.put(INTER_TIME_4, inter_time_4);

        return mapHttp;
    }

    protected HashMap getPacketSize(NFstreamModel nfstreamModel){

        long packet_size_0 = 0;
        long packet_size_1 = 0;
        long packet_size_2 = 0;
        long packet_size_3 = 0;
        long packet_size_4 = 0;


        String withParanthese = nfstreamModel.getSplt_ps();
        String result = splitParanthese(withParanthese);

        String[] splitPacketSize = result.split(", ");

        for(int i=0;i< splitPacketSize.length; i++){
            if(splitPacketSize[i].equals("-1")){
                splitPacketSize[i] = "0";
            }
        }

        packet_size_0 = Long.parseLong(splitPacketSize[0]);
        packet_size_1 = Long.parseLong(splitPacketSize[1]);
        packet_size_2 = Long.parseLong(splitPacketSize[2]);
        packet_size_3 = Long.parseLong(splitPacketSize[3]);
        packet_size_4 = Long.parseLong(splitPacketSize[4]);

        mapHttp = new HashMap();
        mapHttp.put(PACKET_SIZE_0, packet_size_0);
        mapHttp.put(PACKET_SIZE_1, packet_size_1);
        mapHttp.put(PACKET_SIZE_2, packet_size_2);
        mapHttp.put(PACKET_SIZE_3, packet_size_3);
        mapHttp.put(PACKET_SIZE_4, packet_size_4);

        return mapHttp;
    }

    protected long getDnsTypeCount(NFstreamModel nfstreamModel, String dnsType){
        long count = 0;

        String withParanthese = nfstreamModel.getSplt_direction();
        String result = splitParanthese(withParanthese);

        String[] currentDnsType = result.split(", ");

        for(int i = 0; i< currentDnsType.length; i++){
            if(currentDnsType[i] != null && currentDnsType[i].equals(dnsType)){
                count++;
            }
        }
        return count;
    }

    protected int getHogzillaID(){
        return i++;
    }


}
