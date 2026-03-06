package ro.simavi.sphinx.ad.services.rest.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import ro.simavi.sphinx.ad.services.rest.InfoService;
import ro.simavi.sphinx.model.PacketCaptureListModel;
import ro.simavi.sphinx.model.SphinxFilter;
import ro.simavi.sphinx.model.SphinxPage;

import java.util.Collections;

@Service
public class InfoServiceImpl implements InfoService {

    private final RestTemplate restTemplate;

    @Autowired
    public InfoServiceImpl(RestTemplate restTemplate){
        this.restTemplate = restTemplate;

    }
    /** The AD component collects suspicious data traffic and information from the DTM and processes it to identify anomalies. */

    /*
    •	AD.I.02: Abnormal and Suspicious Traffic Activity Interface
    This interface allows the AD to receive information on suspicious traffic data (data traffic packets) from the DTM component to perform anomaly detection analyses.
    o	Input: Suspicious traffic data;
    o	Output: Not applicable.
    */

    public SphinxPage<PacketCaptureListModel> getAbnormalAndSuspiciousData(SphinxFilter sphinxFilter) {

        /*todo: tabela gestiune servicii */
        String DTM_ABNORMAL_AND_SUSPICIOUS_DATA_URL = "http://localhost:8087/sphinx/dtm/api/info/data/abnormalAndSuspicious" ;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        HttpEntity<SphinxFilter> request = new HttpEntity<>(sphinxFilter,headers);

        ParameterizedTypeReference<SphinxPage<PacketCaptureListModel>> parameterizedTypeReference = new ParameterizedTypeReference<SphinxPage<PacketCaptureListModel>>(){};

        HttpEntity<SphinxPage<PacketCaptureListModel>> response = restTemplate.exchange(
                String.join("/", DTM_ABNORMAL_AND_SUSPICIOUS_DATA_URL),
                HttpMethod.POST, // daca se trimite prin post SphinxFilter nu se trimite
                request,
                parameterizedTypeReference
        );

        return response.getBody();
    }

    /* The AD component collects data and information on potential cyber-attacks from the HP and processes it to identify anomalies (new and advanced threats). */
    /*
    •	AD.I.03: Honeypots Interface
    This interface allows the AD to receive information on potential new cyber-attacks (unknown or unregistered advanced threats) from the HP component to perform anomaly detection analyses.
    o	Input: Not applicable;
    o	Output: Collected potential cyber-attack data.
    */

    @Override
    public void getPotentialCyberAttacksData() {
        /* de stabilit cum ne trimite HP datele*/
    }
}
