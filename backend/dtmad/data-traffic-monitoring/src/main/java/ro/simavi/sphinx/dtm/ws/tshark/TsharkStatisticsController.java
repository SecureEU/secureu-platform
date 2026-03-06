package ro.simavi.sphinx.dtm.ws.tshark;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ro.simavi.sphinx.dtm.model.PackageStatistics;
import ro.simavi.sphinx.dtm.services.tshark.TsharkStatisticsService;

import java.util.HashMap;

@RestController
@RequestMapping("/tshark/statistics")
public class TsharkStatisticsController {

    private final TsharkStatisticsService statisticsService;

    public TsharkStatisticsController(TsharkStatisticsService statisticsService){
        this.statisticsService = statisticsService;
    }

    @GetMapping(value = "/getPackageEthernetStatistics")
    public HashMap<String, HashMap<String, PackageStatistics>>  getPackageEthernetStatistics() {
        return statisticsService.getPackageEthernetStatistics();
    }

    @GetMapping(value = "/getPackageIPv4Statistics")
    public HashMap<String, HashMap<String, PackageStatistics>>  getPackageIPv4Statistics() {
        return statisticsService.getPackageIPv4Statistics();
    }

    @GetMapping(value = "/getPackageIPv6Statistics")
    public HashMap<String, HashMap<String, PackageStatistics>>  getPackageIPv6Statistics() {
        return statisticsService.getPackageIPv6Statistics();
    }

    @GetMapping(value = "/getPackageTCPStatistics")
    public HashMap<String, HashMap<String, PackageStatistics>>  getPackageTCPStatistics() {
        return statisticsService.getPackageTCPStatistics();
    }

    @GetMapping(value = "/getPackageUDPStatistics")
    public HashMap<String, HashMap<String, PackageStatistics>>  getPackageUDPStatistics() {
        return statisticsService.getPackageUDPStatistics();
    }

    @GetMapping(value = "/getUsernameStatistics")
    public HashMap<String, Long> getUsernameStatistics() {
        return statisticsService.getUsernameStatistics();
    }


    // global/ per all instance
    @GetMapping(value = "/getProtocolByInstanceStatistics")
    public HashMap<String, HashMap<String, PackageStatistics>> getProtocolByInstanceStatistics() {
        return statisticsService.getProtocolByInstanceStatistics();
    }
}
