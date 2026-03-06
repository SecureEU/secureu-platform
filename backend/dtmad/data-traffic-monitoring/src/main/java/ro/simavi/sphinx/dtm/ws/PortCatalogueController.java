package ro.simavi.sphinx.dtm.ws;

import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import ro.simavi.sphinx.dtm.model.PortCatalogueModel;
import ro.simavi.sphinx.dtm.model.ResponseModel;
import ro.simavi.sphinx.dtm.services.InstanceService;
import ro.simavi.sphinx.dtm.services.alert.PortCatalogueAlertService;
import ro.simavi.sphinx.dtm.services.impl.ToolRemoteServiceImpl;
import springfox.documentation.annotations.ApiIgnore;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/portcatalogue")
public class PortCatalogueController extends CacheableController {

    private final PortCatalogueAlertService portCatalogueAlertService;

    public PortCatalogueController(PortCatalogueAlertService portCatalogueAlertService,
                                   InstanceService instanceService,
                                   ToolRemoteServiceImpl toolRemoteService){
        super(instanceService, toolRemoteService);
        this.portCatalogueAlertService = portCatalogueAlertService;
    }

    @GetMapping(value = "/getPortCatalogueList")
    public List<PortCatalogueModel> getPortCatalogueList() {
        return portCatalogueAlertService.getPortCatalogueList();
    }

    @ApiIgnore
    @GetMapping(value = "/{id}")
    public PortCatalogueModel getById(@PathVariable(name = "id") Long id) {
        return portCatalogueAlertService.findById(id);
    }

    @ApiIgnore
    @PostMapping("/save")
    public ResponseModel save(@RequestBody PortCatalogueModel portCatalogueEntity, BindingResult bindingResult, Model model, Principal principal) {
        if (bindingResult.hasErrors()) {
            return getResponseModel(Boolean.FALSE);
        }

        portCatalogueAlertService.save(portCatalogueEntity);

        // reset cache for all instances;
        clearCacheForAllInstances("portcatalogue");

        return getResponseModel(Boolean.TRUE);
    }

    @ApiIgnore
    @GetMapping(value = "/delete/{id}")
    public ResponseModel getDeleteInstance(@PathVariable(name = "id") Long id) {

        portCatalogueAlertService.delete(id);

        // reset cache for all instances;
        clearCacheForAllInstances("portcatalogue");

        return getResponseModel(Boolean.TRUE);
    }

    @GetMapping(value = "/getPortDiscoveryAlerts")
    public List<PortCatalogueModel> getAssetDiscoveryAlerts() {
        return portCatalogueAlertService.getAlerts();
    }

    @ApiIgnore
    @GetMapping(value = "/clearCache")
    public void clearCache() {
        portCatalogueAlertService.clearCache();
    }

    private ResponseModel getResponseModel(Boolean result){
        ResponseModel responseModel = new ResponseModel();
        responseModel.setError("");
        responseModel.setSuccess(result);
        return responseModel;
    }

}
