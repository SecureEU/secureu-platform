package ro.simavi.sphinx.dtm.init;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import ro.simavi.sphinx.dtm.entities.InstanceEntity;
import ro.simavi.sphinx.dtm.jpa.repositories.InstanceRepository;

@Component
@Order(0)
public class DefaultInstanceInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DefaultInstanceInitializer.class);

    private static final String DEFAULT_KEY = "local";

    private final InstanceRepository instanceRepository;

    public DefaultInstanceInitializer(InstanceRepository instanceRepository) {
        this.instanceRepository = instanceRepository;
    }

    @Override
    public void run(String... args) {
        if (instanceRepository.findByKey(DEFAULT_KEY).isPresent()) {
            logger.info("Default DTM instance '{}' already present — skipping seed", DEFAULT_KEY);
            return;
        }

        InstanceEntity entity = new InstanceEntity();
        entity.setName(DEFAULT_KEY);
        entity.setKey(DEFAULT_KEY);
        entity.setDescription("Auto-registered local instance");
        entity.setEnabled(Boolean.TRUE);
        entity.setUrl("http://localhost:8087");
        entity.setMaster(Boolean.TRUE);
        entity.setHasTshark(Boolean.TRUE);
        entity.setHasSuricata(Boolean.TRUE);

        instanceRepository.save(entity);
        logger.info("Seeded default DTM instance '{}'", DEFAULT_KEY);
    }
}
