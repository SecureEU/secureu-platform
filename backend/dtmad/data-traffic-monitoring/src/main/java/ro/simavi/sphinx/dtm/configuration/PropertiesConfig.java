package ro.simavi.sphinx.dtm.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import ro.simavi.sphinx.dtm.model.ToolModel;
import ro.simavi.sphinx.dtm.util.DtmTools;

@Configuration
public class PropertiesConfig implements CommandLineRunner {

    private final DTMConfigProps dtmConfigProps;

    private static final Logger logger = LoggerFactory.getLogger(PropertiesConfig.class);

    public PropertiesConfig(DTMConfigProps dtmConfigProps){
        this.dtmConfigProps = dtmConfigProps;
    }

    @Override
    public void run(String... args) {
        logger.info("-------------DTM Properties-----------------");

        ToolModel toolModel = dtmConfigProps.getToolModelHashMap().get(DtmTools.TSHARK.getName());
        if (toolModel!=null) {
            String path = toolModel.getPath();
            logger.info("tshark path: {}", path);
        }
        logger.info("Name: {}", dtmConfigProps.getName());
    }

}
