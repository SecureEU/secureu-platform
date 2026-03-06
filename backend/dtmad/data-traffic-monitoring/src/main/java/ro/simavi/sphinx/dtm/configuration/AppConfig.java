package ro.simavi.sphinx.dtm.configuration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class AppConfig {

    @Autowired
    private ApplicationContext context;

    /*
    @Bean
    public MessagingSystemService messagingSystemService(@Value("${service.messagingSystemService}") String qualifier) {
        return (MessagingSystemService) context.getBean(qualifier);
    }
    */

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedOrigins("http://localhost:3000",
                                "http://localhost:3001",
                                "http://localhost:8087",
                                "http://localhost:3002",
                                "http://localhost:1337",
                                "http://localhost:80",
                                "http://172.18.0.6:3001",
                                "http://polaris.146.124.106.181.nip.io" )
                        .allowCredentials(false).maxAge(3600);
            }
        };
    }
}
