package ro.simavi.sphinx.dtm.ws;

import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import ro.simavi.sphinx.dtm.model.ResponseModel;
import ro.simavi.sphinx.dtm.services.DtmConfigService;
import ro.simavi.sphinx.model.ConfigModel;
import springfox.documentation.annotations.ApiIgnore;

import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/config")
public class DtmConfigController {

    private final DtmConfigService configService;

    public DtmConfigController(DtmConfigService configService){
        this.configService = configService;
    }

    @GetMapping(value = "/all")
    public Map<String, ConfigModel> getAll() {
        return configService.getConfigs();
    }

    @GetMapping(value = "/all/{prefix}")
    public Map<String, ConfigModel> getAllWithPrefix(@PathVariable(name = "prefix") String prefix) {
        return configService.getConfigs(prefix);
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
        responseModel.setError("");
        responseModel.setSuccess(result);
        return responseModel;
    }

}
