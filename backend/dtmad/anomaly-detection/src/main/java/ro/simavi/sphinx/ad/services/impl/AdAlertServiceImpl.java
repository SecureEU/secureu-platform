package ro.simavi.sphinx.ad.services.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import ro.simavi.sphinx.ad.entities.*;
import ro.simavi.sphinx.ad.jpa.repositories.AdAlertRepository;
import ro.simavi.sphinx.ad.services.AdAlertService;
import ro.simavi.sphinx.model.event.alertStix.StixModelAlert;
import ro.simavi.sphinx.ad.entities.AlertEntity;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Locale;


@Service
public class AdAlertServiceImpl implements AdAlertService {


    private final AdAlertRepository adAlertRepository;

    public AdAlertServiceImpl(AdAlertRepository adAlertRepository) {
        this.adAlertRepository = adAlertRepository;
    }

    @Override
    public void collect(StixModelAlert stixModelAlert) {
        this.persist(stixModelAlert);

    }

    private void persist(StixModelAlert stixModelAlert){
        this.adAlertRepository.save(toAlert(stixModelAlert));
    }

    private AlertEntity toAlert(StixModelAlert stixModelAlert) {

        AlertEntity alertEntity = new AlertEntity();
        String time = stixModelAlert.getObjects().get(0).getCreated();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withLocale(Locale.ENGLISH);
        LocalDateTime dateTime = LocalDateTime.parse(time, formatter);

        alertEntity.setTimestamp(dateTime);

        alertEntity.setSphinxTool("AD");
        alertEntity.setSourceref(stixModelAlert.getObjects().get(2).getSourceRef());
        alertEntity.setRelationshipType(stixModelAlert.getObjects().get(2).getRelationshipType());
        alertEntity.setTargetRef(stixModelAlert.getObjects().get(2).getTargetRef());
        alertEntity.setSourceref(stixModelAlert.getObjects().get(2).getSourceRef());
        int totalFlow= stixModelAlert.getObjects().get(1).getDetails().getTotalFlow();
        int bytes = stixModelAlert.getObjects().get(1).getDetails().getProtocolFlowModel().getBytes();
        String flowId = stixModelAlert.getObjects().get(1).getDetails().getFlowId();
        String username= stixModelAlert.getObjects().get(1).getDetails().getUsername();
        String title = stixModelAlert.getObjects().get(1).getDetails().getTitle();
        String detailsFlow = "title: " + title+ "totalFlow: "+ String.valueOf(totalFlow )+ "," + "bytes: "+ String.valueOf(bytes)+ ","
                + "flow id: "+ flowId +","+ "username: "+ username;
        alertEntity.setDetails(detailsFlow);
        alertEntity.setEventType(stixModelAlert.getObjects().get(1).getDetails().getAlgorithm().getType());

        String alienIP = this.isNull(stixModelAlert.getObjects().get(1).getDetails().getAlgorithm().getAlienIP());
        String numberOfPairs =this.isNull(stixModelAlert.getObjects().get(1).getDetails().getAlgorithm().getNumberOfPairs());
        String bytesUp = this.isNull(stixModelAlert.getObjects().get(1).getDetails().getAlgorithm().getBytesUp());
        String bytesDown = this.isNull(stixModelAlert.getObjects().get(1).getDetails().getAlgorithm().getBytesDown());
        String numberPkts = this.isNull(stixModelAlert.getObjects().get(1).getDetails().getAlgorithm().getNumberPkts());
        String ports = this.isNull(stixModelAlert.getObjects().get(1).getDetails().getAlgorithm().getPorts());
        String myIP = this.isNull(stixModelAlert.getObjects().get(1).getDetails().getAlgorithm().getMyIP());
        String hostname = this.isNull(stixModelAlert.getObjects().get(1).getDetails().getAlgorithm().getHostname());
        String connections = this.isNull(stixModelAlert.getObjects().get(1).getDetails().getAlgorithm().getConnections());
        String tcpPort = this.isNull(stixModelAlert.getObjects().get(1).getDetails().getAlgorithm().getTcpport());
        String dataMean = this.isNull(stixModelAlert.getObjects().get(1).getDetails().getAlgorithm().getDataMean());
        String dataStdev = this.isNull(stixModelAlert.getObjects().get(1).getDetails().getAlgorithm().getDataStdev());
        String pairsMean = this.isNull(stixModelAlert.getObjects().get(1).getDetails().getAlgorithm().getPairsMean());
        String pairsStdev = this.isNull(stixModelAlert.getObjects().get(1).getDetails().getAlgorithm().getPairsStdev());
        String aliens = this.isNull(stixModelAlert.getObjects().get(1).getDetails().getAlgorithm().getAliens());
        String numberOfFlows = this.isNull(stixModelAlert.getObjects().get(1).getDetails().getAlgorithm().getNumberOfFlows());
        String numberOfFlowsAlienPort = this.isNull(stixModelAlert.getObjects().get(1).getDetails().getAlgorithm().getNumberOfFlowsAlienPort());
        String flowsMean = this.isNull(stixModelAlert.getObjects().get(1).getDetails().getAlgorithm().getFlowsMean());
        String flowsStdev = this.isNull(stixModelAlert.getObjects().get(1).getDetails().getAlgorithm().getFlowsStdev());
        String numberOfFlowsPerPort = this.isNull(stixModelAlert.getObjects().get(1).getDetails().getAlgorithm().getNumberOfFlowsPerPort());
        String portsMean = this.isNull(stixModelAlert.getObjects().get(1).getDetails().getAlgorithm().getPortsMean());
        String portsStdev = this.isNull(stixModelAlert.getObjects().get(1).getDetails().getAlgorithm().getPortsStdev());
        String numberOfClusters = this.isNull(String.valueOf(stixModelAlert.getObjects().get(1).getDetails().getAlgorithm().getNumberOfClusters()));
        String minDirtyProportion = this.isNull(String.valueOf(stixModelAlert.getObjects().get(1).getDetails().getAlgorithm().getMinDirtyProportion()));
        String maxAnomalousCluster = this.isNull(String.valueOf(stixModelAlert.getObjects().get(1).getDetails().getAlgorithm().getMaxAnomalousClusterProportion()));

        String type = stixModelAlert.getObjects().get(1).getDetails().getAlgorithm().getType();
        String detailsKmeans =this.isNull( "Number Of Clusters: " + numberOfClusters + ", " + "Min Dirty Proportion: " + minDirtyProportion
                + "," + "Max Anomalous Cluster: " + maxAnomalousCluster);
        String detailsSflow = "Bytes Up: " + bytesUp + "," + "bytesDown: " + bytesDown + ", "+ "numberPkts: " + numberPkts;
        String details = "Connections: " +connections + ", " + "Hostname: " + hostname;
        String detailsPort = "MyIP: " + myIP  + ", " + "NumberOfFlows: " + numberOfFlows;

        switch(type)
        {
            case "dns_kmeans_clustering":
                alertEntity.setDetails(detailsFlow + ", " + detailsKmeans);
                break;
            case "http_kmeans_clustering":
                alertEntity.setDetails(detailsFlow + ", " +detailsKmeans);
                break;
            case "AbusedSMTP_sflow":
                alertEntity.setDetails(detailsFlow + ", " + detailsSflow  + ", " + details );
                break;
            case "AlienAccessingManyHosts_sflow":
                alertEntity.setDetails(detailsFlow + ", " + detailsSflow + ", " + "AlienIP: " +alienIP + ", " + "Ports: " + ports + ", " + "NumberOfPairs: " + numberOfPairs);
                break;
            case "AtypicalAlienTCPPortUsed_sflow":
                alertEntity.setDetails(detailsFlow + ", " + detailsSflow + ", " + "MyIP: " + myIP  + ", " + "TcpPort: " + tcpPort);
                break;
            case "AtypicalAmountData_sflow":
                alertEntity.setDetails(detailsFlow + ", " + detailsSflow + ", " + "MyIP: " +myIP  + ", " + "NumberOfPairs: " + numberOfPairs
                        + ", " + "DataMean: " + dataMean+ ", " + "DataStdev: " + dataStdev);
                break;
            case "AtypicalNumberOfPairs_sflow":
                alertEntity.setDetails(detailsFlow + ", " + detailsSflow + ", " + "MyIP: " +myIP + ", " + "NumberOfPairs: " + numberOfPairs
                        + ", " + "PairsMean: " + pairsMean+ ", " + "PairsStdev: " + pairsStdev);
                break;
            case "AtypicalTCPPortUsed_sflow":
                alertEntity.setDetails(detailsFlow + ", " + detailsSflow + ", " + "MyIP: " + myIP + ", " + "TcpPort: " + tcpPort);
                break;
            case "CCBotNet_sflow":
                alertEntity.setDetails(detailsFlow + ", " + detailsSflow + ", " + details + ", " + "Aliens: " + aliens);
                break;
            case "DnsTunnel_sflow":
                alertEntity.setDetails(detailsFlow + ", " + detailsSflow + ", " + details);
                break;
            case "HorizontalPortScan_sflow":
                alertEntity.setDetails(detailsFlow + ", " + detailsSflow + ", " + "NumberOfFlowsPerPort: " +numberOfFlowsPerPort + ", " + detailsPort + ", " + "FlowsMean: " + flowsMean
                        + ", " + "FlowsStdev: " + flowsStdev + ", " + "NumberOfFlowsAlienPort: " + numberOfFlowsAlienPort + ", "+ ", " + "Ports: " + ports );
                break;
            case "ICMPTunnel_sflow":
                alertEntity.setDetails(detailsFlow + ", " + detailsSflow + ", " + details);
                break;
            case "MediaStreamingClient_sflow":
                alertEntity.setDetails(detailsFlow + ", " + detailsSflow + ", " + details);
                break;
            case "P2PCommunication_sflow":
                alertEntity.setDetails(detailsFlow + ", " + "NumberOfPairs: " + numberOfPairs + ", " + detailsSflow);
                break;
            case "SMTPTalker_sflow":
                alertEntity.setDetails(detailsFlow + ", " + detailsSflow + ", " + details);
                break;
            case "UDPAmplifier_sflow":
                alertEntity.setDetails(detailsFlow + ", " + detailsSflow + ", " + details);
                break;
            case "VerticalPortScan_sflow":
                alertEntity.setDetails(detailsFlow + ", " + detailsSflow + ", " + detailsPort + ", " + "PortsMean: " + portsMean + ", " + "PortsStdev: " + portsStdev);
                break;
        }
        return alertEntity;
    }

    private String isNull(String stixModelAlert){
        if(stixModelAlert != null){
            return stixModelAlert;
        }else{
            return "-";
        }
    }

    @Override
    public List<AlertEntity> getAlerts(Date date, Long limit) {
        Pageable pageable = PageRequest.of(0, limit.intValue(), Sort.by(Sort.Direction.ASC, "timestamp"));
        if (date==null) {
            return adAlertRepository.findAll(pageable).getContent();
        }else{
            LocalDateTime start = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
            start = start.withHour(0).withMinute(0).withSecond(0).withNano(0);

            LocalDateTime end = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
            end = end.withHour(23).withMinute(59).withSecond(59).withNano(999999999);

            return adAlertRepository.findByTimestampBetween(start, end, pageable).getContent();
        }
    }

}
