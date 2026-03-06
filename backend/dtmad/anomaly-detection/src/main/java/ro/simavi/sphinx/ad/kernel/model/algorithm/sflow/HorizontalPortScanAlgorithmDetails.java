package ro.simavi.sphinx.ad.kernel.model.algorithm.sflow;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import ro.simavi.sphinx.ad.kernel.model.algorithm.SFlowAlgorithmDetails;

@Getter
@Setter
@ToString
public class HorizontalPortScanAlgorithmDetails extends SFlowAlgorithmDetails {

    private String numberOfFlows;

    private String numberOfFlowsPerPort;

    private String myIP;

    private String bytesUp;

    private String bytesDown;

    private String numberPkts;

    private String flowsMean;

    private String stringFlows;

    private String flowsStdev;

    private String numberOfFlowsAlienPort;

    private String ports;

    public HorizontalPortScanAlgorithmDetails( String numberOfFlows, String  numberOfFlowsPerPort,  String myIP,
                                              String bytesUp, String bytesDown,  String numberPkts,  String stringFlows,
                                              String flowsMean,  String flowsStdev,  String numberOfFlowsAlienPort,  String ports){
        super("HorizontalPortScan_sflow");
        setNumberOfFlows(numberOfFlows);
        setNumberOfFlowsPerPort(numberOfFlowsPerPort);
        setMyIP(myIP);
        setBytesUp(bytesUp);
        setBytesDown(bytesDown);
        setNumberPkts(numberPkts);
        setStringFlows(stringFlows);
        setFlowsMean(flowsMean);
        setFlowsStdev(flowsStdev);
        setNumberOfFlowsAlienPort(numberOfFlowsAlienPort);
        setPorts(ports);
    }

}
