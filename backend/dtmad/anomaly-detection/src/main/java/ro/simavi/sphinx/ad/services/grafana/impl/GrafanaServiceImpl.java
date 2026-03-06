package ro.simavi.sphinx.ad.services.grafana.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import ro.simavi.sphinx.ad.services.grafana.GrafanaService;
import ro.simavi.sphinx.model.grafana.GrafanaDatasource;
import ro.simavi.sphinx.model.grafana.GrafanaDatasourceList;

import java.util.Collections;
import java.util.List;

@Service
public class GrafanaServiceImpl implements GrafanaService {

    private final RestTemplate restTemplate;

    @Autowired
    public GrafanaServiceImpl(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public List<GrafanaDatasource> getDataSources() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.setBasicAuth("admin","admin");

        HttpEntity<String> entity = new HttpEntity<>(headers);

        ParameterizedTypeReference<GrafanaDatasourceList> parameterizedTypeReference = new ParameterizedTypeReference<GrafanaDatasourceList>(){};

        String url = "http://localhost:3000/api/datasources";
        HttpEntity<GrafanaDatasourceList> response = restTemplate.exchange(url, HttpMethod.GET, entity, parameterizedTypeReference);

        return response.getBody().getDatasourceList();
    }

    @Override
    public String getDataSourcesString() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.setBasicAuth("admin","admin");

        HttpEntity<String> entity = new HttpEntity<>(headers);

        String url = "http://localhost:3000/api/datasources";
        HttpEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

        return response.getBody();
    }

    @Override
    public String importDataSource(GrafanaDatasource[] grafanaDatasourceList) {
        String result = "";
        for(GrafanaDatasource grafanaDatasource: grafanaDatasourceList){
            result = result + "<br/>"+importDataSource(grafanaDatasource);
        }
        return result;
    }

    private String importDataSource(GrafanaDatasource grafanaDatasource){
        String result = grafanaDatasource.getName()+" ("+ grafanaDatasource.getId() +"):";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.setBasicAuth("admin","admin");

        HttpEntity<GrafanaDatasource> entity = new HttpEntity<>(grafanaDatasource,headers);

        ParameterizedTypeReference<String> parameterizedTypeReference = new ParameterizedTypeReference<String>(){};

        String url = "http://localhost:3000/api/datasources";
        HttpEntity<String> response = null;
        try {
            response = restTemplate.exchange(url, HttpMethod.POST, entity, parameterizedTypeReference);
        }catch (Exception e){
            return result + e.getMessage();
        }
        return result + ((ResponseEntity<String>) response).getStatusCode().toString();
    }
}
