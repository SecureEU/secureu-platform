package ro.simavi.sphinx.dtm.ws;

import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import ro.simavi.sphinx.dtm.entities.BlackWebCategoryEntity;
import ro.simavi.sphinx.dtm.model.ResponseModel;
import ro.simavi.sphinx.dtm.services.BlackWebCategoryService;
import springfox.documentation.annotations.ApiIgnore;

import javax.validation.Valid;
import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/blackwebcategory")
public class BlackWebCategoryController {

    private BlackWebCategoryService blackWebCategoryService;

    public BlackWebCategoryController(BlackWebCategoryService blackWebCategoryService){
        this.blackWebCategoryService=blackWebCategoryService;
    }

    @GetMapping(value = "/list")
    public List<BlackWebCategoryEntity> getList() {
        return blackWebCategoryService.getList();
    }

    @ApiIgnore
    @GetMapping(value = "/{id}")
    public BlackWebCategoryEntity getById(@PathVariable(name = "id") Long id) {
        return blackWebCategoryService.findById(id);
    }

    @ApiIgnore
    @PostMapping("/save")
    private ResponseModel save(@RequestBody @Valid BlackWebCategoryEntity blackWebEntity, BindingResult bindingResult, Model model, Principal principal) {
        blackWebCategoryService.save(blackWebEntity);
        return getResponseModel(Boolean.TRUE);
    }

    @ApiIgnore
    @GetMapping(value = "/delete/{id}")
    public ResponseModel getDeleteInstance(@PathVariable(name = "id") Long id) {

        blackWebCategoryService.delete(id);

        ResponseModel responseModel = new ResponseModel();
        responseModel.setError("");
        responseModel.setSuccess(Boolean.TRUE);
        return responseModel;
    }

    private ResponseModel getResponseModel(Boolean result){
        ResponseModel responseModel = new ResponseModel();
        responseModel.setError("");
        responseModel.setSuccess(result);
        return responseModel;
    }
}
