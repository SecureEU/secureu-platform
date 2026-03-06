package ro.simavi.sphinx.ad.dpi.test;

import ro.simavi.sphinx.ad.dpi.exporter.FileDPIExporter;
import ro.simavi.sphinx.ad.dpi.exporter.HBaseDPIExporter;
import ro.simavi.sphinx.ad.dpi.handler.FileDPIHandler;
import ro.simavi.sphinx.ad.dpi.util.FileFlow;
import ro.simavi.sphinx.ad.dpi.util.RandomMerge;

import java.io.File;

public class DataSetUtil {

//    private static HashMap<String, DataSet> databaseMap = null;

//    public static HashMap<String, DataSet> getDatabaseMap(){
//        if (databaseMap==null){
//            databaseMap.put(CTU_Normal_4_only_DNS.getName(), CTU_Normal_4_only_DNS);
//            databaseMap.put(CTU_IoT_Malware_Capture_7_1.getName(), CTU_IoT_Malware_Capture_7_1);
//            databaseMap.put(CTU_IoT_Malware_Capture_35_1.getName(), CTU_IoT_Malware_Capture_35_1);
//            databaseMap.put(CTU_Malware_Capture_Botnet_45.getName(), CTU_Malware_Capture_Botnet_45);
//            databaseMap.put(CICDDoS2019.getName(), CICDDoS2019);
//            databaseMap.put(MaliciousDoH_dns2tcp_Pcaps.getName(), MaliciousDoH_dns2tcp_Pcaps);
//            databaseMap.put(MaliciousDoH_dnscat2_Pcaps.getName(), MaliciousDoH_dnscat2_Pcaps);
//        }
//        return databaseMap;
//    }

    public static String getResultFolderPath(DataSet dataSet){

        String folder =  DataSet.FOLDER.getName();
        String suricataResultFolder = folder + File.separator + dataSet.getName() + File.separator + dataSet.getDataSetType().getFolder() +  File.separator;
        return suricataResultFolder;
    }

    public static void exportToAdmlEve(String importFileName, String exportFileName, Boolean malware){
        FileDPIHandler dpiFileSource = new FileDPIHandler(importFileName, new FileDPIExporter(exportFileName, malware));
        dpiFileSource.execute();
    }

    public static void exportToAdmlEve(DataSet dataSet){
        exportToAdmlEve(dataSet, Boolean.FALSE);
    }

    public static void exportToAdmlEve(DataSet dataSet, Boolean malware){
        if (malware){
            exportToAdmlMalwareEve(dataSet);
        }else{
            exportToAdmlNormalEve(dataSet);
        }
    }

    private static void exportToAdmlNormalEve(DataSet dataSet){
        String suricataResultFolder = DataSetUtil.getResultFolderPath(dataSet) ;
        String importFileName = suricataResultFolder + dataSet.getDataSetType().getDataSetResult().getInput();
        String exportFileName = suricataResultFolder + dataSet.getDataSetType().getDataSetResult().getOutput();

        FileDPIHandler dpiFileSource = new FileDPIHandler(importFileName, new FileDPIExporter(exportFileName, Boolean.FALSE));
        dpiFileSource.execute();
    }

    private static void exportToAdmlMalwareEve(DataSet dataSet){
        String suricataResultFolder = DataSetUtil.getResultFolderPath(dataSet) ;
        String importFileName = suricataResultFolder + dataSet.getDataSetType().getDataSetResult().getMalwareInput();
        String exportFileName = suricataResultFolder + dataSet.getDataSetType().getDataSetResult().getMalwareOutput();

        FileDPIHandler dpiFileSource = new FileDPIHandler(importFileName, new FileDPIExporter(exportFileName, Boolean.TRUE));
        dpiFileSource.execute();
    }

    public static void merge(DataSet normalDataSet, DataSet malwareDataSet, DataSet mergeDataSet) {

        DataSetUtil.exportToAdmlNormalEve(normalDataSet);//normal
        DataSetUtil.exportToAdmlMalwareEve(malwareDataSet);//malware

        mergeFlows(normalDataSet,malwareDataSet,mergeDataSet );
    }

    public static void mergeFlows(DataSet normalDataSet, DataSet malwareDataSet, DataSet mergeDataSet) {

        String normalSuricataResultFolder = DataSetUtil.getResultFolderPath(normalDataSet) ;
        String malwareSuricataResultFolder = DataSetUtil.getResultFolderPath(malwareDataSet) ;
        String mergeSuricataResultFolder = DataSetUtil.getResultFolderPath(mergeDataSet) ;

        String exportNormalFileName =  normalSuricataResultFolder + mergeDataSet.getDataSetType().getDataSetResult().getOutput();
        String exportAlertFileName = malwareSuricataResultFolder + mergeDataSet.getDataSetType().getDataSetResult().getMalwareOutput();
        String exportRandomMergeFileName =  mergeSuricataResultFolder + mergeDataSet.getDataSetType().getDataSetResult().getMergeOutput();

        RandomMerge randomMerge = new RandomMerge(exportNormalFileName, exportAlertFileName, exportRandomMergeFileName);
        randomMerge.execute();
    }

    public static void saveMergeFlowsToHBase(DataSet mergeDataSet) {
        String mergeSuricataResultFolder = DataSetUtil.getResultFolderPath(mergeDataSet) ;
        String mergeFileName =  mergeSuricataResultFolder + mergeDataSet.getDataSetType().getDataSetResult().getMergeOutput();

        FileFlow fileFlow = new FileFlow(mergeFileName, new HBaseDPIExporter());
        fileFlow.execute();

    }
}
