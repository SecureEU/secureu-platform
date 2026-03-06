package ro.simavi.sphinx.ad.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PropertiesConfig implements CommandLineRunner {

    private final AdConfigProps adConfigProps;

    private static final Logger logger = LoggerFactory.getLogger(PropertiesConfig.class);

    public PropertiesConfig(AdConfigProps adConfigProps){
        this.adConfigProps = adConfigProps;
    }

    @Override
    public void run(String... args) {
        logger.info("-------------AD Properties-----------------");

        logger.info("Name: {}",adConfigProps.getName());
    }

}
