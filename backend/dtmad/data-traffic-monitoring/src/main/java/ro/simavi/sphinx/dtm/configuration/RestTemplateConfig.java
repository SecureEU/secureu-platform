package ro.simavi.sphinx.dtm.configuration;

import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import ro.simavi.sphinx.dtm.handlers.RestCallErrorHandler;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.time.Duration;


@Configuration
public class RestTemplateConfig {

    @Value(value = "${dtm.restTemplate.disableSSLValidation}")
    private Boolean disableSSLValidation;

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) throws KeyManagementException, NoSuchAlgorithmException {

        RestTemplate restTemplate = builder.setConnectTimeout(Duration.ofMillis(100)).build();
        restTemplate.setErrorHandler(new RestCallErrorHandler());

        if (disableSSLValidation) {
            restTemplate.setRequestFactory(getRequestFactory());
        }
        return restTemplate;
    }

    private ClientHttpRequestFactory getRequestFactory() throws NoSuchAlgorithmException, KeyManagementException{

        TrustManager[] trustAllCerts = new TrustManager[] {
                new X509TrustManager() {
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[0];
                    }
                    public void checkClientTrusted(
                            java.security.cert.X509Certificate[] certs, String authType) {
                    }
                    public void checkServerTrusted(
                            java.security.cert.X509Certificate[] certs, String authType) {
                    }
                }
        };
        SSLContext sslContext = SSLContext.getInstance("SSL");
        sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
        CloseableHttpClient httpClient = HttpClients.custom()
                .setSSLContext(sslContext)
                .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                .build();
        HttpComponentsClientHttpRequestFactory customRequestFactory = new HttpComponentsClientHttpRequestFactory();
        customRequestFactory.setHttpClient(httpClient);
        customRequestFactory.setConnectTimeout(1000);

        return customRequestFactory;

    }
}
