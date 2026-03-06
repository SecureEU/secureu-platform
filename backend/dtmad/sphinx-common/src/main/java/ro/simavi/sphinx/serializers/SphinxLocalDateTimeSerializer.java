package ro.simavi.sphinx.serializers;

import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;

import java.time.format.DateTimeFormatter;

public class SphinxLocalDateTimeSerializer extends LocalDateTimeSerializer {

    public SphinxLocalDateTimeSerializer(){
        super(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"));
    }
}
