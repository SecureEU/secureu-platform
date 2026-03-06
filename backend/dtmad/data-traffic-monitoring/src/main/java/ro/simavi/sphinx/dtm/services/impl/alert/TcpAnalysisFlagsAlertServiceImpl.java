package ro.simavi.sphinx.dtm.services.impl.alert;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import ro.simavi.sphinx.model.event.alert.SphinxModel;
import ro.simavi.sphinx.model.event.alert.TcpAnalysisFlagsAlertModel;
import ro.simavi.sphinx.dtm.services.MessagingSystemService;
import ro.simavi.sphinx.dtm.services.alert.TcpAnalysisFlagsAlertService;
import ro.simavi.sphinx.dtm.util.SphinxModelHelper;
import ro.simavi.sphinx.model.event.AlertMetadataModel;
import ro.simavi.sphinx.model.event.AlertModel;
import ro.simavi.sphinx.model.event.EventModel;
import ro.simavi.sphinx.util.FormatHelper;
import ro.simavi.sphinx.util.PackageFields;

@Service
public class TcpAnalysisFlagsAlertServiceImpl implements TcpAnalysisFlagsAlertService {

    private final MessagingSystemService messagingSystemService;

    public TcpAnalysisFlagsAlertServiceImpl(MessagingSystemService messagingSystemService){
        this.messagingSystemService = messagingSystemService;
    }

    private final static String FILTER_CODE = "tcp.analysis.flags";

    @Override
    public void detect(String[] pcapMessage) {
        String filterCode = pcapMessage[PackageFields.FILTER_CODE];
        if (filterCode==null || filterCode.equals("NO_FILTER_CODE")){
            return;
        }

        if (!filterCode.equals(FILTER_CODE)){
            return;
        }

        String protocolsString = pcapMessage[PackageFields.FRAME_PROTOCOLS];
        if (StringUtils.isEmpty(protocolsString)){
            return;
        }

        String[] protocols = protocolsString.split(":");
        boolean isIPv4 = isProtocol(protocols,"ip");
        boolean isIPv6= isProtocol(protocols,"ipv6");

        String protocol = "tcp";
        String addressA = null;
        String addressB = null;
        String portA = pcapMessage[PackageFields.TCP_SRC_PORT];
        String portB = pcapMessage[PackageFields.TCP_DST_PORT];
        long len = Long.parseLong(pcapMessage[PackageFields.FRAME_CAP_LEN]);
        String time = pcapMessage[PackageFields.FRAME_TIME];

        if (isIPv4) {
            addressA = pcapMessage[PackageFields.IP_SRC];
            addressB = pcapMessage[PackageFields.IP_DST];
        }
        if (isIPv6){
            addressA = pcapMessage[PackageFields.IPV6_SRC];
            addressB = pcapMessage[PackageFields.IPV6_DST];
        }

        SphinxModel sphinxModel = SphinxModelHelper.getSphinxModel("tshark");

        AlertMetadataModel alertMetadataModel = new AlertMetadataModel();
        alertMetadataModel.setSignatureSeverity(new String[]{"Minor"});

        AlertModel alertModel = new AlertModel();
        alertModel.setAction("allowed");
        alertModel.setSignature("Tcp Analysis Flags - " + pcapMessage[PackageFields.WS_COL_INFO]);
        alertModel.setMetadata(alertMetadataModel);
        alertModel.setCategory("Tcp Analysis Flags");

        TcpAnalysisFlagsAlertModel tcpAnalysisFlagsAlert = new TcpAnalysisFlagsAlertModel();
        tcpAnalysisFlagsAlert.setProtocol(protocol);
        tcpAnalysisFlagsAlert.setBytes(len);
        tcpAnalysisFlagsAlert.setSrcIp(addressA);
        tcpAnalysisFlagsAlert.setDestIp(addressB);
        tcpAnalysisFlagsAlert.setSrcPort(portA);
        tcpAnalysisFlagsAlert.setDestPort(portB);
        tcpAnalysisFlagsAlert.setTimestamp(FormatHelper.toLocalDateTime(time));
        tcpAnalysisFlagsAlert.setInfo( pcapMessage[PackageFields.WS_COL_INFO]);
        tcpAnalysisFlagsAlert.setHost(pcapMessage[PackageFields.HOSTNAME]);
        tcpAnalysisFlagsAlert.setSphinxModel(sphinxModel);
        tcpAnalysisFlagsAlert.setAlert(alertModel);

        tcpAnalysisFlagsAlert.setEventType("TcpAnalysisFlagsAlertModel"); // TcpAnalysisFlagsAlertModel

        messagingSystemService.sendAlert(tcpAnalysisFlagsAlert);

    }

    @Override
    public void detect(EventModel eventModel) {

    }

    private boolean isProtocol(String[] protocols, String protocol){
        for(String p:protocols){
            if (p.equals(protocol)){
                return true;
            }
        }
        return false;
    }

}
