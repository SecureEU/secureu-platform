package ro.simavi.sphinx.ad.ws.grafana;

import org.springframework.web.bind.annotation.*;
import ro.simavi.sphinx.ad.services.AdConfigService;
import ro.simavi.sphinx.model.ConfigModel;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class AdAlgorithmController {

    private final AdConfigService configService;

    public AdAlgorithmController(AdConfigService configService) {
        this.configService = configService;
    }

    @GetMapping("/algorithms")
    public Map<String, ConfigModel> getAlgorithms() {
        return configService.getAlgorithmList();
    }
}
