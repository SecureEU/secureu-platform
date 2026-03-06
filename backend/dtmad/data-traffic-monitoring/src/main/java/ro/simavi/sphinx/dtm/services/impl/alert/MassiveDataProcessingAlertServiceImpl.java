package ro.simavi.sphinx.dtm.services.impl.alert;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import ro.simavi.sphinx.dtm.model.ConversationsModel;
import ro.simavi.sphinx.dtm.model.enums.ConfigCode;
import ro.simavi.sphinx.dtm.services.alert.MassiveDataProcessingAlertService;
import ro.simavi.sphinx.dtm.services.impl.DtmConfigServiceImpl;
import ro.simavi.sphinx.model.ConfigModel;
import ro.simavi.sphinx.model.event.EventModel;
import ro.simavi.sphinx.util.PackageFields;

import java.util.HashMap;

@Service
public class MassiveDataProcessingAlertServiceImpl implements MassiveDataProcessingAlertService {

     /*
        eth@a1@a2
        ip@a1@a2
        ipv6@a1@a2
        tcp@a1@a2@p1@p2@streamindex
        udp@a1@a2@p1@p2@streamindex
     */


    private final DtmConfigServiceImpl configService;

    private static long THRESHOLD_ALERT_DEFAULT_VALUE = 25000000; //default value

    private HashMap<String,ConversationsModel> conversationsMap = new HashMap<>();

    public MassiveDataProcessingAlertServiceImpl(DtmConfigServiceImpl configService){
        this.configService = configService;
    }

    private Long getValueFromDb(){

        ConfigModel mdpThresholdConfigModel = configService.getValue(ConfigCode.MASSIVE_DATA_PROCESSING_ALERT_THRESHOLD);
        if (mdpThresholdConfigModel.getValue()!=null){
            String value = mdpThresholdConfigModel.getValue();
            if (value!=null){
                return Long.parseLong(value)*1000000;
            }
        }
        return null;
    }

    public long getThresholdAlertValue(){

        Long thresholdAlertValueCached = getValueFromDb();

        if (thresholdAlertValueCached==null) {
            return THRESHOLD_ALERT_DEFAULT_VALUE;
        }

        return thresholdAlertValueCached;
    }

    @Override
    public void detect(String[] pcapMessage) {
        String protocolsString = pcapMessage[PackageFields.FRAME_PROTOCOLS];
        if (StringUtils.isEmpty(protocolsString)){
            return;
        }

        String[] protocols = protocolsString.split(":");
        boolean isIPv4 = isProtocol(protocols,"ip");
        boolean isIPv6= isProtocol(protocols,"ipv6");

        String protocol = null;
        String addressA = null;
        String addressB = null;
        String portA = null;
        String portB = null;
        String streamIndex = null;
        long len = Long.parseLong(pcapMessage[PackageFields.FRAME_CAP_LEN]);
        String time = pcapMessage[PackageFields.FRAME_TIME];

        if (isProtocol(protocols,"eth")){
            protocol = "eth";
            addressA = pcapMessage[PackageFields.ETH_SRC];
            addressB = pcapMessage[PackageFields.ETH_DST];
            String address = protocol+"@"+addressA+"@"+addressB;
            String addressInv = protocol+"@"+addressB+"@"+addressA;

            updateConversation(address, addressInv, len, time, pcapMessage);

        } else if (isIPv4){
            protocol = "ip";
            addressA = pcapMessage[PackageFields.IP_SRC];
            addressB = pcapMessage[PackageFields.IP_DST];
            String address = protocol+"@"+addressA+"@"+addressB;
            String addressInv = protocol+"@"+addressB+"@"+addressA;

            updateConversation(address, addressInv, len, time, pcapMessage);
        } else if (isIPv6){
            protocol = "ipv6";
            addressA = pcapMessage[PackageFields.IPV6_SRC];
            addressB = pcapMessage[PackageFields.IPV6_DST];
            String address = protocol+"@"+addressA+"@"+addressB;
            String addressInv = protocol+"@"+addressB+"@"+addressA;

            updateConversation(address, addressInv, len, time, pcapMessage);
        } else if (isProtocol(protocols,"tcp")){
            protocol = "tcp";
            if (isIPv4) {
                addressA = pcapMessage[PackageFields.IP_SRC];
                addressB = pcapMessage[PackageFields.IP_DST];
            }
            if (isIPv6){
                addressA = pcapMessage[PackageFields.IPV6_SRC];
                addressB = pcapMessage[PackageFields.IPV6_DST];
            }
            portA = pcapMessage[PackageFields.TCP_SRC_PORT];
            portB = pcapMessage[PackageFields.TCP_DST_PORT];
            streamIndex = pcapMessage[PackageFields.TCP_STREAM];
            String address = protocol+"@"+addressA+"@"+addressB+"@"+portA+"@"+portB+"@"+streamIndex;

            String addressInv =protocol+"@"+addressB+"@"+addressA+"@"+portB+"@"+portA+"@"+streamIndex;

            updateConversation(address, addressInv, len, time, pcapMessage);
        } else if (isProtocol(protocols,"udp")){
            protocol = "udp";
            if (isIPv4) {
                addressA = pcapMessage[PackageFields.IP_SRC];
                addressB = pcapMessage[PackageFields.IP_DST];
            }
            if (isIPv6){
                addressA = pcapMessage[PackageFields.IPV6_SRC];
                addressB = pcapMessage[PackageFields.IPV6_DST];
            }
            portA = pcapMessage[PackageFields.UDP_SCR_PORT];
            portB = pcapMessage[PackageFields.UDP_DST_PORT];
            streamIndex = pcapMessage[PackageFields.TCP_STREAM];
            String address = protocol+"@"+addressA+"@"+addressB+"@"+portA+"@"+portB+"@"+streamIndex;

            String addressInv =protocol+"@"+addressB+"@"+addressA+"@"+portB+"@"+portA+"@"+streamIndex;

            updateConversation(address, addressInv, len, time, pcapMessage);
        }
    }

    @Override
    public void detect(EventModel eventModel) {

    }

    private void updateConversation(String address, String addressInv, long len, String time, String [] pcapMessage){
        ConversationsModel conversationsModel = conversationsMap.get(address);
        if (conversationsModel==null){
            conversationsModel = conversationsMap.get(addressInv);
            if (conversationsModel!=null){
                long totalLen = conversationsModel.getBytes()+len;
                conversationsModel.setBytes(totalLen);
                conversationsModel.setPackages(conversationsModel.getPackages()+1);
                conversationsModel.setBytesBA(conversationsModel.getPackageBA()+len);
                conversationsModel.setPackageBA(conversationsModel.getPackageBA()+1);
                conversationsModel.setTime(time);
            }
        }else{
            long totalLen = conversationsModel.getBytes()+len;
            conversationsModel.setBytes(totalLen);
            conversationsModel.setPackages(conversationsModel.getPackages()+1);
            conversationsModel.setBytesAB(conversationsModel.getPackageAB()+len);
            conversationsModel.setPackageAB(conversationsModel.getPackageAB()+1);
            conversationsModel.setTime(time);
        }

        if (conversationsModel == null) {
            conversationsModel = new ConversationsModel();
            conversationsModel.setBytes(len);
            conversationsModel.setPackages(1);
            conversationsModel.setBytesAB(len);
            conversationsModel.setPackageAB(1);
            conversationsModel.setTime(time);
            conversationsModel.setHost(pcapMessage[PackageFields.HOSTNAME]);
            conversationsMap.put(address,conversationsModel);
        }
    }

    public HashMap<String,ConversationsModel> getConversations(){
        return this.conversationsMap;
    }

    public void clearConversations(){
        this.conversationsMap.clear();
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
