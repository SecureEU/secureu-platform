package ro.simavi.sphinx.dtm.ws;

import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ro.simavi.sphinx.dtm.entities.BlackWebEntity;
import ro.simavi.sphinx.dtm.model.ResponseModel;
import ro.simavi.sphinx.dtm.services.BlackWebService;
import ro.simavi.sphinx.dtm.services.InstanceService;
import ro.simavi.sphinx.dtm.services.alert.BlackWebAlertService;
import ro.simavi.sphinx.dtm.services.impl.ToolRemoteServiceImpl;
import springfox.documentation.annotations.ApiIgnore;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/blackweb")
public class BlackWebController extends CacheableController{

    private final BlackWebService blackWebService;

    private final BlackWebAlertService blackWebAlertService;

    public BlackWebController(BlackWebService blackWebService,
                              BlackWebAlertService blackWebAlertService,
                              InstanceService instanceService,
                              ToolRemoteServiceImpl toolRemoteService){
        super(instanceService, toolRemoteService);
        this.blackWebService=blackWebService;
        this.blackWebAlertService = blackWebAlertService;
    }

    @ApiIgnore
    @GetMapping(value = "/list/{categoryId}")
    public List<BlackWebEntity> getList(@PathVariable(name = "categoryId") Long categoryId) {
        return blackWebService.getList(categoryId);
    }

    @ApiIgnore
    @GetMapping(value = "/{id}")
    public BlackWebEntity getById(@PathVariable(name = "id") Long id) {
        return blackWebService.findById(id);
    }

    @ApiIgnore
    @PostMapping("/save")
    private ResponseModel save(@RequestBody @Valid BlackWebEntity blackWebEntity, BindingResult bindingResult, Model model, Principal principal) {
        blackWebService.save(blackWebEntity);
        blackWebAlertService.clearCache();

        clearCacheForAllInstances("blackweb");
        return getResponseModel(Boolean.TRUE);
    }

    @ApiIgnore
    @GetMapping(value = "/delete/{id}")
    public ResponseModel getDeleteInstance(@PathVariable(name = "id") Long id) {

        blackWebService.delete(id);
        blackWebAlertService.clearCache();
        clearCacheForAllInstances("blackweb");

        return getResponseModel(Boolean.TRUE);
    }

    @ApiIgnore
    @PostMapping(value = "/upload/{categoryId}")
    public ResponseModel uploadNewFile(@NotNull @RequestParam("file") MultipartFile multipartFile, @PathVariable(name = "categoryId") Long categoryId) throws IOException {

        BufferedReader reader = new BufferedReader(new InputStreamReader(multipartFile.getInputStream()));

        List<BlackWebEntity> blackWebEntities = new ArrayList<>();
        try {
            String line = reader.readLine();
            while (line != null) {

                BlackWebEntity blackWebEntity = new BlackWebEntity();
                blackWebEntity.setDomain(line);
                blackWebEntities.add(blackWebEntity);

                line = reader.readLine();

            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        blackWebService.importFromFile(blackWebEntities, categoryId);

        blackWebAlertService.clearCache();

        clearCacheForAllInstances("blackweb");

        return getResponseModel(Boolean.TRUE);
    }

    @ApiIgnore
    @GetMapping(value = "/clearCache")
    public void clearCache() {
        blackWebAlertService.clearCache();
    }

    private ResponseModel getResponseModel(Boolean result){
        ResponseModel responseModel = new ResponseModel();
        responseModel.setError("");
        responseModel.setSuccess(result);
        return responseModel;
    }
}
