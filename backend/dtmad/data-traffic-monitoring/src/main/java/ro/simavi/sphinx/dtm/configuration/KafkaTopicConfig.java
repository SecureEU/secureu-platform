package ro.simavi.sphinx.dtm.configuration;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KafkaTopicConfig {

    @Value(value = "${spring.kafka.bootstrap-servers}")
    private String bootstrapAddress;

    @Value(value="${topic.package}")
    private String topicPackage;

    @Bean
    public NewTopic topicPackage() {
        return new NewTopic(topicPackage, 1, (short) 1);
    }

}
