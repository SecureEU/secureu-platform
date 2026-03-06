package ro.simavi.sphinx.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SphinxFilter {

    private int pageNumber;

    private int pageSize;

    private String search;

    private int firstId;

}
