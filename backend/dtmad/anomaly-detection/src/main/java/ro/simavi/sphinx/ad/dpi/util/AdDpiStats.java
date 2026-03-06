package ro.simavi.sphinx.ad.dpi.util;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdDpiStats {

    private long eventCount = 0;

    private long flowCount = 0;

    private long singularFlowCount = 0;

    private long packets = 0;

    private long bytes = 0;

    public void incrementEventCount() {
        eventCount++;
    }

    public void incrementFlowCount() {
        flowCount++;
    }

    public void incrementSingularFlowCount() {
        singularFlowCount++;
    }

    public void incrementPackets() {
        packets++;
    }
    
    public String toString(){
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("{");
        stringBuilder.append("event-count:"+eventCount);
        stringBuilder.append(",packets:"+packets);
        stringBuilder.append(",bytes:"+bytes);
        stringBuilder.append(",flow-count:"+flowCount);
        stringBuilder.append(",singular-flow-count:"+singularFlowCount);
        stringBuilder.append("}");
        return stringBuilder.toString();
    }

    public void addBytes(Long value) {
        bytes = bytes + value;
    }
}
