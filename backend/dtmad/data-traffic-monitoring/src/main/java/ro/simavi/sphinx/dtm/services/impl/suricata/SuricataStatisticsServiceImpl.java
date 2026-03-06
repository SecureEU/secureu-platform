package ro.simavi.sphinx.dtm.services.impl.suricata;

import org.springframework.stereotype.Service;
import ro.simavi.sphinx.dtm.model.event.MetricModel;
import ro.simavi.sphinx.model.event.alert.SphinxModel;
import ro.simavi.sphinx.dtm.services.suricata.SuricataStatisticsService;

import java.util.HashMap;

@Service
public class SuricataStatisticsServiceImpl implements SuricataStatisticsService {

    private HashMap<String,String> decoderHashMap = new HashMap<>();

    private HashMap<String, HashMap<String, String>> decoderPerInstanceHashMap = new HashMap<>();

    public void collect(MetricModel metricModel){
        collectDecoderStatistics(metricModel);
        collectDecoderPerInstanceStatistics(metricModel);
    }

    public HashMap<String,String> getDecoderStatistics(){
        return decoderHashMap;
    }

    public HashMap<String, HashMap<String, String>>  getDecoderPerInstanceStatistics(){
        return decoderPerInstanceHashMap;
    }

    private void collectDecoderStatistics(MetricModel metricModel) {

        SphinxModel sphinx = metricModel.getSphinxModel();
        String tool = sphinx.getTool();
        String component = sphinx.getComponent();

        if (tool.equals("suricata") && component.equals("dtm")) {
            decoderHashMap = getDecoderHashMap(metricModel);
        }

    }

    private HashMap<String,String> getDecoderHashMap(MetricModel metricModel){

        HashMap<String,String> decoderHashMap = new HashMap<>();

        HashMap map = (HashMap) metricModel.getStats().get("decoder");

        String[] fields = new String[]{"pkts","bytes","max_pkt_size","avg_pkt_size","ethernet","tcp","udp","ipv4","ipv6"};

        for(String field:fields){
            decoderHashMap.put(field, map.get(field).toString());
        }

        return decoderHashMap;
    }

    private void collectDecoderPerInstanceStatistics(MetricModel metricModel) {
        SphinxModel sphinx = metricModel.getSphinxModel();
        String tool = sphinx.getTool();
        String component = sphinx.getComponent();

        if (tool.equals("suricata") && component.equals("dtm")) {
            String hostname = metricModel.getHost();
            decoderPerInstanceHashMap.put(hostname, getDecoderHashMap(metricModel));
        }
    }

}
