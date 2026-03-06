package ro.simavi.sphinx.ad.model;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Setter
@Getter
public class ResponseModel implements Serializable {

    private Boolean success;

    private String message;

}
