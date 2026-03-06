package ro.simavi.sphinx.ad;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.MessageSource;
import org.springframework.scheduling.annotation.EnableScheduling;
import ro.simavi.sphinx.ad.services.AdConfigPropsService;
import ro.simavi.sphinx.ad.services.ReputationService;

import java.util.Locale;

@SpringBootApplication
@EnableScheduling
public class ADApplication implements CommandLineRunner {

	@Autowired
	private MessageSource messageSource;

	@Autowired
	private AdConfigPropsService adConfigPropsService;

	@Autowired
	private ReputationService reputationService;

	private static final Logger logger = LoggerFactory.getLogger(ADApplication.class);

	public static void main(String[] args) {
		// So Spark/Hadoop find winutils on Windows; use HADOOP_HOME env or default
		String hadoopHome = System.getenv("HADOOP_HOME");
		if (hadoopHome == null || hadoopHome.isEmpty()) {
			hadoopHome = System.getProperty("hadoop.home.dir", "C:\\tools\\hadoop");
		}
		System.setProperty("hadoop.home.dir", hadoopHome);
		SpringApplication.run(ADApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
		logger.info(messageSource.getMessage("welcome.text", null, new Locale("ro","RO")));

		adConfigPropsService.init();
		adConfigPropsService.display();

		reputationService.update();

	}
}
