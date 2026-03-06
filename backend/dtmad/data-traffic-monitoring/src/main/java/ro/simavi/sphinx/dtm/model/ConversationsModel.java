package ro.simavi.sphinx.dtm.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ConversationsModel {

    private String time;

    private long packages;

    private long bytes;

    private long packageAB;

    private long packageBA;

    private long bytesAB;

    private long bytesBA;

    private String host;

}
