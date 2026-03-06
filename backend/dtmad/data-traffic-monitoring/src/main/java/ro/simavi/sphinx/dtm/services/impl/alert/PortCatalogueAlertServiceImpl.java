package ro.simavi.sphinx.dtm.services.impl.alert;

import org.apache.commons.collections.IteratorUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import ro.simavi.sphinx.dtm.entities.PortCatalogueEntity;
import ro.simavi.sphinx.dtm.jpa.repositories.PortCatalogueRepository;
import ro.simavi.sphinx.dtm.model.PortCatalogueModel;
import ro.simavi.sphinx.model.event.alert.PortDiscoveryAlertModel;
import ro.simavi.sphinx.model.event.alert.SphinxModel;
import ro.simavi.sphinx.dtm.services.MessagingSystemService;
import ro.simavi.sphinx.dtm.services.alert.PortCatalogueAlertService;
import ro.simavi.sphinx.dtm.util.NetworkHelper;
import ro.simavi.sphinx.model.event.AlertMetadataModel;
import ro.simavi.sphinx.model.event.AlertModel;
import ro.simavi.sphinx.model.event.EventModel;
import ro.simavi.sphinx.util.PackageFields;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

@Service
public class PortCatalogueAlertServiceImpl implements PortCatalogueAlertService {

    private final PortCatalogueRepository portCatalogueRepository;

    private final MessagingSystemService messagingSystemService;

    private List<PortCatalogueModel> portCatalogueAlerts = new ArrayList<>();

    private List<PortCatalogueModel> portCatalogueEntitiesCached = null;

    public PortCatalogueAlertServiceImpl(PortCatalogueRepository portCatalogueRepository,
                                         MessagingSystemService messagingSystemService){
        this.portCatalogueRepository = portCatalogueRepository;
        this.messagingSystemService = messagingSystemService;
    }

    @Override
    public List<PortCatalogueModel> getPortCatalogueList() {
        return getPortCatalogueListCached();
    }

    private List<PortCatalogueModel> getPortCatalogueListCached(){
        if (portCatalogueEntitiesCached==null){
            List<PortCatalogueEntity> portCatalogueEntities = IteratorUtils.toList(portCatalogueRepository.findAll().iterator());

            portCatalogueEntitiesCached = new ArrayList<>();
            for(PortCatalogueEntity portCatalogueEntity: portCatalogueEntities){
                portCatalogueEntitiesCached.add(toPortCatalogueModel(portCatalogueEntity));
            }
        }

        return portCatalogueEntitiesCached;
    }

    private PortCatalogueModel toPortCatalogueModel(PortCatalogueEntity portCatalogueEntity){
        PortCatalogueModel portCatalogueModel = new PortCatalogueModel();
        portCatalogueModel.setId(portCatalogueEntity.getId());
        portCatalogueModel.setDescription(portCatalogueEntity.getDescription());
        portCatalogueModel.setName(portCatalogueEntity.getName());
        portCatalogueModel.setPort(portCatalogueEntity.getPort());
        portCatalogueModel.setEndPortInterval(portCatalogueEntity.getEndPortInterval());
        return portCatalogueModel;
    }

    @Override
    public PortCatalogueModel findById(Long id) {
        Optional<PortCatalogueEntity> portCatalogueEntityOptional = portCatalogueRepository.findById(id);
        if (portCatalogueEntityOptional.isPresent()){
            PortCatalogueEntity portCatalogueEntity = portCatalogueEntityOptional.get();
            return toPortCatalogueModel(portCatalogueEntity);
        }
        return null;
    }

    @Override
    public void save(PortCatalogueModel portCatalogueModel) {
        if (portCatalogueModel.getId()!=null){
            Optional<PortCatalogueEntity> portCatalogueEntityOptional = portCatalogueRepository.findById(portCatalogueModel.getId());
            if (portCatalogueEntityOptional.isPresent()){
                PortCatalogueEntity portCatalogueEntityOld = portCatalogueEntityOptional.get();
                portCatalogueEntityOld.setPort(portCatalogueModel.getPort());
                portCatalogueEntityOld.setEndPortInterval(portCatalogueModel.getEndPortInterval());
                portCatalogueEntityOld.setName(portCatalogueModel.getName());
                portCatalogueEntityOld.setDescription(portCatalogueModel.getDescription());
                this.portCatalogueRepository.save(portCatalogueEntityOld);
            }
        }else{
            this.portCatalogueRepository.save(toPortCatalogueEntity(portCatalogueModel));
        }

        this.clearCache();
        this.refreshAlerts();
    }

    private PortCatalogueEntity toPortCatalogueEntity(PortCatalogueModel portCatalogueModel) {
        PortCatalogueEntity portCatalogueEntity = new PortCatalogueEntity();
        portCatalogueEntity.setName(portCatalogueModel.getName());
        portCatalogueEntity.setDescription(portCatalogueModel.getDescription());
        portCatalogueEntity.setPort(portCatalogueModel.getPort());
        portCatalogueEntity.setEndPortInterval(portCatalogueModel.getEndPortInterval());
        return portCatalogueEntity;
    }

    @Override
    public void delete(Long id) {
        this.portCatalogueRepository.deleteById(id);

        this.clearCache();
        this.refreshAlerts();
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

        String port = pcapMessage[PackageFields.TCP_PORT]; //tcp port
        String host = pcapMessage[PackageFields.HOSTNAME];

        String addressA = null;
        String addressB = null;
        String portA = null;
        String portB = null;

        String protocol = null ;
        if (isProtocol(protocols,"tcp")){
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

        }

        detect(port, host, protocol, addressA, addressB, portB);
    }

    private boolean isProtocol(String[] protocols, String protocol){
        for(String p:protocols){
            if (p.equals(protocol)){
                return true;
            }
        }
        return false;
    }

    @Override
    public void detect(EventModel eventModel) {
        String port = eventModel.getSrcPort();
        detect(port, eventModel.getHost(), eventModel.getProtocol(), eventModel.getSrcIp(), eventModel.getDestIp(), eventModel.getDestPort());
    }

    private void detect(String port, String host, String protocol, String srcIp, String destIp, String destPort){
        if (!StringUtils.isEmpty(port)) {
            if (!exists(Long.parseLong(port))) {
                PortCatalogueModel portCatalogueModel = new PortCatalogueModel();
                portCatalogueModel.setPort(Long.parseLong(port));
                portCatalogueModel.setTimestamp(LocalDateTime.now());

                portCatalogueAlerts.add(portCatalogueModel);
                generateAlert(port, host, protocol, srcIp, destIp, destPort);
            }
        }
    }

    private void generateAlert(String port, String host, String protocol, String srcIp, String destIp, String destPort){

        SphinxModel sphinxModel = new SphinxModel();
        sphinxModel.setComponent("dtm");
        sphinxModel.setTool("tshark");
        sphinxModel.setHostname(NetworkHelper.getHostName());
        sphinxModel.setUsername(NetworkHelper.getUsername());

        AlertMetadataModel alertMetadataModel = new AlertMetadataModel();
        alertMetadataModel.setSignatureSeverity(new String[]{"Minor"});

        AlertModel alertModel = new AlertModel();
        alertModel.setAction("allowed");
        alertModel.setSignature("Port Discovery - " + port);
        alertModel.setMetadata(alertMetadataModel);
        alertModel.setCategory("Port Discovery");

        PortDiscoveryAlertModel  portDiscoveryAlertModel= new PortDiscoveryAlertModel();
        portDiscoveryAlertModel.setProtocol(protocol);
        portDiscoveryAlertModel.setSrcIp(srcIp);
        portDiscoveryAlertModel.setDestIp(destIp);

        portDiscoveryAlertModel.setSrcPort(port);
        portDiscoveryAlertModel.setDestPort(destPort);

        portDiscoveryAlertModel.setTimestamp(LocalDateTime.now());
        portDiscoveryAlertModel.setHost(host);

        portDiscoveryAlertModel.setSphinxModel(sphinxModel);
        portDiscoveryAlertModel.setAlert(alertModel);
        portDiscoveryAlertModel.setEventType("PortDiscoveryAlertModel"); // PortDiscoveryAlertModel

        messagingSystemService.sendAlert(portDiscoveryAlertModel);

    }

    private boolean exists(Long port){

        Iterator<PortCatalogueModel> iterator = portCatalogueAlerts.iterator();
        while(iterator.hasNext()){
            PortCatalogueModel portCatalogueModel = iterator.next();
            if (port.intValue()==portCatalogueModel.getPort().intValue()){
                return true;
            }
        }

        for(PortCatalogueModel portCatalogueModel: getPortCatalogueListCached()){
            if (portCatalogueModel.getEndPortInterval()==null){
                if (port.intValue()==portCatalogueModel.getPort().intValue()){
                    return true;
                }
            }else{
                if (port.intValue()>=portCatalogueModel.getPort().intValue() && port.intValue()<=portCatalogueModel.getEndPortInterval().intValue() ){
                    return true;
                }
            }
        }
        return false;
    }

    private void refreshAlerts(){
        Iterator<PortCatalogueModel> iterator = portCatalogueAlerts.iterator();
        while(iterator.hasNext()){
            PortCatalogueModel portCatalogueAlert = iterator.next();
            for(PortCatalogueModel portCatalogueModel: getPortCatalogueListCached()){
                if (portCatalogueModel.getEndPortInterval()==null){
                    if (portCatalogueAlert.getPort().intValue()==portCatalogueModel.getPort().intValue()){
                        iterator.remove();
                    }
                }else{
                    if (portCatalogueAlert.getPort().intValue()>=portCatalogueModel.getPort().intValue() && portCatalogueAlert.getPort().intValue()<=portCatalogueModel.getEndPortInterval().intValue() ){
                        iterator.remove();
                    }
                }

            }
        }
    }

    @Override
    public List<PortCatalogueModel> getAlerts() {
        return this.portCatalogueAlerts;
    }

    @Override
    public void clearCache() {
        this.portCatalogueEntitiesCached = null;
    }
}
