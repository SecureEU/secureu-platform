package ro.simavi.sphinx.dtm.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.env.Environment;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import ro.simavi.sphinx.model.sm.SmModel;
import ro.simavi.sphinx.model.sm.SmResponseModel;

import java.util.Collections;
import java.util.Properties;

@Configuration
public class SmConfig {

    @Autowired
    private Environment environment;

    @Autowired
    private RestTemplate restTemplate;

    private static final Logger logger = LoggerFactory.getLogger(SmConfig.class);

    public static final String OAUTH_CLIENT_ID = "oauth.client.id";
    public static final String OAUTH_CLIENT_SECRET = "oauth.client.secret";
    public static final String OAUTH_TOKEN_ENDPOINT_URI = "oauth.token.endpoint.uri";

    @Bean
    public SmModel createSmModel(){

        SmModel smModel = new SmModel();

            smModel.setUrl(getEnvValue("sphinx.sm.ip", "SM_IP"));
            smModel.setUsername(getEnvValue("sphinx.sm.username", "KAFKA_USERNAME"));
            smModel.setPassword(getEnvValue("sphinx.sm.password", "KAFKA_PASSWORD"));
            smModel.setKafkaAuthentication(getEnvValue("sphinx.sm.kafkaAuthentication", "KAFKA_AUTHENTICATION"));
            smModel.setOauthTokenEndPointUri(getEnvValue("oauth.token.endpoint.uri", "OAUTH_TOKEN_ENDPOINT_URI"));
            smModel.setOauthClientId(getEnvValue("oauth.client.id", "OAUTH_CLIENT_ID"));


        String oauthClientSecret = getOauthClientSecret(smModel);
        smModel.setOauthClientSecret(oauthClientSecret);

        String oauthClientSecretOld = System.getenv("OAUTH_CLIENT_SECRET");

        logger.info("OAUTH_CLIENT_SECRET[old]="+oauthClientSecretOld);
        logger.info("OAUTH_CLIENT_SECRET[new]="+oauthClientSecret);

        Properties properties = System.getProperties();

        if(oauthClientSecret != null){
            properties.put(OAUTH_CLIENT_ID,smModel.getOauthClientId());
            properties.put(OAUTH_CLIENT_SECRET,oauthClientSecret);
            properties.put(OAUTH_TOKEN_ENDPOINT_URI,smModel.getOauthTokenEndPointUri());
        }

        return smModel;
    }

    private String getOauthClientSecret(SmModel smModel){
        ParameterizedTypeReference<SmResponseModel> parameterizedTypeReference = new ParameterizedTypeReference<SmResponseModel>(){};

        MultiValueMap<String, String> map= new LinkedMultiValueMap<String, String>();
        map.add("username", smModel.getUsername());
        map.add("password", smModel.getPassword());

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<MultiValueMap<String, String>>(map, getHeaders());

        try{
            ResponseEntity<SmResponseModel> smResponseModelResponse = restTemplate.exchange(smModel.getUrl()+smModel.getKafkaAuthentication(), HttpMethod.POST, request, parameterizedTypeReference);
            SmResponseModel smResponseModel =  smResponseModelResponse.getBody();
            if (smResponseModel!=null){
                return smResponseModel.getData();
            }
        }catch (Exception e){
            logger.error("========getOauthClientSecret====="+e.getMessage());
        }
        return null;
    }


    private HttpHeaders getHeaders(){
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        return headers;
    }

    private String getEnvValue(String propertityName, String systemEnvName){
        String value = System.getenv(systemEnvName);
        if (value!=null){
            return value;
        }
        return environment.getProperty(propertityName);
    }

}
