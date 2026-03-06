package ro.simavi.sphinx.dtm.model;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.Map;

@Getter
@Setter
public class MessageModel {

    private int partition;
    private long offset;
    private String message;
    private String key;
    private Map<String, String> headers;
    private Date timestamp;


}
