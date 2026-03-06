package ro.simavi.sphinx.ad.services.impl;

import org.springframework.stereotype.Service;
import ro.simavi.sphinx.ad.services.StatisticsService;

@Service
public class StatisticsServiceImpl implements StatisticsService {

//    private HashMap<String,Long> dtmTraficListMap = new HashMap<>();
//
//    private List<Object[]> dtmTraficList = new ArrayList<>();
//
//    private HashMap<String,Long> usernameTraffic = new HashMap<>();
//
//    @Override
//    public void collect(String[] pcapMessage) {
//        collectStatistics(pcapMessage, "Wi-fi", dtmTraficListMap);
//        collectUsernameStatistics(pcapMessage, usernameTraffic);
//    }
//
//    private void collectUsernameStatistics(String[] pcapMessage, HashMap<String,Long> usernameTraffic) {
//
//        long len = Long.parseLong(pcapMessage[PackageFields.FRAME_CAP_LEN]);
//
//        String username = pcapMessage[PackageFields.USERNAME];
//        String hostname = pcapMessage[PackageFields.HOSTNAME];
//
//        String uh = username + " @ " + hostname;
//
//        Long bytes = usernameTraffic.get(uh);
//        if (bytes == null){
//            usernameTraffic.put(uh, len);
//        }else{
//            usernameTraffic.put(uh, len+bytes);
//        }
//
//    }
//
//    private void collectStatistics(String[] pcapMessage, String interfaceName, HashMap<String,Long> dtmTraficListMap) {
//        Long packets = dtmTraficListMap.get(interfaceName);
//        if (packets==null){
//            dtmTraficListMap.put(interfaceName,0L);
//        }else{
//            dtmTraficListMap.put(interfaceName,packets + 1 );
//        }
//    }
//
//    @Override
//    public List<Object[]> getDTMTraffic() {
//
//        Long lastPachets = 0L;
//        Long packets = dtmTraficListMap.get("Wi-fi");
//        if (packets==null){
//            packets = 0L;
//        }
//
//        if (dtmTraficList.size()>0){
//            lastPachets = (Long)dtmTraficList.get(dtmTraficList.size()-1)[2];
//        }
//        Object[] datapoint = new Object[3];
//        datapoint[0] = dtmTraficList.size()>0?packets - lastPachets:0;
//        datapoint[1] = new Date().getTime();
//        datapoint[2] = packets;
//
//        dtmTraficList.add(datapoint);
//
//        return dtmTraficList;
//    }
//
//    @Override
//    public HashMap<String, Long> getUsernameStatistics() {
//        return usernameTraffic;
//    }
}
