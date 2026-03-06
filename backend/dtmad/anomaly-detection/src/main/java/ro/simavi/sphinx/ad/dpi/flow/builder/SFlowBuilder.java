package ro.simavi.sphinx.ad.dpi.flow.builder;

import ro.simavi.sphinx.ad.dpi.flow.model.ProtocolFlow;
import ro.simavi.sphinx.model.event.NFstreamModel;

import java.time.LocalDateTime;

public class SFlowBuilder {

    public ProtocolFlow buildSFlow(NFstreamModel model) {
        ProtocolFlow protocolFlow = new ProtocolFlow();
        try {
            protocolFlow.setId(model.getFlowId());
            protocolFlow.setLowerIp(model.getSrcIp());
            protocolFlow.setUpperIp(model.getDestIp());
            protocolFlow.setLowerPort(Long.parseLong(model.getSrcPort()));
            protocolFlow.setUpperPort(Long.parseLong(model.getDestPort()));
            protocolFlow.setIpProtocol(Long.parseLong(model.getIpProtocol()));
            protocolFlow.setAvgPacketSize(Double.parseDouble(model.getAvgPacketSize())); //packetSize
            LocalDateTime dataFlow = LocalDateTime.now();
            protocolFlow.setTimeStamp2(dataFlow);
            Long flags = 0L;
            try{
                flags = Long.parseLong(model.getFlags());
            }catch (Exception e){

            }
            protocolFlow.setFlags(flags);

        } catch(NumberFormatException ex){
            ex.printStackTrace();
        }
        return (protocolFlow);
    }
}
