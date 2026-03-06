package ro.simavi.sphinx.ad.ws;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ro.simavi.sphinx.ad.model.ResponseModel;
import ro.simavi.sphinx.ad.model.simulation.AlgorithmSimulationModel;
import ro.simavi.sphinx.ad.services.SimulationService;
import springfox.documentation.annotations.ApiIgnore;

import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/simulation")
public class SimulationController {

    private static final Logger logger = LoggerFactory.getLogger(SimulationController.class);

    private final SimulationService simulationService;

    public SimulationController(SimulationService simulationService){
        this.simulationService = simulationService;
    }

    @ApiIgnore
    @GetMapping(value = "/getAlgorithmSimulationList")
    public List<AlgorithmSimulationModel> getAlgorithmSimulationList() {
        return simulationService.getAlgorithmSimulationList();
    }

    @ApiIgnore
    @GetMapping(value = "/execute/{file}")
    public ResponseModel execute(@PathVariable(name = "file") String file){
        long startTime = new Date().getTime();
        logger.info("============= SIMULATION: [start] ==============");
        logger.info("============= SIMULATION: [file]="+file+" ==============");
        try {
            Integer countAlert = simulationService.execute(file + ".csv");
            long endTime = new Date().getTime();
            long totalTime = endTime - startTime;
            logger.info("============= SIMULATION: [totalTime]=" + totalTime + " / [file]= " + file + " / [countAlert]=" + countAlert);
            logger.info("============= SIMULATION: [end] ==============");
            return getResponseModel(true, "Nr. alert for " + file + ": " + countAlert + " (" + totalTime + "ms)");
        }catch (Exception e){
            long endTime = new Date().getTime();
            long totalTime = endTime - startTime;
            logger.error("============= SIMULATION: [error]="+e.getMessage());
            e.printStackTrace();
            logger.error("============= SIMULATION: [totalTime]=" + totalTime + " / [file]= " + file );
            logger.error("============= SIMULATION: [end] ==============");
            return getResponseModel(false, "ERROR for " + file + " /  (" + totalTime + "ms)");
        }
    }

    private ResponseModel getResponseModel(Boolean result, String message){
        ResponseModel responseModel = new ResponseModel();
        responseModel.setMessage(message);
        responseModel.setSuccess(result);
        return responseModel;
    }
}
