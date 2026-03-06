package ro.simavi.sphinx.dtm.configuration;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.config.SslConfigs;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.io.Resource;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.util.StringUtils;
import ro.simavi.sphinx.dtm.model.AssetModel;
import ro.simavi.sphinx.dtm.model.event.MetricModel;
import ro.simavi.sphinx.model.event.AlertEventModel;
import ro.simavi.sphinx.model.event.DtmCmdEventModel;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

@Configuration
@DependsOn({"createSmModel"})
public class KafkaProducerConfig {

    @Value(value = "${spring.kafka.bootstrap-servers}")
    private String bootstrapAddress;

    private static final Logger logger = LoggerFactory.getLogger(KafkaProducerConfig.class);

    @Value(value = "${spring.kafka.clientId:#{null}}")
    private String clientId;

    @Value(value = "${spring.kafka.properties.sasl.jaas.config:#{null}}")
    private String jaasConfig;

    @Value(value = "${spring.kafka.properties.security.protocol:#{null}}")
    private String securityProtocol;

    @Value(value = "${spring.kafka.properties.sasl.mechanism:#{null}}")
    private String saslMechanism;

    @Value(value = "${spring.kafka.ssl.trust-store-location:#{null}}")
    private String sslTrustStoreLocation;

    @Value(value = "${spring.kafka.ssl.trust-store-password:#{null}}")
    private String sslTrustStorePassword;

    @Value(value = "${spring.kafka.properties.sasl.login.callback.handler.class:#{null}}")
    private String saslLoginCallbackHandlerClass;

    @Value("classpath:sphinx.jks")
    private Resource resourceFile;

    //@Value(value="${spring.security .oauth2.resourceserver.opaquetoken.client-secret:#{null}}")
    //private String clientSecret;

    private Map<String, Object> getCommonProperties() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapAddress);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);

        //logger.info("===spring.security.oauth2.resourceserver.opaquetoken.client-secret="+clientSecret);

        if (!StringUtils.isEmpty(clientId)){
        //    logger.info("security.oauth2.client.id="+clientId+"/["+ConsumerConfig.CLIENT_ID_CONFIG+"]");
         //   props.put(ConsumerConfig.CLIENT_ID_CONFIG,clientId);
        }

        if (!StringUtils.isEmpty(jaasConfig)){
            logger.info("spring.kafka.properties.sasl.jaas.config="+jaasConfig+"/["+SaslConfigs.SASL_JAAS_CONFIG+"]");
            props.put(SaslConfigs.SASL_JAAS_CONFIG,jaasConfig);
        }

        if (!StringUtils.isEmpty(securityProtocol)){ // SASL_SSL
            logger.info("spring.kafka.properties.security.protocol="+securityProtocol+"/["+CommonClientConfigs.SECURITY_PROTOCOL_CONFIG+"]");
            props.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG,securityProtocol);
        }

        if (!StringUtils.isEmpty(saslMechanism)){ // OAUTHBEARER
            logger.info("spring.kafka.properties.sasl.mechanism="+saslMechanism+"/["+SaslConfigs.SASL_MECHANISM+"]");
            props.put(SaslConfigs.SASL_MECHANISM,saslMechanism);
        }

        if (!StringUtils.isEmpty(sslTrustStoreLocation)){
            logger.info("spring.kafka.ssl.trust-store-location="+sslTrustStoreLocation+"/["+SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG+"]");
           // props.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG,sslTrustStoreLocation);
            String sslTrustStoreLocationCopy = new String(sslTrustStoreLocation);
            if (sslTrustStoreLocationCopy.startsWith("file:/")){
                sslTrustStoreLocationCopy = sslTrustStoreLocationCopy.substring("file:/".length());
            }
            props.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, sslTrustStoreLocationCopy);
        }

        if (resourceFile!=null){
            try {
                String file = resourceFile.getFile().getAbsolutePath();
                logger.info("spring.kafka.ssl.trust-store-location="+file+"/["+SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG+"]");
                props.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, file);
            } catch (IOException e) {
                logger.info(e.getMessage());
            }
        }

        if (!StringUtils.isEmpty(sslTrustStorePassword)){
            //logger.info("spring.kafka.ssl.trust-store-password="+sslTrustStorePassword+"/["+SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG+"]");
            logger.info("spring.kafka.ssl.trust-store-password=[hidden]");
            props.put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG,sslTrustStorePassword);
        }

        if (!StringUtils.isEmpty(saslLoginCallbackHandlerClass)){
            logger.info("spring.kafka.properties.sasl.login.callback.handler.class="+saslLoginCallbackHandlerClass+"");
            props.put(SaslConfigs.SASL_LOGIN_CALLBACK_HANDLER_CLASS,saslLoginCallbackHandlerClass);
        }

        return props;
    }

    @Bean
    public Map<String, Object> producerStringConfigs() {
        logger.info("@Bean:producerStringConfigs");
        Map<String, Object> configProps = new HashMap<>();
        configProps.putAll(getCommonProperties());
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        return configProps;
    }

    @Bean
    public Map<String, Object> producerJsonConfigs() {
        logger.info("@Bean:producerJsonConfigs");
        Map<String, Object> configProps = new HashMap<>();
        configProps.putAll(getCommonProperties());
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        return configProps;
    }

    @Bean
    public ProducerFactory<String, String> producerPackageFactory() {
        logger.info("@Bean:producerPackageFactory");
        return new DefaultKafkaProducerFactory<>(producerStringConfigs());
    }

    @Bean
    public ProducerFactory<String, MetricModel> producerMetricFactory() {
        logger.info("@Bean:producerMetricFactory");
        return new DefaultKafkaProducerFactory<>(producerJsonConfigs());
    }

    @Bean
    public ProducerFactory<String, AssetModel> producerAssetFactory() {
        logger.info("@Bean:producerAssetFactory");
        return new DefaultKafkaProducerFactory<>(producerJsonConfigs());
    }

    @Bean
    public ProducerFactory<String, AlertEventModel> producerAlertFactory() {
        logger.info("@Bean:producerAlertFactory");
        return new DefaultKafkaProducerFactory<>(producerJsonConfigs());
    }

    @Bean
    public ProducerFactory<String, DtmCmdEventModel> producerDtmCmdFactory() {
        logger.info("@Bean:producerDtmCmdFactory");
        return new DefaultKafkaProducerFactory<>(producerJsonConfigs());
    }

    @Bean(name="packageKafkaTemplate")
    public KafkaTemplate<String, String> packageKafkaTemplate() {
        logger.info("@Bean:packageKafkaTemplate");
        return new KafkaTemplate<>(producerPackageFactory());
    }

    @Bean(name="metricKafkaTemplate")
    public KafkaTemplate<String, MetricModel> metricKafkaTemplate() {
        logger.info("@Bean:metricKafkaTemplate");
        return new KafkaTemplate<>(producerMetricFactory());
    }

    @Bean(name="assetKafkaTemplate")
    public KafkaTemplate<String, AssetModel> assetKafkaTemplate() {
        logger.info("@Bean:assetKafkaTemplate");
        return new KafkaTemplate<>(producerAssetFactory());
    }

    @Bean(name="alertKafkaTemplate")
    public KafkaTemplate<String, AlertEventModel> alertKafkaTemplate() {
        logger.info("@Bean:alertKafkaTemplate");
        return new KafkaTemplate<>(producerAlertFactory());
    }

    @Bean(name="dtmCmdKafkaTemplate")
    public KafkaTemplate<String, DtmCmdEventModel> dtmCmdKafkaTemplate() {
        logger.info("@Bean:dtmCmdKafkaTemplate");
        return new KafkaTemplate<>(producerDtmCmdFactory());
    }

    @Bean(name="realTimeKafkaTemplate")
    public KafkaTemplate<String, String> realTimeKafkaTemplate() {
        logger.info("@Bean:realTimeKafkaTemplate");
        return new KafkaTemplate<>(producerPackageFactory());
    }

    @Bean
    public AdminClient adminClient(){
        logger.info("@Bean:adminClient");
        Properties config = new Properties();
        config.putAll(getCommonProperties());
        AdminClient admin = AdminClient.create(config);
        return admin;

    }
}
