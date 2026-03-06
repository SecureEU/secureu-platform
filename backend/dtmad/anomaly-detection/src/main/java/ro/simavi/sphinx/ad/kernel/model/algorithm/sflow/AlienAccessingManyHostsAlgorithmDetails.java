package ro.simavi.sphinx.ad.kernel.model.algorithm.sflow;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import ro.simavi.sphinx.ad.kernel.model.algorithm.SFlowAlgorithmDetails;

@Getter
@Setter
@ToString
public class AlienAccessingManyHostsAlgorithmDetails extends SFlowAlgorithmDetails {

    private String numberOfPairs;

    private String alienIP;

    private String bytesUp;

    private String bytesDown;

    private String numberPkts;

    private String ports;

    private String stringFlows;

    public AlienAccessingManyHostsAlgorithmDetails(String numberOfPairs, String alienIP , String bytesUp, String bytesDown, String numberPkts, String stringFlows, String ports){
        super("AlienAccessingManyHosts_sflow");
        setNumberOfPairs(numberOfPairs);
        setAlienIP(alienIP);
        setBytesUp(bytesUp);
        setBytesDown(bytesDown);
        setNumberPkts(numberPkts);
        setStringFlows(stringFlows);
        setPorts(ports);
    }

}
