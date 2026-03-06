package ro.simavi.sphinx.ad.dpi.exporter;

import ro.simavi.sphinx.ad.dpi.flow.model.ProtocolFlow;

public class ConsoleDPIExporter implements DPIExporter{

    @Override
    public void save(ProtocolFlow protocolFlow) {
        System.out.println(protocolFlow);
    }

    @Override
    public void saveSflow(ProtocolFlow protocolFlow) {
    }

    @Override
    public void saveSflow(String[] csvLine) {

    }

    @Override
    public void endProcess() {

    }

}
