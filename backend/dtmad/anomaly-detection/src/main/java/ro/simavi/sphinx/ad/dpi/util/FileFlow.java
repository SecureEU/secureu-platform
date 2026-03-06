package ro.simavi.sphinx.ad.dpi.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import ro.simavi.sphinx.ad.dpi.exporter.DPIExporter;
import ro.simavi.sphinx.ad.dpi.flow.model.ProtocolFlow;
import ro.simavi.sphinx.model.event.NFstreamModel;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class FileFlow {

    private String filename;
    private DPIExporter dpiExporter;

    public FileFlow(String filename, DPIExporter dpiExporter){
        this.dpiExporter = dpiExporter;
        this.filename = filename;
    }

    public void execute() {
        try ( FileInputStream fileInputStream  =  new FileInputStream(new File(filename));
              InputStreamReader streamReader = new InputStreamReader(fileInputStream, StandardCharsets.UTF_8);
              BufferedReader reader = new BufferedReader(streamReader)) {

            String line;
            while ((line = reader.readLine()) != null) {
                ProtocolFlow protocolFlow = getProtocolFlow(line);
                dpiExporter.save(protocolFlow);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private ProtocolFlow getProtocolFlow(String flow){

        ObjectMapper mapper = new ObjectMapper();

        ProtocolFlow protocolFlow = null;
        try {
            protocolFlow = mapper.readerFor(ProtocolFlow.class).readValue(flow);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return protocolFlow;
    }

}