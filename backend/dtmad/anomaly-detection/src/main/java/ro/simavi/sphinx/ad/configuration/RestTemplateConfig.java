package ro.simavi.sphinx.ad.configuration;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import ro.simavi.sphinx.ad.handlers.RestCallErrorHandler;


@Configuration
public class RestTemplateConfig {
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        RestTemplate restTemplate = builder.build();
        restTemplate.setErrorHandler(new RestCallErrorHandler());
        return restTemplate;
    }
}
