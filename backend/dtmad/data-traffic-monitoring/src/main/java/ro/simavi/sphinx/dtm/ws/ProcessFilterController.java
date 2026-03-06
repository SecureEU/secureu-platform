package ro.simavi.sphinx.dtm.ws;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import ro.simavi.sphinx.dtm.model.ResponseModel;
import ro.simavi.sphinx.dtm.model.ProcessFilterModel;
import ro.simavi.sphinx.dtm.services.ProcessFilterService;
import springfox.documentation.annotations.ApiIgnore;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/process/filter")
public class ProcessFilterController {

    private final ProcessFilterService processFilterService;

    @Autowired
    public ProcessFilterController(ProcessFilterService processFilterService){
        this.processFilterService = processFilterService;
    }

    @ApiIgnore
    @PostMapping("/add")
    private ResponseModel add(@RequestBody ProcessFilterModel processFilterModel, BindingResult bindingResult, Model model, Principal principal) {
        return save(processFilterModel,bindingResult,model,principal);
    }

    @ApiIgnore
    @GetMapping("/{id}")
    private ProcessFilterModel getFilter(@PathVariable(name = "id") Long id) {
        return processFilterService.findById(id);
    }

    @ApiIgnore
    @PostMapping("/save")
    private ResponseModel save(@RequestBody ProcessFilterModel processFilterModel, BindingResult bindingResult, Model model, Principal principal) {
        if (bindingResult.hasErrors()) {
            return getResponseModel(Boolean.FALSE);
        }

        processFilterService.save(processFilterModel);

        return getResponseModel(Boolean.TRUE);
    }

    @ApiIgnore
    @GetMapping("/delete/{id}")
    private ResponseModel delete(@PathVariable(name = "id") Long id) {
        processFilterService.deleteById(id);
        return getResponseModel(Boolean.TRUE);
    }

    @ApiIgnore
    @GetMapping(value = "/all")
    public List<ProcessFilterModel> getFilters() {
        return processFilterService.findAll();
    }

    private ResponseModel getResponseModel(Boolean result){
        ResponseModel responseModel = new ResponseModel();
        responseModel.setError("");
        responseModel.setSuccess(result);
        return responseModel;
    }
}
