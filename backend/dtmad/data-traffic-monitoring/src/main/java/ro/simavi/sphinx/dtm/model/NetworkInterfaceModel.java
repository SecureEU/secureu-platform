package ro.simavi.sphinx.dtm.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class NetworkInterfaceModel implements Serializable {

    private Long id;

    private String name;

    private String displayName;

    private String fullName;

}
