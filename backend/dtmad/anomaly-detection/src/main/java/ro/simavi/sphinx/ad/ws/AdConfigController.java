package ro.simavi.sphinx.ad.ws;

import io.swagger.annotations.ApiOperation;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import ro.simavi.sphinx.ad.model.ResponseModel;
import ro.simavi.sphinx.ad.services.AdConfigService;
import ro.simavi.sphinx.model.ConfigModel;
import springfox.documentation.annotations.ApiIgnore;

import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/config")
public class AdConfigController {

    private final AdConfigService configService;

    public AdConfigController(AdConfigService configService){
        this.configService = configService;
    }

    @ApiIgnore
    @GetMapping(value = "/all")
    public Map<String, ConfigModel> getAll() {
        return configService.getConfigs();
    }

    @ApiIgnore
    @GetMapping(value = "/all/{prefix}")
    public Map<String, ConfigModel> getAllWithPrefix(@PathVariable(name = "prefix") String prefix) {
        return configService.getConfigs(prefix);
    }

    @GetMapping(value = "/algorithmList")
    @ApiOperation(value = "Return the algorithms list used for anomaly detection")
    public Map<String, ConfigModel> getAlgorithmList() {
        return configService.getAlgorithmList();
    }

    @ApiIgnore
    @GetMapping(value = "/algorithmProperties")
    public Map<String, ConfigModel> getAlgorithmProperties() {
        return configService.getAlgorithmProperties();
    }

    @ApiIgnore
    @PostMapping("/save")
    private ResponseModel save(@RequestBody ConfigModel configModel, BindingResult bindingResult, Model model, Principal principal) {
        if (bindingResult.hasErrors()) {
            return getResponseModel(Boolean.FALSE);
        }

        configService.save(configModel);

        return getResponseModel(Boolean.TRUE);
    }

    private ResponseModel getResponseModel(Boolean result){
        ResponseModel responseModel = new ResponseModel();
        responseModel.setMessage("");
        responseModel.setSuccess(result);
        return responseModel;
    }

}
