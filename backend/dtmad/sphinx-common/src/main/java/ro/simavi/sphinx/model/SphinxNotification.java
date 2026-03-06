package ro.simavi.sphinx.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

@Getter
@Setter
@ToString
public class SphinxNotification implements Serializable {

    private String title;
    private String content;
    private SphinxNotificationWarningLevel warningLevel;

}
