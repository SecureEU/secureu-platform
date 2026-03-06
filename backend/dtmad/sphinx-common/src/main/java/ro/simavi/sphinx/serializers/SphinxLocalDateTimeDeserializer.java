package ro.simavi.sphinx.serializers;

import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;

import java.time.format.DateTimeFormatter;

public class SphinxLocalDateTimeDeserializer extends LocalDateTimeDeserializer {

    public SphinxLocalDateTimeDeserializer(){
        super(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"));
    }
}
