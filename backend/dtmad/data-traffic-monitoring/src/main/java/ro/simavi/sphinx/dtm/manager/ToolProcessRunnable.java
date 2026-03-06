package ro.simavi.sphinx.dtm.manager;

import ro.simavi.sphinx.dtm.model.ProcessModel;
import ro.simavi.sphinx.dtm.model.ToolModel;
import ro.simavi.sphinx.dtm.model.ProcessFilterModel;
import ro.simavi.sphinx.dtm.model.ToolProcessStatusModel;

import java.io.IOException;
import java.util.List;

public interface ToolProcessRunnable extends Runnable{

    void startProcess(ProcessFilterModel processFilterModel) throws IOException;

    void startProcess() throws IOException;

    void stopProcess();

    ToolProcessStatusModel statusProcess();

    void setEnabled(Boolean enabled);

    ToolModel getToolModel();

    ProcessFilterModel getProcessFilterModel();

    List<ProcessModel> getProcessModelList();
}
