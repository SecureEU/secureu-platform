package ro.simavi.sphinx.dtm.services.impl.alert;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import ro.simavi.sphinx.dtm.DTMApplication;
import ro.simavi.sphinx.dtm.entities.BlackWebEntity;
import ro.simavi.sphinx.model.event.alert.BlackWebAlertModel;
import ro.simavi.sphinx.model.event.alert.SphinxModel;
import ro.simavi.sphinx.dtm.services.BlackWebService;
import ro.simavi.sphinx.dtm.services.MessagingSystemService;
import ro.simavi.sphinx.dtm.services.alert.BlackWebAlertService;
import ro.simavi.sphinx.dtm.util.SphinxModelHelper;
import ro.simavi.sphinx.model.event.AlertMetadataModel;
import ro.simavi.sphinx.model.event.AlertModel;
import ro.simavi.sphinx.model.event.EventModel;
import ro.simavi.sphinx.util.FormatHelper;
import ro.simavi.sphinx.util.PackageFields;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class BlackWebAlertServiceImpl implements BlackWebAlertService {

    /* https://github.com/maravento/blackweb/tree/master/bwupdate/lst*/
    /* https://talos-intelligence-site.s3.amazonaws.com/production/document_files/files/000/092/605/original/ip_filter.blf?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Credential=AKIAIXACIED2SPMSC7GA%2F20200615%2Fus-east-1%2Fs3%2Faws4_request&X-Amz-Date=20200615T065934Z&X-Amz-Expires=3600&X-Amz-SignedHeaders=host&X-Amz-Signature=b6d8007efa59c6e60398c9f4004e830e354d54d91452283cf1f2d68564057a9b*/

    private final BlackWebService blackWebService;

    private final MessagingSystemService messagingSystemService;

    private static final Logger logger = LoggerFactory.getLogger(BlackWebAlertServiceImpl.class);

    public BlackWebAlertServiceImpl(BlackWebService blackWebService,MessagingSystemService messagingSystemService){
        this.blackWebService = blackWebService;
        this.messagingSystemService = messagingSystemService;
    }

    private List<BlackWebEntity> blackWebEntitiesCached = null;

   // private List<BlackWebAlertModel> blackwebAlerts = new ArrayList<>();

    private List<BlackWebEntity> getBlackWebEntitiesCached(){
        if (blackWebEntitiesCached==null){
            blackWebEntitiesCached = blackWebService.getList();
        }
        return blackWebEntitiesCached;
    }

    private BlackWebEntity inBlackWeb(String dnsQuery){
        for(BlackWebEntity item: this.getBlackWebEntitiesCached()){
            if (dnsQuery.endsWith(item.getDomain())){
                return item;
            }
        }
        return null;
    }

    @Override
    public void detect(String[] pcapMessage) {
        String dnsQuery = pcapMessage[PackageFields.DNS_QRY_NAME];
        if (StringUtils.isEmpty(dnsQuery)){
            return;
        }

        BlackWebEntity blackWebEntity = inBlackWeb(dnsQuery);

        if (blackWebEntity!=null){
            String timeString = pcapMessage[PackageFields.FRAME_TIME];
            String sourceName = pcapMessage[PackageFields.ETH_SRC];
            String host = pcapMessage[PackageFields.HOSTNAME];

            LocalDateTime localDateTime = null;
            try {
                localDateTime = FormatHelper.toLocalDateTime(timeString);
            }catch (Exception e){
                logger.error(e.getMessage());
                return;
            }

            String httpHost = pcapMessage[PackageFields.HTTP_HOST];
            String ipDest = pcapMessage[PackageFields.IP_DST];
            String ipSrc = pcapMessage[PackageFields.IP_SRC];

            SphinxModel sphinxModel = SphinxModelHelper.getSphinxModel("tshark");

            AlertMetadataModel alertMetadataModel = new AlertMetadataModel();
            alertMetadataModel.setSignatureSeverity(new String[]{"Minor"});

            AlertModel alertModel = new AlertModel();
            alertModel.setAction("allowed");
            alertModel.setSignature("BlackWeb (" + blackWebEntity.getCategory().getName() + "): "+ dnsQuery);
            alertModel.setMetadata(alertMetadataModel);
            alertModel.setCategory("BlackWeb");

            BlackWebAlertModel blackWebAlertModel = new BlackWebAlertModel();

            blackWebAlertModel.setTimestamp(localDateTime);
            blackWebAlertModel.setEthSource(sourceName);
            blackWebAlertModel.setDnsQry(dnsQuery);
            blackWebAlertModel.setHttpHost(httpHost);
            blackWebAlertModel.setDestIp(ipDest);
            blackWebAlertModel.setSrcIp(ipSrc);
            blackWebAlertModel.setType(blackWebEntity.getCategory().getName());

            blackWebAlertModel.setHost(host);

            blackWebAlertModel.setSphinxModel(sphinxModel);
            blackWebAlertModel.setAlert(alertModel);

            blackWebAlertModel.setEventType("BlackWebAlertModel"); // or BlackWebAlertModel

           // blackwebAlerts.add(blackWebAlertModel);

            // TODO : generate alert
            messagingSystemService.sendAlert(blackWebAlertModel);
        }
    }

    @Override
    public void detect(EventModel eventModel) {

    }

    @Override
    public void clearCache() {
        this.blackWebEntitiesCached = null;
    }
}
