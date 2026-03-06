package ro.simavi.sphinx.dtm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.MessageSource;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.util.StringUtils;
import ro.simavi.sphinx.dtm.configuration.DTMConfigProps;
import ro.simavi.sphinx.dtm.manager.ToolProcessManager;
import ro.simavi.sphinx.dtm.model.ToolModel;
import ro.simavi.sphinx.dtm.services.DtmConfigService;
import ro.simavi.sphinx.dtm.services.NetworkPersistService;
import ro.simavi.sphinx.dtm.util.DtmTools;
import ro.simavi.sphinx.dtm.util.NetworkHelper;
import ro.simavi.sphinx.dtm.util.PythonHelper;

import java.io.File;
import java.util.HashMap;
import java.util.Locale;

@SpringBootApplication
@EnableScheduling
@Order(1)
public class DTMApplication implements CommandLineRunner {

	@Autowired
	private MessageSource messageSource;

	@Autowired
	private ToolProcessManager toolManager;

	@Autowired
	private DTMConfigProps dtmConfigProps;

	@Autowired
	private Environment environment;

	@Autowired
	private NetworkPersistService networkPersistService;

	@Autowired
	private DtmConfigService dtmConfigService;

	private static final Logger logger = LoggerFactory.getLogger(DTMApplication.class);

	public static void main(String[] args) {
		SpringApplication.run(DTMApplication.class, args);
	}

	@Override
	public void run(String... args) {
		logger.info(messageSource.getMessage("welcome.text", null, new Locale("ro","RO")));

		this.dtmConfigService.getConfigs();

		this.initEnv(args);
		this.displayEnv();

		//testPython(args);

		networkPersistService.clean();

		toolManager.tryToStart();
	}

	private void testPython(String... args){
		// test python
		logger.info("===============test python====[start]================");
		PythonHelper pythonHelper = new PythonHelper();
		String filePath = "feature-test.py";
		for(String arg:args) {
			if (arg.startsWith("-Ddtm.filePathPython")) {
				filePath = arg.split("=")[1];
			}
		}

		try {
			ToolModel toolModel = dtmConfigProps.getToolModelHashMap().get(DtmTools.NFSTREAM.getName());
			logger.info("logger pt source aici " + toolModel.getProperties().get("source"));
			pythonHelper.start(toolModel.getExe(), filePath, toolModel.getProperties().get("source"), toolModel.getProperties().get("csv"), toolModel.getProperties().get("collectFunction"));
		} catch (Exception e) {
			e.printStackTrace();
		}
		logger.info("===============test python===[stop]=================");
	}

	private void displayEnv(){
		for (DtmTools dtmTools : DtmTools.values()) {
			String toolName = dtmTools.getName();
			ToolModel toolModel = dtmConfigProps.getToolModelHashMap().get(toolName);
			if (toolModel!=null){
				logger.info("-D"+toolName+".path=" + toolModel.getPath() );
				logger.info("-D"+toolName+".exe=" + toolModel.getExe() );

				String[] properties = dtmTools.getProperties();
				if (properties!=null && properties.length>0) {
					for (String property : properties) {
						logger.info("-D"+toolName+"."+property+"=" + toolModel.getProperties().get(property) );
					}
				}
			}
		}
		logger.info("-Ddtm.instanceKey=" + dtmConfigProps.getInstanceKey() );

		logger.info("logging.file.name="+environment.getProperty("logging.file.name"));
		logger.info("spring.datasource.url="+environment.getProperty("spring.datasource.url"));

		logger.info("NetworkHelper.getUsername=" + NetworkHelper.getUsername());
		logger.info("NetworkHelper.getHostNameFromEnv=" + NetworkHelper.getHostNameFromEnv());
		logger.info("NetworkHelper.getHostAddress=" + NetworkHelper.getHostAddress());
		logger.info("NetworkHelper.getHostName=" + NetworkHelper.getHostName());

	}

	private void initEnv(String... args){

		String instanceKey = environment.getProperty("dtm.instanceKey");
		if (instanceKey==null){
			instanceKey = NetworkHelper.getInstanceKey();
		}

		// properties
		HashMap<String, ToolModel> toolModelHashMap = new HashMap<>();
		boolean isWindows = File.separator.equals("\\");
		for (DtmTools dtmTools : DtmTools.values()) {
			String toolName = dtmTools.getName();
			ToolModel toolModel = new ToolModel();
			toolModel.setName(toolName);
			String path = environment.getProperty("dtm.tool."+toolName+".path");
			String exe = environment.getProperty("dtm.tool."+toolName+".exe");
			// Windows: never use Unix path for tshark (overrides stale/docker props)
			if (isWindows && DtmTools.TSHARK.getName().equals(toolName) && path != null && path.startsWith("/")) {
				path = "C:/tools/Wireshark";
				exe = (exe != null && !exe.isEmpty() && !exe.startsWith("/")) ? exe : "tshark.exe";
			}
			toolModel.setPath(path);
			toolModel.setExe(exe);

			String[] properties = dtmTools.getProperties();
			if (properties!=null && properties.length>0){
				HashMap<String, String> toolProperties = new HashMap<>();
				for(String property: properties) {
					toolProperties.put(property,environment.getProperty("dtm.tool."+toolName+"."+property) );
				}
				toolModel.setProperties(toolProperties);
			}

			toolModelHashMap.put(toolName, toolModel);
		}

		dtmConfigProps.setToolModelHashMap(toolModelHashMap);

		// override: ENV
		for (DtmTools dtmTools : DtmTools.values()) {
			String toolName = dtmTools.getName();
			String path = System.getenv("dtm.tool."+toolName+".path");
			ToolModel toolModel = dtmConfigProps.getToolModelHashMap().get(toolName);
			if (toolModel==null){
				continue;
			}
			if (!StringUtils.isEmpty(path)){
				toolModel.setPath(path);
			}
			String exe = System.getenv("dtm.tool."+toolName+".exe");
			if (!StringUtils.isEmpty(exe)){
				toolModel.setExe(exe);
			}
			String[] properties = dtmTools.getProperties();
			if (properties!=null && properties.length>0){
				for(String property: properties) {
					String propertyValue = System.getenv("dtm.tool."+toolName+"."+property);
					// property => propertyValue
					if (!StringUtils.isEmpty(propertyValue)){
						toolModel.getProperties().put(property,propertyValue);
					}
				}
			}
		}

		// override: ARG
		for(String arg:args){
			if (arg.startsWith("-Ddtm.instanceKey")){
				instanceKey = arg.split("=")[1];
			}

			for (DtmTools dtmTools : DtmTools.values()) {
				String toolName = dtmTools.getName();
				if (arg.startsWith("-D"+toolName+".path=")){
					String value = arg.split("=")[1];
					ToolModel toolModel = dtmConfigProps.getToolModelHashMap().get(toolName);
					if (toolModel==null){
						toolModel = new ToolModel();
						toolModel.setName(toolName);
					}
					toolModel.setPath(value);
					dtmConfigProps.getToolModelHashMap().put(toolName, toolModel);
				}
			}
		}

		// On Windows, force tshark path after any -Dtshark.path=/usr/bin override
		if (isWindows) {
			ToolModel tsharkModel = dtmConfigProps.getToolModelHashMap().get(DtmTools.TSHARK.getName());
			if (tsharkModel != null && tsharkModel.getPath() != null && tsharkModel.getPath().startsWith("/")) {
				tsharkModel.setPath("C:/tools/Wireshark");
				tsharkModel.setExe(tsharkModel.getExe() != null && !tsharkModel.getExe().isEmpty() && !tsharkModel.getExe().startsWith("/") ? tsharkModel.getExe() : "tshark.exe");
			}
		}

		dtmConfigProps.setInstanceKey(instanceKey);
	}

}
