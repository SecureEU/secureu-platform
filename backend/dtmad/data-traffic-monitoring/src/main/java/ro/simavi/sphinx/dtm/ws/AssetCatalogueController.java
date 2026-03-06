package ro.simavi.sphinx.dtm.ws;

import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import ro.simavi.sphinx.dtm.entities.AssetCatalogueEntity;
import ro.simavi.sphinx.dtm.model.AssetModel;
import ro.simavi.sphinx.dtm.model.ResponseModel;
import ro.simavi.sphinx.dtm.services.AssetCatalogueService;
import ro.simavi.sphinx.dtm.services.InstanceService;
import ro.simavi.sphinx.dtm.services.impl.ToolRemoteServiceImpl;
import springfox.documentation.annotations.ApiIgnore;

import javax.validation.Valid;
import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/assetcatalogue")
public class AssetCatalogueController extends CacheableController{

    private final AssetCatalogueService assetCatalogueService;

    public AssetCatalogueController(AssetCatalogueService assetCatalogueService,
                                    InstanceService instanceService,
                                    ToolRemoteServiceImpl toolRemoteService ){
        super(instanceService, toolRemoteService);
        this.assetCatalogueService = assetCatalogueService;
    }

    @GetMapping(value = "/getAssetCatalogueList")
    public List<AssetCatalogueEntity> getAssetCatalogueList() {
        return assetCatalogueService.getAssetCatalogueList();
    }
    
    @GetMapping(value = "/{id}")
    public AssetCatalogueEntity getById(@PathVariable(name = "id") Long id) {
        return assetCatalogueService.findById(id);
    }

    /*
        POST: sphinx/dtm/assetcatalogue/save
        {
            "physicalAddress":"10:15:17:c3:df:d8",
            "name":"test",
            "description":"test"
        }
     */
    @PostMapping("/save")
    public ResponseModel save(@RequestBody @Valid AssetModel assetModel, BindingResult bindingResult, Model model, Principal principal) {
        if (bindingResult.hasErrors()) {
            return getResponseModel(Boolean.FALSE);
        }

        assetCatalogueService.save(assetModel);

        // reset cache for all instances;
        clearCacheForAllInstances("assetcatalogue");

        return getResponseModel(Boolean.TRUE);
    }

    @GetMapping(value = "/delete/{id}")
    public ResponseModel getDeleteInstance(@PathVariable(name = "id") Long id) {

        assetCatalogueService.delete(id);

        // reset cache for all instances;
        clearCacheForAllInstances("assetcatalogue");

        return getResponseModel(Boolean.TRUE);
    }

    @ApiIgnore
    @GetMapping(value = "/clearCache")
    public void clearCache() {
        assetCatalogueService.clearCache();
    }


    @GetMapping(value = "/getAssetDiscoveryAlerts")
    public List<AssetCatalogueEntity> getAssetDiscoveryAlerts() {
        return assetCatalogueService.getAssetAlerts();
    }

    private ResponseModel getResponseModel(Boolean result){
        ResponseModel responseModel = new ResponseModel();
        responseModel.setError("");
        responseModel.setSuccess(result);
        return responseModel;
    }

}
