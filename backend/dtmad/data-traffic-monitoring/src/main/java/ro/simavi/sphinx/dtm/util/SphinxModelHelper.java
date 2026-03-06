package ro.simavi.sphinx.dtm.util;

import ro.simavi.sphinx.model.event.alert.SphinxModel;

public class SphinxModelHelper {

    public static SphinxModel getSphinxModel(String tool){
        SphinxModel sphinxModel = new SphinxModel();
        sphinxModel.setComponent("dtm");
        sphinxModel.setTool(tool);
        sphinxModel.setHostname(NetworkHelper.getHostName());
        sphinxModel.setUsername(NetworkHelper.getUsername());

        return sphinxModel;
    }
}
