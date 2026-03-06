package ro.simavi.sphinx.ad.kernel.model.algorithm.sflow;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import ro.simavi.sphinx.ad.kernel.model.algorithm.SFlowAlgorithmDetails;


@Getter
@Setter
@ToString
public class P2PCommunicationAlgorithmDetails extends SFlowAlgorithmDetails {

    private String numberOfPairs;

    private String myIP;

    private String bytesUp;

    private String bytesDown;

    private String numberPkts;

    private String stringFlows;

    public P2PCommunicationAlgorithmDetails(String numberOfPairs, String myIP, String bytesUp, String bytesDown, String numberPkts, String stringFlows){
        super("P2PCommunication_sflow");
        setNumberOfPairs(numberOfPairs);
        setMyIP(myIP);
        setBytesUp(bytesUp);
        setBytesDown(bytesDown);
        setNumberPkts(numberPkts);
        setStringFlows(stringFlows);
    }
}
