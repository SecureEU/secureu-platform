package ro.simavi.sphinx.serializers;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;

/**
 * Deserializes "host" when it can be either a string or an object like {"name":"L304455"} (e.g. from Logstash/Beats).
 */
public class HostStringDeserializer extends JsonDeserializer<String> {

    @Override
    public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonToken t = p.getCurrentToken();
        if (t == JsonToken.VALUE_STRING) {
            return p.getText();
        }
        if (t == JsonToken.START_OBJECT) {
            JsonNode node = p.getCodec().readTree(p);
            JsonNode nameNode = node != null ? node.get("name") : null;
            return (nameNode != null && nameNode.isTextual()) ? nameNode.asText() : null;
        }
        return null;
    }
}
