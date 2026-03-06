package ro.simavi.sphinx.ad.dpi.flow.builder;

import org.apache.commons.lang.StringUtils;
import ro.simavi.sphinx.ad.dpi.flow.model.HttpFlow;
import ro.simavi.sphinx.ad.dpi.flow.model.ProtocolFlow;
import ro.simavi.sphinx.model.event.*;
import java.time.LocalDateTime;


public class HttpFlowBuilder extends CommonFlowBuilder  {

//    private Integer getHttpMethod(List<EventModel> eventModels) {
//        for(EventModel model : eventModels) {
//
//            if (model instanceof HttpEventModel){
//
//                HttpModel httpModel = ((HttpEventModel)model).getHttp();
//
//                if (httpModel!=null) {
//                    String httpMethod = httpModel.getHttpMethod();
//                    MethodHttp[] methods = MethodHttp.values();
//                    for (MethodHttp method : methods) {
//                        if (httpMethod != null && httpMethod.equals(method.name())) {
//                            return method.getCode();
//                        }
//                    }
//                }else{
//                    System.out.println(model);
//                }
//
//            }
//        }
//
//        return null;
//    }
//
//
//    private String getHostName(List<EventModel> eventModels) {
//        for(EventModel model : eventModels) {
//
//            if (model instanceof HttpEventModel) {
//                HttpModel httpModel = ((HttpEventModel) model).getHttp();
//
//                if (httpModel != null) {
//                    String hostname = httpModel.getHostname();
//                    if (hostname != null && !"".equals(hostname)) {
//                        return hostname;
//                    }
//                }
//            }
//        }
//
//        return null;
//    }
    private String splitData(String data){
        String[] parts1 = data.split("T");
        String date = parts1[0];
        String time = parts1[1];
        return date + " " + time;
    }


    private String getId(NFstreamModel nfstreamModel, HttpFlow httpFlow) {
        String communityId =  nfstreamModel.getFlowId();
        String dataEvent = String.valueOf(httpFlow.getTimeStamp2());
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

    @Override
    public ProtocolFlow build(NFstreamModel model) {

        HttpFlow httpFlow = new HttpFlow();
        httpFlow.setDetectedProtocol(model.getProtocol());
        httpFlow.setLowerIp(model.getSrcIp());
        httpFlow.setUpperPort(Long.parseLong(model.getDestPort()));
        httpFlow.setUpperIp(model.getDestIp());
        httpFlow.setLowerPort(Long.parseLong(model.getSrcPort()));
        httpFlow.setAvgPacketSize(Double.parseDouble(model.getAvgPacketSize()));
        httpFlow.setFlowDuration(Long.parseLong(model.getFlowDuration()));
        httpFlow.setBytes(Long.parseLong(model.getBytes()));
        httpFlow.setPackets(Long.parseLong(model.getPackets()));
        httpFlow.setAvgInterTime(Double.parseDouble(model.getAvgInterTime()));
        httpFlow.setMaxPacketSize(Long.parseLong(model.getMaxPacketSize()));
        httpFlow.setMinPacketSize(Long.parseLong(model.getMinPacketSize()));
        httpFlow.setPacketsWithoutPayload(getPacketsWithoutPayload(model));
        httpFlow.setInterTime0(Long.parseLong(getinterTime(model).get(INTER_TIME_0).toString()));
        httpFlow.setInterTime1(Long.parseLong(getinterTime(model).get(INTER_TIME_1).toString()));
        httpFlow.setInterTime2(Long.parseLong(getinterTime(model).get(INTER_TIME_2).toString()));
        httpFlow.setInterTime3(Long.parseLong(getinterTime(model).get(INTER_TIME_3).toString()));
        httpFlow.setInterTime4(Long.parseLong(getinterTime(model).get(INTER_TIME_4).toString()));

        httpFlow.setPacketSize0(Long.parseLong(getPacketSize(model).get(PACKET_SIZE_0).toString()));
        httpFlow.setPacketSize1(Long.parseLong(getPacketSize(model).get(PACKET_SIZE_1).toString()));
        httpFlow.setPacketSize2(Long.parseLong(getPacketSize(model).get(PACKET_SIZE_2).toString()));
        httpFlow.setPacketSize3(Long.parseLong(getPacketSize(model).get(PACKET_SIZE_3).toString()));
        httpFlow.setPacketSize4(Long.parseLong(getPacketSize(model).get(PACKET_SIZE_4).toString()));
        LocalDateTime dataFlow = LocalDateTime.now();
        httpFlow.setTimeStamp2(dataFlow);
        httpFlow.setId(getId(model, httpFlow));
        httpFlow.setMethod(model.getHttpMethod());
        if (StringUtils.isEmpty(model.getHttpUrl())){
            httpFlow.setUrl("");
        }else{
            String[] splitUrl = (model.getHttpUrl()).split(" ");
            httpFlow.setUrl(splitUrl[0]);
        }


        httpFlow.setHostServerName(model.getHostServerName());
//        System.out.println("HTTTTTPPPPPP================>" + httpFlow);

        return httpFlow;
    }

    @Override
    public ProtocolFlow buildHogzilla(HogzillaModel model) {
        HttpFlow httpFlow = new HttpFlow();
        httpFlow.setId(String.valueOf(getHogzillaID()));
        httpFlow.setDetectedProtocol("HTTP");
        httpFlow.setLowerIp(model.getSrcIp());
        httpFlow.setUpperPort(Long.parseLong(model.getDestPort()));
        httpFlow.setUpperIp(model.getDestIp());
        httpFlow.setLowerPort(Long.parseLong(model.getSrcPort()));
        httpFlow.setAvgPacketSize(Double.parseDouble(model.getAvgPacketSize()));
        httpFlow.setFlowDuration(Long.parseLong(model.getFlowDuration()));
        httpFlow.setBytes(Long.parseLong(model.getBytes()));
        httpFlow.setPackets(Long.parseLong(model.getPackets()));
        httpFlow.setAvgInterTime(Double.parseDouble(model.getAvgInterTime()));
        httpFlow.setMaxPacketSize(Long.parseLong(model.getMaxPacketSize()));
        httpFlow.setMinPacketSize(Long.parseLong(model.getMinPacketSize()));
        httpFlow.setPacketsWithoutPayload(Long.parseLong(model.getPacketsWithoutPayload()));
        httpFlow.setInterTime0(Long.parseLong(model.getInterTime0()));
        httpFlow.setInterTime1(Long.parseLong(model.getInterTime1()));
        httpFlow.setInterTime2(Long.parseLong(model.getInterTime2()));
        httpFlow.setInterTime3(Long.parseLong(model.getInterTime3()));
        httpFlow.setInterTime4(Long.parseLong(model.getInterTime4()));

        httpFlow.setPacketSize0(Long.parseLong(model.getPacketSize0()));
        httpFlow.setPacketSize1(Long.parseLong(model.getPacketSize1()));
        httpFlow.setPacketSize2(Long.parseLong(model.getPacketSize2()));
        httpFlow.setPacketSize3(Long.parseLong(model.getPacketSize3()));
        httpFlow.setPacketSize4(Long.parseLong(model.getPacketSize4()));
        httpFlow.setTimeStamp2(model.getTimestamp());
        httpFlow.setMethod(model.getHttpMethod());
        httpFlow.setHostServerName("clients1.google.ca");
        httpFlow.setUrl("clients1.google.ca/complete/search?client=chrome&hl=en-US&q=cr");
//        System.out.println("HTTTTTPPPPPP!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!" + httpFlow);

        return httpFlow;
    }

}
