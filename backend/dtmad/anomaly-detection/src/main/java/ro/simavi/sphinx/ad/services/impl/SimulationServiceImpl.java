package ro.simavi.sphinx.ad.services.impl;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigValueFactory;
import org.apache.commons.io.FileUtils;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.spark.SparkContext;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import ro.simavi.sphinx.ad.SpringContext;
import ro.simavi.sphinx.ad.configuration.AdConfigProps;
import ro.simavi.sphinx.ad.dpi.exporter.DPIExporter;
import ro.simavi.sphinx.ad.dpi.exporter.HBaseDPIExporter;
import ro.simavi.sphinx.ad.kernel.enums.AdTools;
import ro.simavi.sphinx.ad.kernel.hbase.AdmlHBaseRDD;
import ro.simavi.sphinx.ad.model.simulation.AlgorithmSimulationModel;
import ro.simavi.sphinx.ad.model.simulation.SflowTestedAlgorithm;
//import ro.simavi.sphinx.ad.scala.kernel.SFlowMain;
import ro.simavi.sphinx.ad.services.AdConfigService;
import ro.simavi.sphinx.ad.services.MessagingSystemService;
import ro.simavi.sphinx.ad.services.SimulationService;
import ro.simavi.sphinx.util.OperatingSystem;
import ro.simavi.sphinx.util.OperatingSystemHelper;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class SimulationServiceImpl implements SimulationService {

    private static final Logger logger = LoggerFactory.getLogger(SimulationServiceImpl.class);

    private final Environment environment;

    private final AdConfigProps adConfigProps;

    private final SparkContext sparkContext;

    private final JavaSparkContext javaSparkContext;

    private final AdConfigService adConfigService;

    private final MessagingSystemService messagingSystemService;

    private Process logstashProcess = null;

    public SimulationServiceImpl(Environment environment,
                                 AdConfigProps adConfigProps, AdConfigService adConfigService,
                                 SparkContext sparkContext,  JavaSparkContext javaSparkContext,
                                 MessagingSystemService messagingSystemService){
       this.environment=environment;
       this.adConfigProps = adConfigProps;
       this.adConfigService = adConfigService;
       this.sparkContext = sparkContext;
       this.javaSparkContext = javaSparkContext;
       this.messagingSystemService = messagingSystemService;
    }

    @Override
    public List<AlgorithmSimulationModel> getAlgorithmSimulationList() {

        String path = environment.getProperty("ad.adml.simulation.path");

        List<AlgorithmSimulationModel> algorithmSimulationModels = new ArrayList<>();

        FilenameFilter filter = new FilenameFilter() {
            @Override
            public boolean accept(File f, String name) {
                return name.endsWith(".csv");
            }
        };

        if (path == null || path.isEmpty()) {
            return algorithmSimulationModels;
        }
        File f = new File(path);
        if (!f.exists()) {
            if (f.mkdirs()) {
                logger.info("Created simulation path: {}", path);
            }
            return algorithmSimulationModels;
        }
        if (!f.isDirectory()) {
            return algorithmSimulationModels;
        }
        String[] pathnames = f.list(filter);
        if (pathnames == null) {
            return algorithmSimulationModels;
        }
        for (String pathname : pathnames) {
            algorithmSimulationModels.add(new AlgorithmSimulationModel(pathname, pathname));
        }

        return algorithmSimulationModels;
    }

    private boolean startLogstash(String file){

        String simulationPath = environment.getProperty("ad.adml.simulation.path");
        String simulationWorkPath = environment.getProperty("ad.adml.simulation.path")+File.separator + "work";
        String logstashPath = environment.getProperty("ad.adml.simulation.tool.logstash.path");

        // 1. Stop logstash [logstash]
        stopProcess();

        // 3. Delete "work" folder
        File simulationWorkFile = new File(simulationWorkPath);
        if (simulationWorkFile.exists()){
            try {
                FileUtils.cleanDirectory(simulationWorkFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }else{
            try {
                FileUtils.forceMkdir(simulationWorkFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        // 4. Copy file to "work"
        long sflowNumbers = 0;
        File fileCSV = new File(simulationPath + File.separator + file);
        if (!fileCSV.exists()){
            return false;
        }else{
            try {
                String fileName = simulationPath + File.separator+"work"+File.separator + file;
                FileUtils.copyFile(fileCSV, new File(fileName));
                sflowNumbers = Files.lines(Paths.get(fileName)).count()-1;
            } catch (IOException e) {
                logger.error(e.getMessage());
            }
        }

        // 5. Start logstash with file : logstash -f [simulationPath]\logstashNf-folder.conf
        // WORK_PATH: input.file.path = simulationWorkPath
        // SINCEDB_PATH = simulationWorkPath

        boolean startOk = startLogstash(simulationPath, logstashPath);

        //6. wait logstash
        try {
            JavaPairRDD javaPairRDD = AdmlHBaseRDD.getInstance().connectSFlow(javaSparkContext);
            long count = 0;
            while (count<sflowNumbers - 10){ // 10 - marja de eroare
                count = javaPairRDD.count();
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    logger.error(e.getMessage());
                }
            }
        } catch (IOException e) {
            logger.error(e.getMessage());
        }

        return  startOk;
    }

    public void initHbase(){
        // horizontal port scan =================================================================================
//        HIST07-10.1.1.11     column=info:name, timestamp=1619848271245, value=HIST07-10.1.1.11
//        HIST07-10.1.1.11     column=info:size, timestamp=1619848271245, value=3697
//        HIST07-10.1.1.11     column=values:1158, timestamp=1619848271245, value=516.0
//        HIST07-10.1.1.11     column=values:1159, timestamp=1619848271245, value=408.0 [fara]
//        HIST07-10.1.1.11     column=values:1160, timestamp=1619848271245, value=407.0 [fara]

        Table table = AdmlHBaseRDD.getInstance().getAdmlHistograms();

        String id = String.valueOf("HIST07-10.1.1.11");
        Put putFlow = new Put(Bytes.toBytes(id));

        putFlow.addColumn(Bytes.toBytes("info"), Bytes.toBytes("name"),Bytes.toBytes("HIST07-10.1.1.11"));
        putFlow.addColumn(Bytes.toBytes("info"), Bytes.toBytes("size"),Bytes.toBytes("3697"));
        putFlow.addColumn(Bytes.toBytes("values"), Bytes.toBytes("1158"),Bytes.toBytes("516"));

        try {
            table.put(putFlow);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // vertical port scan=============================================================================================
//        HIST08-10.1.1.11     column=info:name, timestamp=1619879552302, value=HIST08-10
//        .1.1.11
//        HIST08-10.1.1.11     column=info:size, timestamp=1619879552302, value=3
//        HIST08-10.1.1.11     column=values:10, timestamp=1619879552302, value=1.0
//        HIST08-10.1.1.11     column=values:18, timestamp=1619879552302, value=0.1666666
//        6666666666
//        HIST08-10.1.1.11     column=values:22, timestamp=1619879552302, value=0.3333333
//        333333333

        id = String.valueOf("HIST08-10.1.1.11");
        Put putFlow2 = new Put(Bytes.toBytes(id));

        putFlow2.addColumn(Bytes.toBytes("info"), Bytes.toBytes("name"),Bytes.toBytes("HIST08-10.1.1.11"));
        putFlow2.addColumn(Bytes.toBytes("info"), Bytes.toBytes("size"),Bytes.toBytes("10"));
        putFlow2.addColumn(Bytes.toBytes("values"), Bytes.toBytes("10"),Bytes.toBytes("0.10"));
        putFlow2.addColumn(Bytes.toBytes("values"), Bytes.toBytes("12"),Bytes.toBytes("0.10"));
        putFlow2.addColumn(Bytes.toBytes("values"), Bytes.toBytes("14"),Bytes.toBytes("0.10"));
        putFlow2.addColumn(Bytes.toBytes("values"), Bytes.toBytes("9"),Bytes.toBytes("0.10"));
        putFlow2.addColumn(Bytes.toBytes("values"), Bytes.toBytes("4"),Bytes.toBytes("0.10"));
        putFlow2.addColumn(Bytes.toBytes("values"), Bytes.toBytes("5"),Bytes.toBytes("0.10"));
        putFlow2.addColumn(Bytes.toBytes("values"), Bytes.toBytes("6"),Bytes.toBytes("0.10"));
        putFlow2.addColumn(Bytes.toBytes("values"), Bytes.toBytes("7"),Bytes.toBytes("0.10"));
        putFlow2.addColumn(Bytes.toBytes("values"), Bytes.toBytes("8"),Bytes.toBytes("0.10"));
        putFlow2.addColumn(Bytes.toBytes("values"), Bytes.toBytes("9"),Bytes.toBytes("0.10"));
        putFlow2.addColumn(Bytes.toBytes("values"), Bytes.toBytes("15"),Bytes.toBytes("0.10"));

        try {
            table.put(putFlow2);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Atypical alien TCP port used =================================================================
//        HIST02-10.1.1.10     column=info:name, timestamp=1620122561021, value=HIST02-10.1.1.10
//        HIST02-10.1.1.10     column=info:size, timestamp=1620122561021, value=308
//        HIST02-10.1.1.10     column=values:1158, timestamp=1620122561021, value=1.0

//        HIST02-10.1.1.11     column=info:name, timestamp=1620122585003, value=HIST02-10.1.1.11
//        HIST02-10.1.1.11     column=info:size, timestamp=1620122585003, value=308
//        HIST02-10.1.1.11     column=values:1158, timestamp=1620122585003, value=1.0

        id = String.valueOf("HIST02-10.1.1.11");
        Put putFlow3 = new Put(Bytes.toBytes(id));

        putFlow3.addColumn(Bytes.toBytes("info"), Bytes.toBytes("name"),Bytes.toBytes("HIST02-10.1.1.11"));
        putFlow3.addColumn(Bytes.toBytes("info"), Bytes.toBytes("size"),Bytes.toBytes("1308"));
        putFlow3.addColumn(Bytes.toBytes("values"), Bytes.toBytes("1158"),Bytes.toBytes("1.0"));

        try {
            table.put(putFlow3);
        } catch (IOException e) {
            e.printStackTrace();
        }

//        HIST02.1-10.1.1.11   column=info:name, timestamp=1620123617430, value=HIST02.1-10.1.1.11
//        HIST02.1-10.1.1.11   column=info:size, timestamp=1620123617430, value=308
//        HIST02.1-10.1.1.11   column=values:1158, timestamp=1620123617430, value=1.0

        id = String.valueOf("HIST02.1-10.1.1.11");
        Put putFlow4 = new Put(Bytes.toBytes(id));

        putFlow4.addColumn(Bytes.toBytes("info"), Bytes.toBytes("name"),Bytes.toBytes("HIST02.1-10.1.1.11"));
        putFlow4.addColumn(Bytes.toBytes("info"), Bytes.toBytes("size"),Bytes.toBytes("308"));
        putFlow4.addColumn(Bytes.toBytes("values"), Bytes.toBytes("1159"),Bytes.toBytes("1.0"));

        try {
            table.put(putFlow4);
        } catch (IOException e) {
            e.printStackTrace();
        }

        //        HIST05-104.143.5   column=info:name, timestamp=1620123617430, value=HIST05-104.143.5
        //        HIST05-104.143.5   column=info:size, timestamp=1620123617430, value=308
        //        HIST05-104.143.5   column=values:1158, timestamp=1620123617430, value=1.0

        id = String.valueOf("HIST05-104.143.5");
        Put putFlow5 = new Put(Bytes.toBytes(id));

        putFlow5.addColumn(Bytes.toBytes("info"), Bytes.toBytes("name"),Bytes.toBytes("HIST05-104.143.5"));
        putFlow5.addColumn(Bytes.toBytes("info"), Bytes.toBytes("size"),Bytes.toBytes("308"));
        putFlow5.addColumn(Bytes.toBytes("values"), Bytes.toBytes("1158"),Bytes.toBytes("1.0"));

        try {
            table.put(putFlow5);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Atypical TCP port used =====================================================================
//        HIST01-10.1.1.12     column=info:name, timestamp=1620129281290, value=HIST01-10.1.1.12
//        HIST01-10.1.1.12     column=info:size, timestamp=1620129281290, value=308

        id = String.valueOf("HIST01-10.1.1.500");
        Put putFlow6 = new Put(Bytes.toBytes(id));

        putFlow6.addColumn(Bytes.toBytes("info"), Bytes.toBytes("name"),Bytes.toBytes("HIST01-10.1.1.500"));
        putFlow6.addColumn(Bytes.toBytes("info"), Bytes.toBytes("size"),Bytes.toBytes("308"));
        putFlow6.addColumn(Bytes.toBytes("values"), Bytes.toBytes("1158"),Bytes.toBytes("1.0"));

        try {
            table.put(putFlow6);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Atypical amount of data transfered
//        HIST04-10.1.1.17     column=info:name, timestamp=1620191959353, value=HIST04-10.1.1.17
//        HIST04-10.1.1.17     column=info:size, timestamp=1620191959353, value=1
//        HIST04-10.1.1.17     column=values:14.0, timestamp=1620191959353, value=1.0

        id = String.valueOf("HIST04-10.1.1.17");
        Put putFlow7 = new Put(Bytes.toBytes(id));

        putFlow7.addColumn(Bytes.toBytes("info"), Bytes.toBytes("name"),Bytes.toBytes("HIST04-10.1.1.17"));
        putFlow7.addColumn(Bytes.toBytes("info"), Bytes.toBytes("size"),Bytes.toBytes("30"));
        putFlow7.addColumn(Bytes.toBytes("values"), Bytes.toBytes("14.0"),Bytes.toBytes("0.00000000005"));

        try {
            table.put(putFlow7);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Atypical number of pairs in the period
//        HIST03-10.1.1.10     column=info:name, timestamp=1620195018868, value=HIST03-10.1.1.10
//        HIST03-10.1.1.10     column=info:size, timestamp=1620195018868, value=1
//        HIST03-10.1.1.10     column=values:13.0, timestamp=1620195018868, value=1.0
//        HIST03-10.1.1.11     column=info:name, timestamp=1620194991802, value=HIST03-10.1.1.11
//        HIST03-10.1.1.11     column=info:size, timestamp=1620194991802, value=1
//        HIST03-10.1.1.11     column=values:12.0, timestamp=1620194991802, value=1.0

        id = String.valueOf("HIST03-10.1.1.10");
        Put putFlow8 = new Put(Bytes.toBytes(id));

        putFlow8.addColumn(Bytes.toBytes("info"), Bytes.toBytes("name"),Bytes.toBytes("HIST03-10.1.1.10"));
        putFlow8.addColumn(Bytes.toBytes("info"), Bytes.toBytes("size"),Bytes.toBytes("10"));
        putFlow8.addColumn(Bytes.toBytes("values"), Bytes.toBytes("13.0"),Bytes.toBytes("0.00000000005"));

        try {
            table.put(putFlow8);
        } catch (IOException e) {
            e.printStackTrace();
        }

        id = String.valueOf("HIST03-10.1.1.11");
        Put putFlow9 = new Put(Bytes.toBytes(id));

        putFlow9.addColumn(Bytes.toBytes("info"), Bytes.toBytes("name"),Bytes.toBytes("HIST03-10.1.1.11"));
        putFlow9.addColumn(Bytes.toBytes("info"), Bytes.toBytes("size"),Bytes.toBytes("10"));
        putFlow9.addColumn(Bytes.toBytes("values"), Bytes.toBytes("12.0"),Bytes.toBytes("0.00000000005"));

        try {
            table.put(putFlow9);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    public Integer execute(String file) {

        Environment environment = SpringContext.getBean(Environment.class);
        String hbaseZookeeperQuorum = environment.getProperty("hbase.zookeeper.quorum");
        String clientPortString = environment.getProperty("hbase.zookeeper.property.clientPort");
        Integer hbaseZookeeperClientPort = 2181;
        try{
            hbaseZookeeperClientPort = Integer.parseInt(clientPortString);
        }catch (Exception e){

        }

        String logstashEnabled = environment.getProperty("ad.adml.simulation.tool.logstash.enabled");
        Boolean withLogstash = logstashEnabled!=null && logstashEnabled.equals("true");

        // 0. clear hbase
        AdmlHBaseRDD admlHBaseRDD = AdmlHBaseRDD.getInstance();
        if (!admlHBaseRDD.isOk()) {
            logger.error("Hbase clear!");
            return 0;
        }
        //  sau din hbase shell:
        // truncate 'adml_sflows'
        admlHBaseRDD.truncateAdmlFlows("adml_sflows");
        admlHBaseRDD.truncateAdmlFlows("adml_histograms");

        initHbase();

        boolean withSleep = file.startsWith("test_MediaStreamingClient");

        boolean ok = false;
        if (withLogstash){
            logger.info("=====================Start (startLogstash):");
            long start = new Date().getTime();
            ok = startLogstash(file);
            long end = new Date().getTime();
            logger.info("=====================Duration (startLogstash):"+(end-start));
        }else{
            logger.info("=====================Start (populateHBaseFromCSV):");
            long start = new Date().getTime();
            ok = populateHBaseFromCSV(file, withSleep);
            long end = new Date().getTime();
            logger.info("=====================Duration (populateHBaseFromCSV):"+(end-start));
        }
        if (!ok){
            return 0;
        }

        // 7. Run Scala:
        Map<String, Object> map = adConfigProps.getMap(AdTools.SFLOW);
        Config config = adConfigService.getMergeConfig(map);

        config = overrideConfig(file, config);

        //SFlowMain sFlowMain = new SFlowMain();
        //int count = sFlowMain.run(config, sparkContext, messagingSystemService,hbaseZookeeperQuorum, hbaseZookeeperClientPort );
        int count = 0;
        return count ;
    }

    private Config overrideConfig(String file, Config config){

        Config newConfig = config;

        for (SflowTestedAlgorithm sflowTestedAlgorithm : SflowTestedAlgorithm.values()) {
            String property = sflowTestedAlgorithm.getName() + ".disabled";
            Boolean tested = sflowTestedAlgorithm.isTested();

            String prefix = sflowTestedAlgorithm.getPrefix();
            if (prefix!=null && file.startsWith(prefix)){
                tested = false;
            }
            newConfig = newConfig.withValue(property, ConfigValueFactory.fromAnyRef(tested?1:0));

        }

        return newConfig;
    }

    private boolean populateHBaseFromCSV(String file, boolean withSleep) {
        String simulationPath = environment.getProperty("ad.adml.simulation.path");
        File fileCSV = new File(simulationPath + File.separator + file);
        if (!fileCSV.exists()) {
            return false;
        }else{

            DPIExporter dpiExporter = new HBaseDPIExporter();
            BufferedReader reader;
            int count = 0;
            try {
                reader = new BufferedReader(new FileReader(fileCSV));
                String line = reader.readLine();
                while (line != null) {
                    if (count>0) {
                    //    String[] csvLine = line.split(",", -1);
                        String[] csvLine = line.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)");
                        dpiExporter.saveSflow(csvLine);

                        if (withSleep) {
                            try {
                                Thread.sleep(100);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }

                    // read next line
                    line = reader.readLine();
                    count++;
                }
                reader.close();
                logger.info("=====================Count (populateHBaseFromCSV):"+(count));
            } catch (IOException e) {
                e.printStackTrace();
            }

            return true;
        }
    }

    private boolean startLogstash(String simulationPath, String logstashPath){
        try {
          //  new Thread(){
          //      public void run(){
                   return startProcess(simulationPath, logstashPath);
        //        }
        //    }.start();

        }catch (Exception e){
            logger.error("Error start Logstash !!!");
        }
        return false;
    }

    private boolean startProcess(String simulationPath, String logstashPath) {

        ProcessBuilder pb = new ProcessBuilder(getCommandAndArgs(logstashPath, simulationPath));
        Map<String, String> env = pb.environment();
        String workFolder = (simulationPath+"\\work").replaceAll("\\\\","/");
        env.put("WORK_PATH", workFolder.concat("/*.csv"));
        env.put("SINCEDB_PATH", workFolder.concat("/since-nfstream.db"));

        try {
            logstashProcess = pb.start();

            StringBuilder inputStreamStringBuilder = new StringBuilder();
            BufferedReader br = new BufferedReader(new InputStreamReader(logstashProcess.getInputStream()), 1);
            String line = null;
            boolean finish = false;
            while (finish==false && (line = br.readLine()) != null) {
                inputStreamStringBuilder.append(line);
                finish = line.contains("Successfully started Logstash");
                logger.info("[logstash]:"+line);
            }
            if (finish){
                return true;
            }
            String info = inputStreamStringBuilder.toString();
            if (info!=null && info.contains("[FATAL]")){
                stopProcess();
                return false;
            }
        } catch (IOException e) {
            logger.error(e.getMessage());
            return false;
        }
        return true;
    }

    public void stopProcess() {
        if (logstashProcess!=null) {
            try {
                logstashProcess.destroy();
                logstashProcess.waitFor(2, TimeUnit.SECONDS);
                if(logstashProcess.isAlive()) {
                    logstashProcess.destroyForcibly();
                    logstashProcess.waitFor(2, TimeUnit.SECONDS);
                }
            } catch (InterruptedException e) {
                logger.error(e.getMessage());
            }
        }
    }

    private  List<String>  getCommandAndArgs(String logstashPath, String simulationPath) {
        OperatingSystem operatingSystem  = OperatingSystemHelper.getOperatingSystem();

        // cmd /c  C:\tools\logstash\bin\logstash -f C:\tools\logstash\config\logstash.conf
        // delete .lock file from /data directory if fatal error
        List<String> commandAndArgs = new ArrayList<>();

        String command =logstashPath + File.separator + "bin" + File.separator + "logstash";
        /*todo : test on linux/unix */
        if (operatingSystem.equals(OperatingSystem.LINUX)) {
            commandAndArgs.add("bash"); // "/bin/sh" /*todo*/ "bash", "-c" / or /bin/bash
            commandAndArgs.add("-c");
        }else{
            commandAndArgs.add("cmd");
            commandAndArgs.add("/c");
        }
        commandAndArgs.add(command);
        commandAndArgs.add("-f");

        commandAndArgs.add(simulationPath + File.separator + "logstashNf-folder.conf");

        commandAndArgs.add("--path.data");
        commandAndArgs.add(simulationPath+File.separator+"data"+File.separator+"data-nfstream-"+new Date().getTime());

        return commandAndArgs;
    }


}
