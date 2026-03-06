package ro.simavi.sphinx.ad.dpi.flow.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

//Delete
@Getter
@Setter
@ToString(callSuper=true, includeFieldNames=true)
public class HttpFlow extends ProtocolFlow {

    private long method;

    private String url;

    private String hostServerName;

    public String toString(){
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("{AvgPacketSize: " + getAvgPacketSize());
        stringBuilder.append(", getAvgInterTime: " + getAvgInterTime());
        stringBuilder.append(", PacketsWithoutPayload " + getPacketsWithoutPayload());
        stringBuilder.append(", FlowDurationa: " + getFlowDuration());
        stringBuilder.append(", Max " + getMaxPacketSize());
        stringBuilder.append(", Min " + getMinPacketSize());
        stringBuilder.append(", Bytes: " + getBytes());
        stringBuilder.append(", Packets: " + getPackets());
        stringBuilder.append(", Intertime 0: " + getInterTime0());
//        stringBuilder.append(", Method: " + getMethod());
        stringBuilder.append(", Intertime 1: " + getInterTime1());
        stringBuilder.append(", Intertime 2: " + getInterTime2());
        stringBuilder.append(", Intertime 3: " + getInterTime3());
        stringBuilder.append(", Intertime 4: " + getInterTime4());
        stringBuilder.append("}");
        return stringBuilder.toString();
    }

}
