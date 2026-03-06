package ro.simavi.sphinx.dtm.util;

import org.python.core.Options;
import org.python.core.PyFunction;
import org.python.core.PyObject;
import org.python.core.PyString;
import org.python.util.PythonInterpreter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import ro.simavi.sphinx.dtm.model.ToolModel;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Properties;


public class PythonHelper {

    private static final Logger logger = LoggerFactory.getLogger(PythonHelper.class);

    public void start(ToolModel toolModel) throws Exception {
        start(toolModel.getExe(), toolModel.getProperties().get("pythonFile"), toolModel.getProperties().get("source"), toolModel.getProperties().get("csv"), toolModel.getProperties().get("collectFunction") );
    }

    public void start(ToolModel toolModel, String source) throws Exception {
        start(toolModel.getExe(), toolModel.getProperties().get("pythonFile"), source, toolModel.getProperties().get("csv"), toolModel.getProperties().get("collectFunction") );
    }

    public void start(String exe, final String filePath, String source, String csv, String collectFunction) throws Exception {

        try {
            new Thread(){
                public void run(){

                    String filePath2 = filePath;
                    if (filePath == null || filePath.trim().isEmpty()) {
                        logger.error("Python file path is null or empty! Skipping process start.");
                        return;
                    }
                    if (!new File(filePath).exists()){
                        filePath2 = resolvePythonScriptPath(filePath);
                    }

                    logger.info("==start[Python]==");
                    logger.info("exe="+exe);
                    logger.info("filePath2="+filePath2);
                    logger.info("collectFunction="+collectFunction);
                    logger.info("source="+source);
                    logger.info("csv="+csv);
                    logger.info("=========");

                    ProcessBuilder processBuilder = new ProcessBuilder(exe, filePath2, collectFunction, source, csv);
                    //processBuilder.redirectErrorStream(true);
                    try {
                        Process process = processBuilder.start();

                        StringBuilder inputStreamStringBuilder = new StringBuilder();
                        BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()), 1);
                        String line = null;
                        while ((line = br.readLine()) != null) {
                            inputStreamStringBuilder.append(line);
                            logger.info("[python]:"+line);
                        }

                        // error
                        try (BufferedReader br2 = new BufferedReader(new InputStreamReader(process.getErrorStream()), 1)){
                            StringBuilder stringBuilder = new StringBuilder();
                            while ((line = br2.readLine()) != null) {
                                stringBuilder.append(line);
                            }
                            String error = stringBuilder.toString();
                            if (!StringUtils.isEmpty(error)) {
                                logger.error(error);
                            }
                        } catch(Exception e){
                            e.printStackTrace();
                        }

                        int exitCode = process.waitFor();
                        logger.info(exitCode+"");

                    } catch (IOException e) {
                        logger.error("Error start process Python.... " );
                        e.printStackTrace();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }.start();

        }catch (Exception e){
            logger.error("Error start Python.... " );
        }
    }

    public void startPy(String filePath, String source, String csv, String collectFunction) throws Exception {

        Properties props = new Properties();
        props.put("python.console.encoding", "UTF-8");
        props.put("python.import.site", "false");
        Properties preProps = System.getProperties();
        PythonInterpreter.initialize(preProps, props, new String[0]);

        ScriptEngineManager manager = new ScriptEngineManager();
        Options.importSite = false;
        PythonInterpreter interpreter = new PythonInterpreter();
        ScriptEngine engine = manager.getEngineByName("python");

        if (engine!=null) {
            try {
                try {
                    logger.info("===[python:filePath]==="+filePath);
                    logger.info("===[python:filePath] exists ? ==="+new File(filePath).exists());
                    interpreter.execfile(filePath);
                }catch (Exception e){
                    interpreter.execfile(resolvePythonScriptPath(filePath));
                }

                PyFunction pyFunction = interpreter.get(collectFunction, PyFunction.class);
                PyObject object = pyFunction.__call__(new PyString(source), new PyString(csv));
                System.out.println(object);
                logger.info(object+"");
            } catch (Exception e) {
                e.printStackTrace();
                logger.error(e.getMessage());
            }
        }
    }

    private String resolvePythonScriptPath(String filename) {
        if (filename == null || filename.trim().isEmpty()) return filename;
        File f = new File(filename);
        if (f.isAbsolute()) return f.getAbsolutePath();
        // Try multiple bases depending on run config working dir (module, secureu repo, or SECUR-EU parent)
        String[] bases = {
            "src/test/resources",
            "data-traffic-monitoring/data-traffic-monitoring/src/test/resources",
            "secureu/data-traffic-monitoring/data-traffic-monitoring/src/test/resources"
        };
        for (String base : bases) {
            File file = new File(base, filename);
            if (file.exists()) return file.getAbsolutePath();
        }
        return new File(bases[0], filename).getAbsolutePath();
    }

}
