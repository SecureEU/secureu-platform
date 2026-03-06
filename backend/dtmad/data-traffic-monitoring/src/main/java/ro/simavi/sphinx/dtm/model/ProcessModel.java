package ro.simavi.sphinx.dtm.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ro.simavi.sphinx.dtm.entities.enums.ProcessType;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ProcessModel implements Serializable {

    private Long pid;

    private String filterName;

    private String interfaceName;

    private String interfaceDisplayName;

    @NotNull
    private String interfaceFullName;

    private String instanceKey;

    private Boolean active;

    private Boolean enabled;

    private ProcessFilterModel filterModel;

    private ProcessType processType;

}
