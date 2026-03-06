package ro.simavi.sphinx.ad.ws;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

//@RestController
//@RequestMapping("/about")
public class AboutController {

    private final BuildProperties buildProperties;

    private final Environment environment;

    //@Autowired
    public AboutController(BuildProperties buildProperties, Environment environment){
        this.buildProperties = buildProperties;
        this.environment = environment;
    }

    @RequestMapping(value = "/info", produces = "application/json", method = RequestMethod.GET)
    public ResponseEntity<String> info() {

        String profile = environment.getActiveProfiles().length>0?environment.getActiveProfiles()[0]:"-";

        // Artifact's name from the pom.xml file
        String name = buildProperties.getName();
        // Artifact version
        String version = buildProperties.getVersion();
        // Date and Time of the build
        String time = buildProperties.getTime().toString();
        // Artifact ID from the pom file
        String artifact = buildProperties.getArtifact();
        // Group ID from the pom file
        String group = buildProperties.getGroup();

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder
                .append("profile=").append(profile).append("\n")
                .append("name=").append(name).append("\n")
                .append("version=").append(version).append("\n")
                .append("time=").append(time).append("\n")
                .append("artifact=").append(artifact).append("\n")
                .append("group=").append(group).append("\n");

        return ResponseEntity.ok()
                .body(stringBuilder.toString());

    }
}
