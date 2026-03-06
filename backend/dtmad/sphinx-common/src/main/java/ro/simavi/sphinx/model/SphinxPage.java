package ro.simavi.sphinx.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SphinxPage<T> {

    private long total;
    private SphinxFilter sphinxFilter;
    private T content;

}
