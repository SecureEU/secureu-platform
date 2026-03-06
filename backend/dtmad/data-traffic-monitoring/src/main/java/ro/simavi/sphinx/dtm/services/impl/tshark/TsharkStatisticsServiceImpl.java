package ro.simavi.sphinx.dtm.services.impl.tshark;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import ro.simavi.sphinx.dtm.model.event.MetricModel;
import ro.simavi.sphinx.dtm.model.PackageStatistics;
import ro.simavi.sphinx.model.event.alert.SphinxModel;
import ro.simavi.sphinx.dtm.services.tshark.TsharkStatisticsService;
import ro.simavi.sphinx.util.PackageFields;

import java.util.Arrays;
import java.util.HashMap;

@Service
public class TsharkStatisticsServiceImpl implements TsharkStatisticsService {

    private boolean ignorePort = true;
    private static final Logger logger = LoggerFactory.getLogger(TsharkStatisticsServiceImpl.class);

    private HashMap<String, HashMap<String, PackageStatistics>> ethMap = new HashMap<>();
    private HashMap<String, HashMap<String, PackageStatistics>> ipv4Map = new HashMap<>();
    private HashMap<String, HashMap<String, PackageStatistics>> ipv6Map = new HashMap<>();
    private HashMap<String, HashMap<String, PackageStatistics>> tcpMap = new HashMap<>();
    private HashMap<String, HashMap<String, PackageStatistics>> udpMap = new HashMap<>();

    private HashMap<String,Long> usernameTraffic = new HashMap<>();

    // protocol
    private HashMap<String,PackageStatistics> protocolMap = new HashMap<>();

    private HashMap<String, HashMap<String, PackageStatistics>> protocolMapByInstance = new HashMap<>();


    @Override
    public void collect(String[] pcapMessage) {
        collect(pcapMessage, null, ethMap);
        collect(pcapMessage, "IPv4", ipv4Map); // IPv4
        collect(pcapMessage, "IPv6", ipv6Map); // IPv6
        collect(pcapMessage,"6",tcpMap); // TCP
        collect(pcapMessage,"17",udpMap); // UDP
        collect(pcapMessage, protocolMap);

        collectUsernameStatistics(pcapMessage, usernameTraffic);

    }

    private void collect(String[] pcapMessage, HashMap<String, PackageStatistics> protocolMap) {
        if (pcapMessage.length<=PackageFields.FRAME_PROTOCOLS){
            logger.info("collect /FRAME_PROTOCOLS [8]="+Arrays.toString(pcapMessage));
            return;
        }
        String protocolsString = pcapMessage[PackageFields.FRAME_PROTOCOLS];
        if (StringUtils.isEmpty(protocolsString)){
            return;
        }

        long len = Long.parseLong(pcapMessage[PackageFields.FRAME_CAP_LEN]);

        String[] protocols = protocolsString.split(":");
        update(protocols,"eth", len, protocolMap);
        update(protocols,"ip", len, protocolMap);
        update(protocols,"ipv6", len, protocolMap);
        update(protocols,"udp", len, protocolMap);
        update(protocols,"tcp", len, protocolMap);
    }

    private void update( String[] protocols, String protocol, long len, HashMap<String, PackageStatistics> protocolMap){
        boolean isProtocol = isProtocol(protocols,protocol);
        if (isProtocol){
            PackageStatistics packageStatistics = protocolMap.get(protocol);
            if (packageStatistics==null){
                packageStatistics = new PackageStatistics();
                packageStatistics.setPackages(1L);
                packageStatistics.setBytes(len);
                protocolMap.put(protocol, packageStatistics);
            }else{
                packageStatistics.setPackages(packageStatistics.getPackages()+1);
                packageStatistics.setBytes(packageStatistics.getBytes()+len);
            }
        }
    }


    private void collectUsernameStatistics(String[] pcapMessage, HashMap<String,Long> usernameTraffic) {
        Long len = null;
        if (pcapMessage.length>PackageFields.FRAME_CAP_LEN) {
            len = Long.parseLong(pcapMessage[PackageFields.FRAME_CAP_LEN]);
        }else{
            logger.info("collectUsernameStatistics /FRAME_CAP_LEN [6]="+Arrays.toString(pcapMessage));
            return;
        }

        if (pcapMessage.length>PackageFields.HOSTNAME) {
            String username = pcapMessage[PackageFields.USERNAME];
            String hostname = pcapMessage[PackageFields.HOSTNAME];

            String uh = username + " @ " + hostname;

            Long bytes = usernameTraffic.get(uh);
            if (bytes == null) {
                usernameTraffic.put(uh, len);
            } else {
                usernameTraffic.put(uh, len + bytes);
            }

        }else {
            logger.info("collectUsernameStatistics /HOSTNAME [32]="+Arrays.toString(pcapMessage));
        }
    }

    private boolean isProtocol(String[] protocols, String protocol){
        for(String p:protocols){
            if (p.equals(protocol)){
                return true;
            }
        }
        return false;
    }


    private String getInterface(String[] pcapMessage){

        String interfaceName = "-";
        if (pcapMessage.length>PackageFields.FRAME_INTERFACE_DESCRIPTION) {
            interfaceName = pcapMessage[PackageFields.FRAME_INTERFACE_DESCRIPTION]; // 5
        }else{
            logger.warn("getInterface / pcapMessage [FRAME_INTERFACE_DESCRIPTION/5]= " + Arrays.toString(pcapMessage));
        }

        if (StringUtils.isEmpty(interfaceName) || "-".equals(interfaceName)){
            if (pcapMessage.length>PackageFields.FRAME_INTERFACE_NAME) {
                interfaceName = pcapMessage[PackageFields.FRAME_INTERFACE_NAME]; //3
            }else{
                logger.warn("getInterface / pcapMessage [FRAME_INTERFACE_NAME/3]= " + Arrays.toString(pcapMessage));
            }
        }
        if (StringUtils.isEmpty(interfaceName)){
            interfaceName = "-";
        }
        return interfaceName;
    }

    private String getSourceName(String[] pcapMessage, String protocolId, String protocol){
        String sourceName = null;
        if (protocolId==null) {
            if (pcapMessage.length>PackageFields.ETH_SRC) {
                sourceName = pcapMessage[PackageFields.ETH_SRC]; //eth.src
            }else{
                logger.warn("getInterface / pcapMessage [ETH_SRC/9]= " + Arrays.toString(pcapMessage));
            }
        }else{
            if ("IPv6".equals(protocol)){
                if (pcapMessage.length>PackageFields.TCP_PORT) { // 17
                    sourceName = pcapMessage[PackageFields.IPV6_SRC]; //ip.src
                }
            }else {
                if (pcapMessage.length>PackageFields.IP_SRC){
                    sourceName = pcapMessage[PackageFields.IP_SRC]; //ip.src
                }
                if (!ignorePort) {
                    if (pcapMessage.length > PackageFields.TCP_PORT) { //17
                        if ("TCP".equals(protocol)) {
                            sourceName = sourceName + ":" + pcapMessage[PackageFields.TCP_PORT]; //17
                        } else if ("UDP".equals(protocol)) {
                            sourceName = sourceName + ":" + pcapMessage[PackageFields.UDP_PORT]; //18
                        }
                    }
                }
            }
        }
        if (StringUtils.isEmpty(sourceName)){
            sourceName = "-";
        }
        return sourceName;
    }

    private String getDestName(String[] pcapMessage, String protocolId, String protocol){
        String destName = null;
        if (protocolId==null) {
            if (pcapMessage.length>PackageFields.ETH_DST) {
                destName = pcapMessage[PackageFields.ETH_DST]; //eth.dest
            }else{
                logger.warn("getDestName / pcapMessage [ETH_DST/10]= " + Arrays.toString(pcapMessage));
            }
        }else{
            if ("IPv6".equals(protocol)){
                if (pcapMessage.length>PackageFields.TCP_PORT) { // 17
                    destName = pcapMessage[PackageFields.IPV6_DST]; //ip.dest // 22
                }
            }else {
                if (pcapMessage.length>PackageFields.IP_DST) {
                    destName = pcapMessage[PackageFields.IP_DST]; //ip.dest // 12
                }
                if (!ignorePort) {
                    if (pcapMessage.length > PackageFields.TCP_PORT) { //17
                        if ("TCP".equals(protocol)) {
                            destName = destName + ":" + pcapMessage[PackageFields.TCP_PORT];
                        } else if ("UDP".equals(protocol)) {
                            destName = destName + ":" + pcapMessage[PackageFields.UDP_PORT];
                        }
                    }
                }
            }
        }
        if (StringUtils.isEmpty(destName)){
            destName = "-";
        }
        return destName;
    }

    private void collect(String[] pcapMessage,String protocolId,  HashMap<String, HashMap<String, PackageStatistics>> map) {
        String protocol="ETH";
        if (protocolId!=null) {
            if ("IPv4".equals(protocolId)){
                String proto = null;
                if (pcapMessage.length>PackageFields.IP) {
                    proto = pcapMessage[PackageFields.IP]; // 13
                }
                if (StringUtils.isEmpty(proto)) {
                    return;
                }
                if (!proto.contains("Internet Protocol Version 4")){
                    return;
                }
                protocol = "IPv4";
            }else if ("IPv6".equals(protocolId)){
                if (pcapMessage.length>PackageFields.TCP_PORT) { // 17
                    String address = pcapMessage[PackageFields.IPV6_ADDR]; // 20
                    if (StringUtils.isEmpty(address)) {
                        return;
                    }
                    protocol = "IPv6";
                }
            }else{
                if (protocolId.equals("6")) {
                    protocol = "TCP";
                }
                if (protocolId.equals("17")) {
                }
                String proto =null;
                if (pcapMessage.length>PackageFields.IP_PROTO) { // 14
                    proto = pcapMessage[PackageFields.IP_PROTO];
                }
                if (StringUtils.isEmpty(proto)) {
                    return;
                }
                if (!protocolId.equals(proto)) {
                    return;
                }
            }
        }

        String interfaceName = getInterface(pcapMessage);

        HashMap<String, PackageStatistics> sourceMap = map.get(interfaceName);
        if (sourceMap==null){
            sourceMap = new HashMap<>();
            map.put(interfaceName,sourceMap);
        }

        addStatistics(pcapMessage, protocolId, getSourceName(pcapMessage, protocolId, protocol), map, sourceMap);
        addStatistics(pcapMessage, protocolId, getDestName(pcapMessage, protocolId, protocol), map, sourceMap);
    }

    private void addStatistics(String[] pcapMessage, String protocolId, String sourceName, HashMap<String, HashMap<String, PackageStatistics>> map, HashMap<String, PackageStatistics> sourceMap){

        if (pcapMessage.length>PackageFields.FRAME_CAP_LEN){
            long len = Long.parseLong(pcapMessage[PackageFields.FRAME_CAP_LEN]);
            PackageStatistics packageStatistics = sourceMap.get(sourceName);
            if (packageStatistics==null){
                packageStatistics = new PackageStatistics();
                packageStatistics.setBytes(len);
                packageStatistics.setPackages(1);
                sourceMap.put(sourceName,packageStatistics);
            }else{
                packageStatistics.setBytes(packageStatistics.getBytes()+len);
                packageStatistics.setPackages(packageStatistics.getPackages()+1);
            }
        }else{
            logger.warn("addStatistics / pcapMessage [FRAME_CAP_LEN/6]= " + Arrays.toString(pcapMessage));
        }

    }

    @Override
    public HashMap<String, HashMap<String, PackageStatistics>> getPackageEthernetStatistics() {
        return ethMap;
    }

    @Override
    public HashMap<String, HashMap<String, PackageStatistics>> getPackageIPv4Statistics() {
        return ipv4Map;
    }

    @Override
    public HashMap<String, HashMap<String, PackageStatistics>> getPackageUDPStatistics() {
        return udpMap;
    }

    @Override
    public HashMap<String, HashMap<String, PackageStatistics>> getPackageTCPStatistics() {
        return tcpMap;
    }

    @Override
    public HashMap<String, HashMap<String, PackageStatistics>> getPackageIPv6Statistics() {
        return ipv6Map;
    }

    @Override
    public HashMap<String, PackageStatistics> getProtocolStatistics() {
        return protocolMap;
    }

    @Override
    public HashMap<String, Long> getUsernameStatistics() {
        return usernameTraffic;
    }

    @Override
    public HashMap<String, HashMap<String, PackageStatistics>> getProtocolByInstanceStatistics() {
        return protocolMapByInstance;
    }


    @Override
    public void collect(MetricModel metricModel) {
        SphinxModel sphinx = metricModel.getSphinxModel();
        String tool = sphinx.getTool();
        String component = sphinx.getComponent();

        if (tool.equals("tshark") && component.equals("dtm")) {

            HashMap map = (HashMap) metricModel.getStats().get("traffic");
            String hostname = metricModel.getHost();
            String username = metricModel.getUsername();
            String instance = hostname + "@" + username;
            protocolMapByInstance.put(instance, map);

        }
    }

}
