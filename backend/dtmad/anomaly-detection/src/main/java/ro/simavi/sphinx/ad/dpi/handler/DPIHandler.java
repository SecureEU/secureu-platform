package ro.simavi.sphinx.ad.dpi.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import ro.simavi.sphinx.ad.dpi.exporter.DPIExporter;
import ro.simavi.sphinx.ad.dpi.exporter.HBaseDPIExporter;
import ro.simavi.sphinx.ad.dpi.impl.AdDpi;
import ro.simavi.sphinx.model.event.HogzillaModel;
import ro.simavi.sphinx.model.event.NFstreamModel;

import java.io.IOException;

@Component
public class DPIHandler {

    private AdDpi adDpi;

    public DPIHandler(){
        adDpi = new AdDpi();
        adDpi.setDpiExporter(new HBaseDPIExporter());
    }

    public void receiveMessage(String flow){
        NFstreamModel nfstreamModel = getEvent(flow);
        if (nfstreamModel!=null) {
            adDpi.process(nfstreamModel);
        }
    }

    public void receiveHogMessage(String flow){
        HogzillaModel hogzillaModel = getHogEvent(flow);
        if (hogzillaModel!=null) {
            adDpi.processHogzilla(hogzillaModel);
        }
    }

    private NFstreamModel getEvent(String flow){

        ObjectMapper mapper = new ObjectMapper();

        NFstreamModel nfstreamModel = null;
        try {
            nfstreamModel = mapper.readerFor(NFstreamModel.class).readValue(flow);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return nfstreamModel;
    }
    private HogzillaModel getHogEvent(String flow){

        ObjectMapper mapper = new ObjectMapper();

        HogzillaModel hogzillaModel = null;
        try {
            hogzillaModel = mapper.readerFor(HogzillaModel.class).readValue(flow);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return hogzillaModel;
    }

    public void endProcess() {
        adDpi.gc();
    }

    public void setDPIExporter(DPIExporter dpiExporter) {
        this.adDpi.setDpiExporter(dpiExporter);
    }
}
