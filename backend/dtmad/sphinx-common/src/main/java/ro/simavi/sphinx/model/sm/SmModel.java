package ro.simavi.sphinx.model.sm;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SmModel {

    private String url;

    private String username;

    private String password;

    private String oauthClientId;

    private String oauthClientSecret;

    private String oauthTokenEndPointUri;

    private String kafkaAuthentication;

}
