package ro.simavi.sphinx.dtm.configuration;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.config.SslConfigs;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@EnableKafka
@Configuration
@Profile("!agent")
@DependsOn({"createSmModel"})
public class KafkaConsumerConfig {

    private static final Logger logger = LoggerFactory.getLogger(KafkaConsumerConfig.class);

    @Value(value = "${spring.kafka.bootstrap-servers}")
    private String bootstrapAddress;

    @Value(value = "${kafka.group.id}")
    private String groupId;

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

    private Map<String, Object> getCommonProperties() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapAddress);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        if (!StringUtils.isEmpty(clientId)){
      //      logger.info("security.oauth2.client.id="+clientId+"/["+ConsumerConfig.CLIENT_ID_CONFIG+"]");
      //      props.put(ConsumerConfig.CLIENT_ID_CONFIG,clientId);
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
            String sslTrustStoreLocationCopy = new String(sslTrustStoreLocation);
            if (sslTrustStoreLocationCopy.startsWith("file:/")){
                sslTrustStoreLocationCopy = sslTrustStoreLocationCopy.substring("file:/".length());
            }
            props.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG,sslTrustStoreLocationCopy);
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
    private Map<String, Object> getProps(){
        Map<String, Object> props = new HashMap<>();
        props.putAll(getCommonProperties());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        return props;
    }

    private Map<String, Object> getAdminProps(){
        Map<String, Object> props = new HashMap<>();
        props.putAll(getCommonProperties());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        return props;
    }

    @Bean
    public ConsumerFactory<String, Object> consumerStringFactory() {
        logger.info("@Bean:consumerStringFactory");
        DefaultKafkaConsumerFactory defaultKafkaConsumerFactory = new DefaultKafkaConsumerFactory<>(getProps());
        Object value = defaultKafkaConsumerFactory.getConfigurationProperties().get(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG);
        logger.info("consumerStringFactory==="+value);
        return defaultKafkaConsumerFactory;
    }

    @Bean
    public ConsumerFactory<String, String> consumerJsonFactory() {
        logger.info("@Bean:consumerJsonFactory");
        Map<String, Object> props = new HashMap<>();
        props.putAll(getCommonProperties());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        DefaultKafkaConsumerFactory defaultKafkaConsumerFactory = new DefaultKafkaConsumerFactory<>(props);

        Object value = defaultKafkaConsumerFactory.getConfigurationProperties().get(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG);
        logger.info("consumerJsonFactory==="+value);

        return defaultKafkaConsumerFactory;
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaMetricListenerContainerFactory() {
        logger.info("@Bean:kafkaMetricListenerContainerFactory");
        ConcurrentKafkaListenerContainerFactory<String, String> factory =  new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerJsonFactory());
        return factory;
    }


    @Bean("filterKafkaListenerContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, String> filterKafkaListenerContainerFactory() {
        logger.info("@Bean:filterKafkaListenerContainerFactory");
        ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerStringFactory());

        return factory;
    }

    @Bean
    public KafkaAdmin kafkaAdmin(){
        logger.info("@Bean:kafkaAdmin [No-Agent]");
        return new KafkaAdmin(getAdminProps());
    }

    @Bean
    public KafkaConsumer<String,String> kafkaConsumer(){
        logger.info("@Bean:kafkaAdmin [No-Agent]");

        Map<String, Object> properties = getAdminProps();

        properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
     //   properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class);
     //   properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class);
        properties.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 100);
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        properties.put(ConsumerConfig.CLIENT_ID_CONFIG, "dtm-kafka-consumer");
        properties.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, "read_committed");

        // properties.put("enable.partition.eof", "false");

        return new KafkaConsumer(properties);
    }

}
