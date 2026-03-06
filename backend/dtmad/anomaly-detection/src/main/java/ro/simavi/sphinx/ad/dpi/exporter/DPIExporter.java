package ro.simavi.sphinx.ad.dpi.exporter;

import ro.simavi.sphinx.ad.dpi.flow.model.ProtocolFlow;

public interface DPIExporter {

    void save(ProtocolFlow protocolFlow);

    void saveSflow(ProtocolFlow protocolFlow);

    void saveSflow(String[] csvLine);

    void endProcess();

}
