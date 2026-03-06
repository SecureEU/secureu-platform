package ro.simavi.sphinx.ad.kernel.model.algorithm.sflow;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import ro.simavi.sphinx.ad.kernel.model.algorithm.SFlowAlgorithmDetails;

@Getter
@Setter
@ToString
public class AtypicalNumberOfPairsAlgorithmDetails extends SFlowAlgorithmDetails {

    private String numberOfPairs;

    private String bytesUp;

    private String bytesDown;

    private String numberPkts;

    private String myIP;

    private String stringFlows;

    private String pairsMean;

    private String pairsStdev;

    public AtypicalNumberOfPairsAlgorithmDetails(String numberOfPairs, String myIP, String bytesUp, String bytesDown, String numberPkts, String stringFlows, String pairsMean, String pairsStdev){
        super("AtypicalNumberOfPairs_sflow");
        setNumberOfPairs(numberOfPairs);
        setMyIP(myIP);
        setBytesUp(bytesUp);
        setBytesDown(bytesDown);
        setNumberPkts(numberPkts);
        setStringFlows(stringFlows);
        setPairsMean(pairsMean);
        setPairsStdev(pairsStdev);
    }

}
