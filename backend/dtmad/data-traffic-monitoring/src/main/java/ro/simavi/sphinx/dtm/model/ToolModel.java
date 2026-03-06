package ro.simavi.sphinx.dtm.model;


import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;

@Getter
@Setter
public class ToolModel {

    private String name;

    private String path;

    private String exe;

    private String excludeIP;

    private HashMap<String, String> properties = new HashMap();

}
