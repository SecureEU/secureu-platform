package ro.simavi.sphinx.ad.kernel.model.algorithm.sflow;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import ro.simavi.sphinx.ad.kernel.model.algorithm.SFlowAlgorithmDetails;

@Getter
@Setter
@ToString
public class AtypicalAmountDataAlgorithmDetails extends SFlowAlgorithmDetails {

    private String numberOfPairs;

    private String bytesUp;

    private String bytesDown;

    private String numberPkts;

    private String myIP;

    private String stringFlows;

    private String dataMean;

    private String dataStdev;

    public AtypicalAmountDataAlgorithmDetails(String numberOfPairs, String myIP, String bytesUp, String bytesDown, String numberPkts, String stringFlows, String dataMean, String dataStdev){
        super("AtypicalAmountData_sflow");
        setNumberOfPairs(numberOfPairs);
        setMyIP(myIP);
        setBytesUp(bytesUp);
        setBytesDown(bytesDown);
        setNumberPkts(numberPkts);
        setStringFlows(stringFlows);
        setDataMean(dataMean);
        setDataStdev(dataStdev);
    }

}
