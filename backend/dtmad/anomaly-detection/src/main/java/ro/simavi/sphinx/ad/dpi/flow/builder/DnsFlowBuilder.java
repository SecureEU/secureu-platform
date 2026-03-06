package ro.simavi.sphinx.ad.dpi.flow.builder;

import ro.simavi.sphinx.ad.dpi.flow.enums.RRType;
import ro.simavi.sphinx.ad.dpi.flow.model.DnsFlow;
import ro.simavi.sphinx.ad.dpi.flow.model.ProtocolFlow;
import ro.simavi.sphinx.model.enums.DnsType;
import ro.simavi.sphinx.model.event.HogzillaModel;
import ro.simavi.sphinx.model.event.NFstreamModel;
import java.time.LocalDateTime;


import java.util.*;

// deep packet inspection
// https://dzone.com/articles/dissecting-dns-communications
// https://www2.cs.duke.edu/courses/fall16/compsci356/DNS/DNS-primer.pdf
// https://sourcedaddy.com/networking/dns-protocol.html
public class DnsFlowBuilder extends CommonFlowBuilder {

    public static int NDPI_MAX_DNS_REQUESTS = 16;
    public static int RCODE_MASK = 0x0f;
    public static Map<RRType, Integer> rrTypeMap;


    // deprecated si comentat in barnyard2
//    private boolean isBadPachet(DnsFlow dnsFlow){
//
//        /*
//        if((dnsFlow.getNumQueries() > 0) && (dnsFlow.getNumQueries() <= NDPI_MAX_DNS_REQUESTS)
//                && (((dnsFlow.getFlags() & 0x2800) == 0x2800)
//                || ((dnsFlow.getNumAnswers() == 0) && (dnsFlow.getAuthorityRrs() == 0)))) {
//            return false;
//        }
//        */
//        long query = dnsFlow.getNumQueries();
//        long answer = dnsFlow.getNumAnswers();
//
//        if ((query <= NDPI_MAX_DNS_REQUESTS) &&
//                (answer == 0 || dnsFlow.getAuthorityRrs()==0 || dnsFlow.getAdditionalRrs()==0) && getRetCode(dnsFlow) !=0){
//
//            return false;
//        }
//
//        return true;
//
//    }

    private long getRetCode(NFstreamModel nfstreamModel){
        return getFlags(nfstreamModel) & RCODE_MASK;
    }


    private long getFlags(NFstreamModel nfstreamModel){
            String getModel = nfstreamModel.getFlags();
            if(getModel != ""){
                return Long.parseLong(getModel);
            }
        return 0;
    }

    private String splitData(String data){
        String[] parts1 = data.split("T");
        String date = parts1[0];
        String time = parts1[1];
        return date + " " + time;
    }


    private String getId(NFstreamModel nfstreamModel, DnsFlow dnsFlow) {
        String communityId =  nfstreamModel.getFlowId();
        String dataEvent = String.valueOf(dnsFlow.getTimeStamp2());
        String dataSplit = splitData(dataEvent);

        String splitData[] = dataSplit.split("-");
        String dataInitial = splitData[0] + splitData[1] + splitData[2];
        String dataSplt[] = dataInitial.split(" ");
        String splitTime[] = dataSplt[1].split(":");
        String dataFinal = dataSplt[0];
        String timeFinal = "";
        for(String time: splitTime){
            timeFinal += time;
        }
        return communityId +"."+ dataFinal + timeFinal;
    }

//    private long getAdditionalRrs(List<EventModel> eventModels) {
//        long count = 0;
//        for(EventModel eventModel: eventModels){
//            List list = ((DnsEventModel)eventModel).getDns().getAdditional();
//            if (list!=null) {
//                count = count + list.size();
//            }
//        }
//
//        return count;
//    }
//
//    private long getAuthorityRrs(List<EventModel> eventModels) {
//
//        long count = 0;
//        for(EventModel eventModel: eventModels){
//            List list = ((DnsEventModel)eventModel).getDns().getAuthorities();
//            if (list!=null) {
//                count = count + list.size();
//            }
//        }
//
//        return count;
//
//    }

    @Override
    public ProtocolFlow build(NFstreamModel model) {
        DnsFlow dnsFlow = new DnsFlow();
        dnsFlow.setDetectedProtocol(model.getProtocol());
        dnsFlow.setLowerIp(model.getSrcIp());
        dnsFlow.setUpperPort(Long.parseLong(model.getDestPort()));
        dnsFlow.setUpperIp(model.getDestIp());
        dnsFlow.setLowerPort(Long.parseLong(model.getSrcPort()));
        dnsFlow.setAvgPacketSize(Double.parseDouble(model.getAvgPacketSize()));
        dnsFlow.setFlowDuration(Long.parseLong(model.getFlowDuration()));
        dnsFlow.setBytes(Long.parseLong(model.getBytes()));
        dnsFlow.setPackets(Long.parseLong(model.getPackets()));
        dnsFlow.setFlags(getFlags(model));
        dnsFlow.setRetCode(getRetCode(model));
        dnsFlow.setAvgInterTime(Double.parseDouble(model.getAvgInterTime()));
        dnsFlow.setMaxPacketSize(Long.parseLong(model.getMaxPacketSize()));
        dnsFlow.setMinPacketSize(Long.parseLong(model.getMinPacketSize()));
        dnsFlow.setPacketsWithoutPayload(getPacketsWithoutPayload(model));

       dnsFlow.setInterTime0(Long.parseLong(getinterTime(model).get(INTER_TIME_0).toString()));
       dnsFlow.setPacketSize0(Long.parseLong(getPacketSize(model).get(PACKET_SIZE_0).toString()));
       dnsFlow.setPacketSize1(Long.parseLong(getPacketSize(model).get(PACKET_SIZE_1).toString()));

        dnsFlow.setNumQueries(getDnsTypeCount(model, DnsType.QUERY.getValue()));
        dnsFlow.setNumAnswers(getDnsTypeCount(model, DnsType.ANSWER.getValue()));

        dnsFlow.setQueryType(Integer.parseInt(model.getQueryType()));
        dnsFlow.setRspType(Integer.parseInt(model.getRspType()));
        dnsFlow.setHostname(model.getServerName());
        LocalDateTime dataFlow = LocalDateTime.now();
        dnsFlow.setTimeStamp2(dataFlow);
        dnsFlow.setId(getId(model, dnsFlow));
//        System.out.println("DNSSSSS===========================>>>" + dnsFlow);

        return dnsFlow;

          //nu mai trebuie
//        dnsFlow.setAuthorityRrs(getAuthorityRrs(eventModels));
//        dnsFlow.setAdditionalRrs(getAdditionalRrs(eventModels));
//        dnsFlow.setBadPacket(isBadPachet(dnsFlow) ? 1 : 0);


    }

    @Override
    public ProtocolFlow buildHogzilla(HogzillaModel model) {
        DnsFlow dnsFlow = new DnsFlow();
        dnsFlow.setDetectedProtocol("DNS");
        dnsFlow.setLowerIp(model.getSrcIp());
        dnsFlow.setUpperPort(Long.parseLong(model.getDestPort()));
        dnsFlow.setUpperIp(model.getDestIp());
        dnsFlow.setLowerPort(Long.parseLong(model.getSrcPort()));
        dnsFlow.setAvgPacketSize(Double.parseDouble(model.getAvgPacketSize()));
        dnsFlow.setFlowDuration(Long.parseLong(model.getFlowDuration()));
        dnsFlow.setBytes(Long.parseLong(model.getBytes()));
        dnsFlow.setPackets(Long.parseLong(model.getPackets()));
        dnsFlow.setPacketsWithoutPayload(Long.parseLong(model.getPacketsWithoutPayload()));
        dnsFlow.setRetCode(Long.parseLong(model.getDnsRetode()));
        dnsFlow.setAvgInterTime(Double.parseDouble(model.getAvgInterTime()));
        dnsFlow.setMaxPacketSize(Long.parseLong(model.getMaxPacketSize()));
        dnsFlow.setMinPacketSize(Long.parseLong(model.getMinPacketSize()));

        dnsFlow.setInterTime0(Long.parseLong(model.getInterTime0()));
        dnsFlow.setPacketSize0(Long.parseLong(model.getPacketSize0()));
        dnsFlow.setPacketSize1(Long.parseLong(model.getPacketSize1()));

        dnsFlow.setNumQueries(Long.parseLong(model.getDnsNumQueries()));
        dnsFlow.setNumAnswers(Long.parseLong(model.getDnsNumAnswers()));

        dnsFlow.setQueryType(Integer.parseInt(model.getDnsQueryType()));
        dnsFlow.setRspType(Integer.parseInt(model.getDnRspType()));
        dnsFlow.setHostname("clients1.google.ca");
        dnsFlow.setTimeStamp2(model.getTimestamp());
        dnsFlow.setId(String.valueOf(getHogzillaID()));
//        System.out.println("DNSSSSSS!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!" + dnsFlow);

        return dnsFlow;
    }




}
