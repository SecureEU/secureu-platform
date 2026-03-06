package ro.simavi.sphinx.dtm.services.impl;

import org.apache.commons.collections.IteratorUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import ro.simavi.sphinx.dtm.entities.AssetCatalogueEntity;
import ro.simavi.sphinx.dtm.jpa.repositories.AssetCatalogueRepository;
import ro.simavi.sphinx.dtm.model.AssetModel;
import ro.simavi.sphinx.model.event.alert.SphinxModel;
import ro.simavi.sphinx.dtm.services.AssetCatalogueService;
import ro.simavi.sphinx.dtm.services.MessagingSystemService;
import ro.simavi.sphinx.dtm.util.DtmTools;
import ro.simavi.sphinx.dtm.util.SphinxModelHelper;
import ro.simavi.sphinx.model.event.EthernetModel;
import ro.simavi.sphinx.model.event.EventModel;
import ro.simavi.sphinx.util.FormatHelper;
import ro.simavi.sphinx.util.PackageFields;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class AssetCatalogueServiceImpl implements AssetCatalogueService {

    private final AssetCatalogueRepository assetCatalogueRepository;

    private final MessagingSystemService messagingSystemService;

    private List<AssetModel> assetAlerts = new ArrayList<>();

    private List<AssetCatalogueEntity> assetCatalogueEntitiesCached = null;

    private static final Logger logger = LoggerFactory.getLogger(AssetCatalogueServiceImpl.class);

    public AssetCatalogueServiceImpl(AssetCatalogueRepository assetCatalogueRepository,
                                     MessagingSystemService messagingSystemService){
        this.assetCatalogueRepository = assetCatalogueRepository;
        this.messagingSystemService = messagingSystemService;
    }

    @Override
    public List<AssetCatalogueEntity> getAssetCatalogueList() {
        return getAssetCatalogueListCached();
    }

    private List<AssetCatalogueEntity> getAssetCatalogueListCached(){
        if (assetCatalogueEntitiesCached==null){
            assetCatalogueEntitiesCached = IteratorUtils.toList(assetCatalogueRepository.findAll().iterator());
        }
        return assetCatalogueEntitiesCached;
    }

    @Override
    public AssetCatalogueEntity findById(Long id) {
        Optional<AssetCatalogueEntity> assetCatalogueEntityOptional = assetCatalogueRepository.findById(id);
        if (assetCatalogueEntityOptional.isPresent()){
            AssetCatalogueEntity assetCatalogueEntity = assetCatalogueEntityOptional.get();
            return assetCatalogueEntity;
        }
        return null;
    }

    @Override
    public void save(AssetModel assetModel) {
        //TypeId = 1 - assetCatalog
        //TypeId = 2 - assetDescovery
        AssetCatalogueEntity assetCatalogueEntity = toAssetCatalogueEntity(assetModel);

        if ((assetCatalogueEntity.getId() != null) && (assetCatalogueEntity.getTypeId() == 1)){
            Optional<AssetCatalogueEntity> assetCatalogueEntityOptional = assetCatalogueRepository.findById(assetCatalogueEntity.getId());
            if (assetCatalogueEntityOptional.isPresent()){
                AssetCatalogueEntity assetCatalogueEntityOld = assetCatalogueEntityOptional.get();
                assetCatalogueEntityOld.setName(assetCatalogueEntity.getName());
                assetCatalogueEntityOld.setDescription(assetCatalogueEntity.getDescription());
                this.assetCatalogueRepository.save(assetCatalogueEntityOld);
            }
        }else{
            this.assetCatalogueRepository.save(assetCatalogueEntity);
        }

        if (assetCatalogueEntity!=null && (assetCatalogueEntity.getTypeId() == 1)) {
            // put on topic: dtm-asset

            SphinxModel sphinxModel =  assetModel.getSphinx();
            if (sphinxModel==null) {
                sphinxModel = SphinxModelHelper.getSphinxModel("tshark");
            }
            AssetModel responseAssetModel = new AssetModel();
            responseAssetModel.setId(assetCatalogueEntity.getId().toString());
            responseAssetModel.setSphinx(sphinxModel);
            responseAssetModel.setDescription(assetCatalogueEntity.getDescription());
            responseAssetModel.setPhysicalAddress(assetCatalogueEntity.getPhysicalAddress());
            responseAssetModel.setTimestamp(assetCatalogueEntity.getCreatedDate());
            responseAssetModel.setStatus("allowed");
            responseAssetModel.setName(assetCatalogueEntity.getName());
            responseAssetModel.setIp(assetCatalogueEntity.getIp());
            responseAssetModel.setLastTouch(assetCatalogueEntity.getLastTouchDate());
            messagingSystemService.sendAsset(responseAssetModel);
        }


        if(assetCatalogueEntity != null) {
            this.refreshDiscovery(assetCatalogueEntity);
        }
        this.clearCache();
    }

    private AssetCatalogueEntity toAssetCatalogueEntity(AssetModel assetModel) {
        List<AssetCatalogueEntity> list = IteratorUtils.toList(assetCatalogueRepository.findAll().iterator());
        for(AssetCatalogueEntity oldAssetCatalogueEntity: list){
            if ((assetModel.getPhysicalAddress().equals(oldAssetCatalogueEntity.getPhysicalAddress()))){
                AssetCatalogueEntity newAssetCatalogueEntity = new AssetCatalogueEntity();
                if (!StringUtils.isEmpty(assetModel.getId())){
                    Long id = Long.parseLong(assetModel.getId());
                    newAssetCatalogueEntity.setId(id);
                }

                newAssetCatalogueEntity.setPhysicalAddress(oldAssetCatalogueEntity.getPhysicalAddress());
                newAssetCatalogueEntity.setIp(oldAssetCatalogueEntity.getIp());
                newAssetCatalogueEntity.setLastTouchDate(oldAssetCatalogueEntity.getLastTouchDate());
                newAssetCatalogueEntity.setTypeId(1L);

                newAssetCatalogueEntity.setName(assetModel.getName());
                newAssetCatalogueEntity.setDescription(assetModel.getDescription());

                return newAssetCatalogueEntity;
            }
        }
        return null;

    }

    @Override
    public void delete(Long id) {
        // put on topic: dtm-asset

        Optional<AssetCatalogueEntity> assetCatalogueEntityOptional = assetCatalogueRepository.findById(id);

        if (assetCatalogueEntityOptional.isPresent()){
            AssetCatalogueEntity assetCatalogueEntity = assetCatalogueEntityOptional.get();

            SphinxModel sphinxModel = SphinxModelHelper.getSphinxModel("tshark");

            AssetModel responseAssetModel = new AssetModel();
            if (assetCatalogueEntity.getTypeId() == 1){
                responseAssetModel.setId(assetCatalogueEntity.getId().toString());
                responseAssetModel.setSphinx(sphinxModel);
                responseAssetModel.setDescription(assetCatalogueEntity.getDescription());
                responseAssetModel.setPhysicalAddress(assetCatalogueEntity.getPhysicalAddress());
                responseAssetModel.setTimestamp(assetCatalogueEntity.getCreatedDate());
                responseAssetModel.setStatus("deleted");
                responseAssetModel.setName(assetCatalogueEntity.getName());
                responseAssetModel.setIp(assetCatalogueEntity.getIp());
                responseAssetModel.setLastTouch(assetCatalogueEntity.getLastTouchDate());
                messagingSystemService.sendAsset(responseAssetModel);
            }
        }

        this.assetCatalogueRepository.deleteById(id);

        this.clearCache();
    }

    // suricata
    @Override
    public void collect(EventModel metricModel) {
        EthernetModel ethernetModel = metricModel.getEther();
        String physicalAddress = ethernetModel.getSrcMac();
        String ip = metricModel.getSrcIp();
        LocalDateTime time = metricModel.getTimestamp();
        if (ethernetModel!=null){
            add(physicalAddress, ip, time, DtmTools.SURICATA);
        }
    }

    // tshark
    @Override
    public void collect(String[] pcapMessage) {

        if (pcapMessage.length<=PackageFields.IP_SRC){
            logger.warn("collect / IP_SRC [11]"+Arrays.toString(pcapMessage));
            return;
        }

        String physicalAddress = pcapMessage[PackageFields.ETH_SRC]; //eth.src
        String ip = pcapMessage[PackageFields.IP_SRC];
        String time = (pcapMessage[PackageFields.FRAME_TIME]+"");
        LocalDateTime localDateTime = null;
        Long timestamp = null;
        if (!StringUtils.isEmpty(time)){
            try {
                timestamp = FormatHelper.getTimestamp(time);
            }catch (Exception e){
                System.out.println("-------------->" + time);
            }

            try {
                localDateTime = FormatHelper.toLocalDateTime(timestamp);
            }catch (Exception e){
                System.out.println("===>1" + time + " -- " + timestamp);
            }

        }
        add(physicalAddress, ip, localDateTime, DtmTools.TSHARK);
    }

    private void add(String physicalAddress, String ip, LocalDateTime time, DtmTools dtmTools){
        if (!StringUtils.isEmpty(physicalAddress)) {

            AssetCatalogueEntity existAssetCatalog = getAssetCatalogueEntityRemoveDuplicate(physicalAddress);

            boolean exists = false;
            if(existAssetCatalog != null && existAssetCatalog.getTypeId() == 2){
                existAssetCatalog.setLastTouchDate(time);
                exists = true;
            }

            if(!exists){
                exists = touch(physicalAddress, time);
            }

            if (!exists) {
                // put on topic: dtm-asset
                SphinxModel sphinxModel = SphinxModelHelper.getSphinxModel(dtmTools.getName());

                AssetModel assetModel = new AssetModel();
                assetModel.setSphinx(sphinxModel);
                assetModel.setDescription("");
                assetModel.setPhysicalAddress(physicalAddress);
                assetModel.setTimestamp(time);
                assetModel.setLastTouch(time);
                assetModel.setStatus("alert");
                assetModel.setIp(ip);
                assetModel.setId(UUID.randomUUID().toString());
                assetAlerts.add(assetModel);
                messagingSystemService.sendAsset(assetModel);
                this.persist(assetModel);
            }
        }
    }

    public void persist(AssetModel assetModel) {
        AssetCatalogueEntity alert = new AssetCatalogueEntity();
        alert.setPhysicalAddress(assetModel.getPhysicalAddress());
        alert.setLastTouchDate(assetModel.getLastTouch());
        alert.setIdAlert(assetModel.getId());
        alert.setTypeId(2L);
        this.assetCatalogueRepository.save(alert);
    }

    private boolean touch(String physicalAddress, LocalDateTime time){
        AssetCatalogueEntity assetCatalogueEntity = getAssetCatalogueEntityByPhysicalAddress(physicalAddress);
        if (assetCatalogueEntity!=null && assetCatalogueEntity.getTypeId() == 1){
            assetCatalogueEntity.setTouch(Boolean.TRUE);
            LocalDateTime oldTime = assetCatalogueEntity.getLastTouchDate();
            if (time!=null && time.isAfter(oldTime)){
                assetCatalogueEntity.setLastTouchDate(time);
            }
            return true;
        }
        return false;
    }

    public boolean untouch(String physicalAddress){
        AssetCatalogueEntity assetCatalogueEntity = getAssetCatalogueEntityByPhysicalAddress(physicalAddress);
        if (assetCatalogueEntity!=null){
            assetCatalogueEntity.setTouch(Boolean.FALSE);
            return true;
        }
        return false;
    }

    private AssetModel getAssetModelByPhysicalAddress(String physicalAddress){
        for(AssetModel assetModel: assetAlerts){
            if (physicalAddress.equals(assetModel.getPhysicalAddress())){
                return assetModel;
            }
        }
        return null;
    }


    private AssetCatalogueEntity getAssetCatalogueEntityByPhysicalAddress(String physicalAddress){
        for(AssetCatalogueEntity assetCatalogueEntity: getAssetCatalogueListCached()){
            if ((physicalAddress.equals(assetCatalogueEntity.getPhysicalAddress()))){
                return assetCatalogueEntity;
            }
        }
        return null;
    }
    private AssetCatalogueEntity getAssetCatalogueEntityRemoveDuplicate(String physicalAddress){
        List<AssetCatalogueEntity> existAssetCatalog = IteratorUtils.toList(assetCatalogueRepository.findAll().iterator());
        for(AssetCatalogueEntity assetCatalogueEntity: existAssetCatalog){
            if(physicalAddress.equals(assetCatalogueEntity.getPhysicalAddress()) ){
               return assetCatalogueEntity;
            }
        }
        return null;
    }

    /*
    private boolean exists(String physicalAddress){
        for(AssetModel assetModel: assetAlerts){
            if (physicalAddress.equals(assetModel.getPhysicalAddress())){
                return true;
            }
        }
        for(AssetCatalogueEntity assetCatalogueEntity: getAssetCatalogueListCached()){
            if (physicalAddress.equals(assetCatalogueEntity.getPhysicalAddress())){
                return true;
            }
        }
        return false;
    }
    */

    private void refreshDiscovery(AssetCatalogueEntity catalogueEntity){
        List<AssetCatalogueEntity> list = IteratorUtils.toList(assetCatalogueRepository.findAll().iterator());
            for(AssetCatalogueEntity discoveryEntity: list){
                if ((discoveryEntity.getTypeId() == 2) && (catalogueEntity.getPhysicalAddress().equals(discoveryEntity.getPhysicalAddress()))){
                    this.assetCatalogueRepository.deleteById(discoveryEntity.getId());
                }
            }
    }

    @Override
    public List<AssetModel> getAlerts() {
        return this.assetAlerts;
    }

    @Override
    public List<AssetCatalogueEntity> getAssetAlerts() {
        return (List<AssetCatalogueEntity>) this.assetCatalogueRepository.findAll();
    }

    @Override
    public void clearCache() {
        this.assetCatalogueEntitiesCached = null;
    }
}
