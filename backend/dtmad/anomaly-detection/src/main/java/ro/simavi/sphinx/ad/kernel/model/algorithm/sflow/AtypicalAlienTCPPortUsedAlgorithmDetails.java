package ro.simavi.sphinx.ad.kernel.model.algorithm.sflow;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import ro.simavi.sphinx.ad.kernel.model.algorithm.SFlowAlgorithmDetails;

@Getter
@Setter
@ToString
public class AtypicalAlienTCPPortUsedAlgorithmDetails extends SFlowAlgorithmDetails {

    private String tcpport;

    private String bytesUp;

    private String bytesDown;

    private String numberPkts;

    private String myIP;

    private String stringFlows;

    public AtypicalAlienTCPPortUsedAlgorithmDetails(String tcpport, String myIP, String bytesUp, String bytesDown, String numberPkts, String stringFlows){
        super("AtypicalAlienTCPPortUsed_sflow");
        setTcpport(tcpport);
        setBytesUp(bytesUp);
        setBytesDown(bytesDown);
        setNumberPkts(numberPkts);
        setStringFlows(stringFlows);
        setMyIP(myIP);
    }

}
