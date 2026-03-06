package ro.simavi.sphinx.dtm.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProcessFilterModel {

    private Long id;

    private String name;

    private String command;

    private String description;

    private String code;

    private Boolean canDelete;
}
