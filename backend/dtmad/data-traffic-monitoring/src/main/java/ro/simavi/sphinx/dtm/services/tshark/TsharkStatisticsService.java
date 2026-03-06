package ro.simavi.sphinx.dtm.services.tshark;

import ro.simavi.sphinx.dtm.model.event.MetricModel;
import ro.simavi.sphinx.dtm.model.PackageStatistics;

import java.util.HashMap;

/*
The AD component focuses on the near real-time extraction of spatiotemporal statistics,
such as the connectivity between pair of IP-enabled components
(computers, IT devices & Artificial Intelligence or AI Honeypots) available in the core network system.
 */
public interface TsharkStatisticsService {

    void collect(String[] pcapMessage);

    void collect(MetricModel metricModel);

    HashMap<String, HashMap<String, PackageStatistics>> getPackageEthernetStatistics();

    HashMap<String,HashMap<String,PackageStatistics>> getPackageIPv4Statistics();

    HashMap<String,HashMap<String,PackageStatistics>> getPackageUDPStatistics();

    HashMap<String,HashMap<String,PackageStatistics>> getPackageTCPStatistics();

    HashMap<String,HashMap<String,PackageStatistics>> getPackageIPv6Statistics();

    HashMap<String, PackageStatistics> getProtocolStatistics();

    HashMap<String, Long> getUsernameStatistics();

    HashMap<String, HashMap<String, PackageStatistics>> getProtocolByInstanceStatistics();
}
