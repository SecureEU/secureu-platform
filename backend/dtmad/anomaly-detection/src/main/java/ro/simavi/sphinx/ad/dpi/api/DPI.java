package ro.simavi.sphinx.ad.dpi.api;

import ro.simavi.sphinx.ad.dpi.flow.model.ProtocolFlow;
import ro.simavi.sphinx.model.event.HogzillaModel;
import ro.simavi.sphinx.model.event.NFstreamModel;

// deep packet inspection for suricata
public interface DPI {

    void process(NFstreamModel nFstreamModel);

    void processHogzilla(HogzillaModel hogzillaModel);

    // save in Hbase, 'adml_flows' table
    void saveFlow(ProtocolFlow protocolFlow);

    void gc();
}
