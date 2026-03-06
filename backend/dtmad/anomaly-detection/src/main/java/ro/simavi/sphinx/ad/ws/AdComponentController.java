package ro.simavi.sphinx.ad.ws;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ro.simavi.sphinx.ad.entities.AdComponentEntity;
import ro.simavi.sphinx.ad.jpa.repositories.AdComponentRepository;
import ro.simavi.sphinx.ad.model.ResponseModel;
import ro.simavi.sphinx.ad.services.AdComponentService;
import ro.simavi.sphinx.ad.services.AnomalyDetectionService;
import springfox.documentation.annotations.ApiIgnore;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/component")
public class AdComponentController {

    private final AdComponentService adComponentService;

    private final AnomalyDetectionService anomalyDetectionService;

    private final AdComponentRepository adComponentRepository;

    public AdComponentController(AdComponentService adComponentService,
                                 AnomalyDetectionService anomalyDetectionService,
                                 AdComponentRepository adComponentRepository){
        this.adComponentService=adComponentService;
        this.anomalyDetectionService = anomalyDetectionService;
        this.adComponentRepository = adComponentRepository;
    }

    @ApiIgnore
    @GetMapping(value = "/list")
    public List<AdComponentEntity> getList() {
        return adComponentService.getAdComponentList();
    }

    @ApiIgnore
    @GetMapping(value = "/disable/{id}")
    public AdComponentEntity disable(@PathVariable(name = "id") Long id) {
        Optional<AdComponentEntity> componentEntityOptional = adComponentRepository.findById(id);
        if (componentEntityOptional.isPresent()) {

            AdComponentEntity adComponentEntity = adComponentService.enable(componentEntityOptional.get(), Boolean.FALSE);

            adComponentService.enable(componentEntityOptional.get(), Boolean.FALSE);
            anomalyDetectionService.removeComponentService(componentEntityOptional.get());

            return adComponentEntity;

        }
        return componentEntityOptional.get();
    }

    @ApiIgnore
    @GetMapping(value = "/enable/{id}")
    public AdComponentEntity enable(@PathVariable(name = "id") Long id) {
        Optional<AdComponentEntity> componentEntityOptional = adComponentRepository.findById(id);
        if (componentEntityOptional.isPresent()) {
            AdComponentEntity adComponentEntity =  adComponentService.enable(componentEntityOptional.get(), Boolean.TRUE);
            anomalyDetectionService.addComponentService(componentEntityOptional.get());
            return adComponentEntity;
        }
        return componentEntityOptional.get();
    }

    private ResponseModel getResponseModel(Boolean result){
        ResponseModel responseModel = new ResponseModel();
        responseModel.setMessage("");
        responseModel.setSuccess(result);
        return responseModel;
    }
}
