package ro.simavi.sphinx.ad.ws;

import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import ro.simavi.sphinx.ad.model.ResponseModel;
import ro.simavi.sphinx.ad.services.AdConfigService;
import ro.simavi.sphinx.ad.services.ReputationService;
import ro.simavi.sphinx.model.ConfigModel;
import springfox.documentation.annotations.ApiIgnore;

import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/update")
public class UpdateController {

    private final ReputationService reputationService;

    public UpdateController(ReputationService reputationService){
        this.reputationService = reputationService;
    }

    @ApiIgnore
    @PostMapping("/reputation")
    private ResponseModel updateReputation(@RequestBody ConfigModel configModel, BindingResult bindingResult, Model model, Principal principal) {
        if (bindingResult.hasErrors()) {
            return getResponseModel(Boolean.FALSE);
        }

        reputationService.update();

        return getResponseModel(Boolean.TRUE);
    }

    private ResponseModel getResponseModel(Boolean result){
        ResponseModel responseModel = new ResponseModel();
        responseModel.setMessage("");
        responseModel.setSuccess(result);
        return responseModel;
    }

}
