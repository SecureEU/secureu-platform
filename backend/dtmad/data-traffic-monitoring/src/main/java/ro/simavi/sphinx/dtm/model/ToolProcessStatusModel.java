package ro.simavi.sphinx.dtm.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ToolProcessStatusModel implements Serializable {

    private boolean isAlive;

    private long noPcap;

    private String info;

    private ProcessModel processModel;

    private List<ProcessModel> processModelList;

    boolean starting;

}
