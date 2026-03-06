package ro.simavi.sphinx.ad.configuration;

import org.apache.spark.SparkConf;
import org.apache.spark.SparkContext;
import org.apache.spark.api.java.JavaSparkContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SparkConfig {

    private static final Logger logger = LoggerFactory.getLogger(SparkConfig.class);

    @Value(value = "${spark.master}")
    private String sparkMaster;

    @Value(value = "${spark.app.name}")
    private String sparkAppName;

    @Bean
    public SparkConf sparkConf() {
        // spark://172.18.252.148:7077 sau 172.18.252.148 = localhost, dar nu merge cu localhost
        if (sparkMaster==null || sparkMaster.startsWith("$")){
            sparkMaster = "local[*]";
        }

        logger.info("**  sparkMaster: " +sparkMaster);
        logger.info("**  sparkAppName " + sparkAppName);

        SparkConf sparkConf = new SparkConf()
                .setMaster(sparkMaster)
                .setAppName(sparkAppName)
                //.setJars(new String[]{"C:\\work\\SPHINX\\anomaly-detection\\target\\anomaly-detection-0.0.1-SNAPSHOT.jar"})
                .set("spark.executor.memory", "1g")
                .set("spark.default.parallelism", "160")
                .set("spark.driver.memory", "1g"); // 160
        logger.info("** Conctare spark");
        return sparkConf;
    }

    @Bean
    public SparkContext sparkContext() {
        return new SparkContext(sparkConf());
    }

    @Bean
    public JavaSparkContext javaSparkContext() {
        //return new JavaSparkContext(sparkConf());
        return new JavaSparkContext(sparkContext());
    }
}
