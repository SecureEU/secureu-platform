package ro.simavi.sphinx.dtm.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import ro.simavi.sphinx.dtm.model.ToolModel;

import java.util.HashMap;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix="dtm")
public class DTMConfigProps {

    private String name;

    private String instanceKey;

    private HashMap<String, ToolModel> toolModelHashMap;

}
