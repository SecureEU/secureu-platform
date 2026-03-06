package ro.simavi.sphinx.dtm.schedule;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ro.simavi.sphinx.dtm.entities.AssetCatalogueEntity;
import ro.simavi.sphinx.dtm.jpa.repositories.AssetCatalogueRepository;
import ro.simavi.sphinx.dtm.services.AssetCatalogueService;
import ro.simavi.sphinx.dtm.services.DtmConfigService;
import ro.simavi.sphinx.dtm.services.MessagingSystemService;
import ro.simavi.sphinx.dtm.util.SphinxModelHelper;
import ro.simavi.sphinx.model.ConfigModel;
import ro.simavi.sphinx.model.event.AlertMetadataModel;
import ro.simavi.sphinx.model.event.AlertModel;
import ro.simavi.sphinx.model.event.alert.ReactivateAssetAlertModel;
import ro.simavi.sphinx.model.event.alert.SphinxModel;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

@Component
public class AssetCatalogueTouchScheduler {

    private static final Logger logger = LoggerFactory.getLogger(AssetCatalogueTouchScheduler.class);

    private final AssetCatalogueService assetCatalogueService;

    private final AssetCatalogueRepository assetCatalogueRepository;

    private final DtmConfigService dtmConfigService;

    private final MessagingSystemService messagingSystemService;

    public AssetCatalogueTouchScheduler(AssetCatalogueService assetCatalogueService, AssetCatalogueRepository assetCatalogueRepository,
                                        DtmConfigService dtmConfigService, MessagingSystemService messagingSystemService){
        this.assetCatalogueService = assetCatalogueService;
        this.assetCatalogueRepository = assetCatalogueRepository;
        this.dtmConfigService = dtmConfigService;
        this.messagingSystemService=messagingSystemService;
    }

    /*
        every 60 seconds
     */
    @Scheduled(cron = "*/60 * * * * *")
    public void touch(){
        List<AssetCatalogueEntity> assetCatalogueEntities = assetCatalogueService.getAssetCatalogueList();
        for (AssetCatalogueEntity assetCatalogueEntity: assetCatalogueEntities){
            if (assetCatalogueEntity!=null && assetCatalogueEntity.getTouch()!=null && assetCatalogueEntity.getTouch()){
                AssetCatalogueEntity oldAssetCatalogueEntity = assetCatalogueRepository.findById(assetCatalogueEntity.getId()).get();

                long minutes = checkReactivateAlert(oldAssetCatalogueEntity, assetCatalogueEntity);

                oldAssetCatalogueEntity.setLastTouchDate(assetCatalogueEntity.getLastTouchDate());

                oldAssetCatalogueEntity.setLastDelay(minutes);

                assetCatalogueRepository.save(oldAssetCatalogueEntity);
            }
        }
    }

    private long checkReactivateAlert(AssetCatalogueEntity oldAssetCatalogueEntity, AssetCatalogueEntity newAssetCatalogueEntity){
        LocalDateTime oldLocalDateTime = oldAssetCatalogueEntity.getLastTouchDate();
        LocalDateTime newLocalDateTime = newAssetCatalogueEntity.getLastTouchDate();

        Long minutes = getMinutes(oldLocalDateTime,newLocalDateTime);

        Map<String, ConfigModel> configModelMap = dtmConfigService.getConfigs("dtm.assetDiscovery.timeSilent");
        ConfigModel configModel = configModelMap.get("dtm.assetDiscovery.timeSilent");

        Long timeSilentMinutes = 11520L;
        try {
            //String timeSilentExpresion = configModel.getValue();
            //*todo: convert timeSilentExpresion to timeSilentMinutes
            //timeSilentMinutes = Long.parseLong(configModel.getValue());
            timeSilentMinutes = toTimeSilentMinutes(configModel.getValue());
        }catch (Exception e){
            logger.error(e.getMessage());
        }

        if (minutes>timeSilentMinutes){
            // alert

            String timeSilentBeauty = toBeautyFormat(configModel.getValue());
            SphinxModel sphinxModel = SphinxModelHelper.getSphinxModel("tshark");

            AlertMetadataModel alertMetadataModel = new AlertMetadataModel();
            alertMetadataModel.setSignatureSeverity(new String[]{"Minor"});

            AlertModel alertModel = new AlertModel();
            alertModel.setAction("allowed");
            alertModel.setSignature("ReactivateAsset (timeSilent="+timeSilentBeauty+")");
            alertModel.setMetadata(alertMetadataModel);
            alertModel.setCategory("ReactivateAsset");

            ReactivateAssetAlertModel reactivateAssetAlertModel = new ReactivateAssetAlertModel();
            reactivateAssetAlertModel.setPhysicalAddress(oldAssetCatalogueEntity.getPhysicalAddress());
            reactivateAssetAlertModel.setName(oldAssetCatalogueEntity.getName());
            reactivateAssetAlertModel.setTimeSilent(timeSilentBeauty);
            reactivateAssetAlertModel.setLastDelay(minutes+"");
            reactivateAssetAlertModel.setSphinxModel(sphinxModel);
            reactivateAssetAlertModel.setAlert(alertModel);
            LocalDateTime dateLt = LocalDateTime.now();
            reactivateAssetAlertModel.setTimestamp(dateLt);
            reactivateAssetAlertModel.setEventType("ReactivateAssetAlertModel");

            messagingSystemService.sendAlert(reactivateAssetAlertModel);
        }

        return minutes;


    }

    private String toBeautyFormat(String timeSilentExpresion){
        String[] parts = timeSilentExpresion.split(" ");
        Long value = Long.parseLong(parts[0]);
        String period = parts[1].trim();
        if ("d".equals(period)){
            return value+" day"+(value==1?"":"s");
        }else if ("m".equals(period)){
            return value+" minute"+(value==1?"":"s");
        }else if ("h".equals(period)){
            return value+" hour"+(value==1?"":"s");
        }
        return timeSilentExpresion;
    }

    private Long toTimeSilentMinutes(String timeSilentExpresion){
        // nr. X d, X m, X h
        String[] parts = timeSilentExpresion.split(" ");
        Long value = Long.parseLong(parts[0]);
        String period = parts[1].trim();
        if ("d".equals(period)){
            return value*1440;
        }else if ("m".equals(period)){
            return value;
        }else if ("h".equals(period)){
            return value*60;
        }
        return 0L;
    }

    private Long getMinutes(LocalDateTime oldLocalDateTime, LocalDateTime newLocalDateTime) {

        LocalDateTime tempDateTime = LocalDateTime.from( oldLocalDateTime );

        long minutes = tempDateTime.until( newLocalDateTime , ChronoUnit.MINUTES );

        return minutes;
    }

}
