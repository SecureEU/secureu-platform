package ro.simavi.sphinx.dtm.services.impl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ro.simavi.sphinx.dtm.entities.*;
import ro.simavi.sphinx.dtm.jpa.repositories.DtmAlertRepository;
import ro.simavi.sphinx.dtm.services.AlertService;
import ro.simavi.sphinx.model.event.AlertEventModel;
import ro.simavi.sphinx.model.event.alert.BlackWebAlertModel;
import ro.simavi.sphinx.model.event.alert.MassiveDataProcessingAlertModel;
import ro.simavi.sphinx.model.event.alert.ReactivateAssetAlertModel;
import ro.simavi.sphinx.model.event.alert.TcpAnalysisFlagsAlertModel;
import ro.simavi.sphinx.model.event.alertStix.StixModelAlert;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;


@Service
public class AlertServiceImpl implements AlertService {

    private List<AlertEventModel> alertModelList = new ArrayList<>();

    private final DtmAlertRepository alertRepository;

    public AlertServiceImpl(DtmAlertRepository alertRepository) {
        this.alertRepository = alertRepository;
    }

    @Value("${spring.dtm.numberOfDays:180}")
    private String numberOfDays;

    @Override
    public void collect(AlertEventModel alertModel, StixModelAlert stixModelAlert) {

        alertModelList.add(alertModel);
        AlertEventModel alertEventModel = getAlertEventModel(alertModelList, alertModel);
        if(alertModel.getCount() == null){
            alertModel.setCount(0L);
        }
        if (alertEventModel==null) {
            alertModel.setCount(1L);
            alertModelList.add(alertModel);
        }else{
            alertEventModel.setTimestamp(alertModel.getTimestamp());
            alertEventModel.setCount(alertEventModel.getCount()+1);
        }

        this.persist(alertModel, stixModelAlert);

    }

    private void persist(AlertEventModel alertModel, StixModelAlert stixModelAlert) {
        if(alertModel.getType() !=null && alertModel.getType().equals("bundle")){
            this.alertRepository.save(toAlertStix(stixModelAlert));
        }else{
            this.alertRepository.save(toAlert(alertModel));
        }
    }

    @Scheduled(fixedDelay=20000)
    public void deleteByNumberOfDays(){
        if(this.alertRepository.count() > 0){
            LocalDateTime dateDeleted = LocalDateTime.now().minusDays(Integer.parseInt(numberOfDays));
            DateTimeFormatter formatters = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            String date = dateDeleted.format(formatters);
            LocalDateTime parsedDate = LocalDateTime.parse(date, formatters);

            this.alertRepository.deleteByNumberOfDays(parsedDate);
        }
    }

    private AlertEntity toAlertStix(StixModelAlert stixModelAlert){
        AlertEntity alertEntity = new SuricataAlertEntity();

        String dateSplit = stixModelAlert.getObjects().get(0).getDetails().getTimestamp();
        String[] dateZone  = dateSplit.split("G");
        String[] dataTime = dateZone[0].split("T");
        String datafinal = dataTime[0]+ " "+ dataTime[1];
        String[] dataWithoutNs = datafinal.split("\\.");

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withLocale(Locale.ENGLISH);
        LocalDateTime dateTime = LocalDateTime.parse(dataWithoutNs[0], formatter);
        alertEntity.setTimestamp(dateTime);
        alertEntity.setCount(1);
        alertEntity.setProtocol(stixModelAlert.getObjects().get(0).getDetails().getProto());
        alertEntity.setHost(stixModelAlert.getObjects().get(0).getDetails().getHost());
        alertEntity.setSrcIp(stixModelAlert.getObjects().get(0).getDetails().getSrcIp());
        alertEntity.setSrcPort(String.valueOf(stixModelAlert.getObjects().get(0).getDetails().getSrcPort()));
        alertEntity.setDestIp(stixModelAlert.getObjects().get(0).getDetails().getDestIp());
        alertEntity.setDestPort(String.valueOf(stixModelAlert.getObjects().get(0).getDetails().getDestPort()));
        alertEntity.setEventType(stixModelAlert.getEventType());
        alertEntity.setSignature(stixModelAlert.getObjects().get(0).getDetails().getAlert().getSignature());
        alertEntity.setSeverity(stixModelAlert.getObjects().get(0).getDetails().getAlert().getSeverity());
        alertEntity.setCategory(stixModelAlert.getObjects().get(0).getDetails().getAlert().getCategory());
        alertEntity.setAction(stixModelAlert.getObjects().get(0).getDetails().getAlert().getAction());
        alertEntity.setSphinxTool("DTM");
        alertEntity.setDetails("Flow id: "+ stixModelAlert.getFlowId());
        ((SuricataAlertEntity) alertEntity).setLocation("Country: " + stixModelAlert.getObjects().get(0).getDetails().getSrcGeoip().getCountryName()
                    + ", " + "City: " + stixModelAlert.getObjects().get(0).getDetails().getSrcGeoip().getCityName()
                    + ", " + "lon/lat: " + String.valueOf(stixModelAlert.getObjects().get(0).getDetails().getSrcGeoip().getLatitude()) +
                    "/" + String.valueOf(stixModelAlert.getObjects().get(0).getDetails().getSrcGeoip().getLatitude()));
        ((SuricataAlertEntity) alertEntity).setRelationshipType(stixModelAlert.getObjects().get(2).getRelationshipType());
        ((SuricataAlertEntity) alertEntity).setTargetRef(stixModelAlert.getObjects().get(2).getTargetRef());
        ((SuricataAlertEntity) alertEntity).setSourceRef(stixModelAlert.getObjects().get(2).getSourceRef());

        return alertEntity;

    }

    private AlertEntity toAlert(AlertEventModel alertEventModel) {
        // transform alertModel => flat model to save into postgresql
        AlertEntity alertEntity = null;
        if(alertEventModel != null){
            alertEntity = new SuricataAlertEntity();
            alertEntity.setTimestamp(alertEventModel.getTimestamp2());
            if (alertEventModel instanceof BlackWebAlertModel) {
                alertEntity = new BlackWebAlertEntity();
                BlackWebAlertModel blackWebAlertModel = (BlackWebAlertModel) alertEventModel;
                alertEntity.setDetails("Eth Source: "+blackWebAlertModel.getEthSource() + ", " + "HttpHost: "+
                        blackWebAlertModel.getHttpHost() + ", "+ "Dns Qry: " + blackWebAlertModel.getDnsQry() +
                        ", " + "Type: " + blackWebAlertModel.getType() +", "+  "FlowId: " + alertEventModel.getFlowId());
            }

            if (alertEventModel instanceof MassiveDataProcessingAlertModel) {
                alertEntity = new MassiveDataProcessingAlertEntity();
                MassiveDataProcessingAlertModel massiveDataProcessingAlertModel = (MassiveDataProcessingAlertModel) alertEventModel;
                alertEntity.setDetails("Bytes: "+massiveDataProcessingAlertModel.getBytes() + ", " + "Packets: "+
                        massiveDataProcessingAlertModel.getPackets() + ", "+ "Lenght: " + massiveDataProcessingAlertModel.getLen()+
                        ", "+"FlowId: " + alertEventModel.getFlowId());
            }

            if (alertEventModel instanceof ReactivateAssetAlertModel) {
                alertEntity = new ReactivateAssetAlertEntity();
                ReactivateAssetAlertModel reactivateAsset = (ReactivateAssetAlertModel) alertEventModel;
                alertEntity.setDetails("TimeSilent: "+reactivateAsset.getTimeSilent() + ", " + "LastDelay: "+
                        reactivateAsset.getLastDelay() + ", "+ "PhysicalAddress: " + reactivateAsset.getPhysicalAddress() +
                        ", " + "Name: " + reactivateAsset.getName()+"," + "FlowId: " + alertEventModel.getFlowId());
                alertEntity.setTimestamp(alertEventModel.getTimestamp());
            }
            if (alertEventModel instanceof TcpAnalysisFlagsAlertModel) {
                alertEntity = new TcpAnalysisFlagsAlerteEntity();
                TcpAnalysisFlagsAlertModel tcpAnalysisFlags = (TcpAnalysisFlagsAlertModel) alertEventModel;
                alertEntity.setDetails("Bytes: "+tcpAnalysisFlags.getBytes() + ", " + "Info: "+
                        tcpAnalysisFlags.getInfo()+ ", "+ "FlowId: " + alertEventModel.getFlowId());
            }

            if(alertEventModel.getCount() != null){
                alertEntity.setCount(alertEventModel.getCount().intValue());
            }else {
                alertEntity.setCount(1);
            }
            alertEntity.setSphinxTool("DTM");

            alertEntity.setProtocol(alertEventModel.getProtocol());
            alertEntity.setHost(alertEventModel.getHost());
            alertEntity.setSrcIp(alertEventModel.getSrcIp());
            alertEntity.setSrcPort(alertEventModel.getSrcPort());
            alertEntity.setDestIp(alertEventModel.getDestIp());
            alertEntity.setDestPort(alertEventModel.getDestPort());
            //daca primeste o alerta de suricata care nu este stix
            if(alertEventModel.getType() != null && alertEventModel.getType().equals("SuricataIDPS")){
                alertEntity.setEventType(alertEventModel.getEventKind());
            }
            else{
                alertEntity.setEventType(alertEventModel.getEventType());
            }

            if(alertEventModel.getAlert() != null){
                alertEntity.setSeverity(alertEventModel.getAlert().getSeverity());
                alertEntity.setSignature(alertEventModel.getAlert().getSignature());
                alertEntity.setCategory(alertEventModel.getAlert().getCategory());
                alertEntity.setAction(alertEventModel.getAlert().getAction());
                if(alertEventModel.getAlert().getMetadata() != null){
                    if (alertEventModel.getAlert().getMetadata().getSignatureSeverity() != null) {
                        alertEntity.setSignatureSeverity(alertEventModel.getAlert().getMetadata().getSignatureSeverity()[0]);

                    }else{
                        alertEntity.setSignatureSeverity(" ");
                    }
                }

            }
        }
        return alertEntity;
    }

    private AlertEventModel getAlertEventModel(List<AlertEventModel> alertModelList, AlertEventModel metricModel) {
        if(metricModel.getType()!= null && metricModel.getType().equals("bundle")){
            return null;
        }else{
            for (AlertEventModel alertEventModel : alertModelList) {
                if(metricModel.getAlert() != null && alertEventModel.getAlert() != null && metricModel.getAlert().getMetadata() !=null){
                    if (equals(alertEventModel.getHost(), metricModel.getHost()) &&
                            equals(alertEventModel.getSrcIp(), metricModel.getSrcIp()) &&
                            equals(alertEventModel.getSrcPort(), metricModel.getSrcPort()) &&
                            equals(alertEventModel.getDestIp(), metricModel.getDestIp()) &&
                            equals(alertEventModel.getDestPort(), metricModel.getDestPort()) &&
                            equals(alertEventModel.getAlert().getSignature(), metricModel.getAlert().getSignature()) &&
                            equals(alertEventModel.getAlert().getMetadata().getSignatureSeverity()[0], metricModel.getAlert().getMetadata().getSignatureSeverity()[0])) {
                        return alertEventModel;
                    }
                }
            }
        }
       return null;

    }

    private boolean equals(String item1, String item2) {
        if (item1 == null && item2 == null) {
            return true;
        }
        if (item1 != null && item2 != null && item1.equals(item2)) {
            return true;
        }
        return false;
    }

    @Override
    public List<AlertEventModel> getAlerts() {
        Collections.sort(alertModelList, new Comparator<AlertEventModel>() {

            @Override
            public int compare(AlertEventModel o1, AlertEventModel o2) {
                return o2.getTimestamp().compareTo(o1.getTimestamp());
            }

        });
        return alertModelList;
    }

    @Override
    public Page<AlertEntity> getAlertFiler(String prefix, int pageNo, int pageSize) {

        Pageable pageable = PageRequest.of(pageNo, pageSize, Sort.by(Sort.Direction.DESC, "timestamp"));
        return alertRepository.findAllBySphinxTool(prefix, pageable);

    }

    @Override
    public List<AlertEntity> getAlerts(Date date, Long limit) {
        Pageable pageable = PageRequest.of(0, limit.intValue(), Sort.by(Sort.Direction.ASC, "timestamp"));
        if (date==null) {
            return alertRepository.findAll(pageable).getContent();
        }else{
            LocalDateTime start = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
            start = start.withHour(0).withMinute(0).withSecond(0).withNano(0);

            LocalDateTime end = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
            end = end.withHour(23).withMinute(59).withSecond(59).withNano(999999999);

            return alertRepository.findByTimestampBetween(start, end, pageable).getContent();
        }
    }

    @Override
    public void removeAll() {
        alertModelList.clear();
    }

}
