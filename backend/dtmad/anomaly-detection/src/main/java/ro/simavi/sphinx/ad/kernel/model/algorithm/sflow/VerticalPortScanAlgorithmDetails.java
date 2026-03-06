package ro.simavi.sphinx.ad.kernel.model.algorithm.sflow;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import ro.simavi.sphinx.ad.kernel.model.algorithm.SFlowAlgorithmDetails;

@Getter
@Setter
@ToString
public class VerticalPortScanAlgorithmDetails extends SFlowAlgorithmDetails {

    private String numberOfFlows;

    private String numberOfPorts;

    private String myIP;

    private String bytesUp;

    private String bytesDown;

    private String numberPkts;

    private String stringFlows;

    private String portsMean;

    private String portsStdev;

    public VerticalPortScanAlgorithmDetails(
            String numberOfFlows, String numberOfPorts, String myIP, String bytesUp, String bytesDown, String numberPkts,String  stringFlows,String portsMean,String  portsStdev){
        super("VerticalPortScan_sflow");
        setNumberOfFlows(numberOfFlows);
        setNumberOfPorts(numberOfPorts);
        setMyIP(myIP);
        setBytesUp(bytesUp);
        setBytesDown(bytesDown);
        setNumberPkts(numberPkts);
        setStringFlows(stringFlows);
        setPortsMean(portsMean);
        setPortsStdev(portsStdev);

    }

}
