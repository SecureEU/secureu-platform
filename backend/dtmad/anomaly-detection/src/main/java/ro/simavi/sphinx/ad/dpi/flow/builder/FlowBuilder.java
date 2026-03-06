package ro.simavi.sphinx.ad.dpi.flow.builder;

import ro.simavi.sphinx.ad.dpi.flow.model.ProtocolFlow;
import ro.simavi.sphinx.model.event.HogzillaModel;
import ro.simavi.sphinx.model.event.NFstreamModel;

import java.util.List;

public interface FlowBuilder {

    ProtocolFlow build(NFstreamModel model);

    ProtocolFlow buildHogzilla(HogzillaModel model);

}
