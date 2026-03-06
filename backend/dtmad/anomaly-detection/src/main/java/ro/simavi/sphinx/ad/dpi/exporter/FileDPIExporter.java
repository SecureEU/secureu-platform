package ro.simavi.sphinx.ad.dpi.exporter;

import com.fasterxml.jackson.databind.ObjectMapper;
import ro.simavi.sphinx.ad.dpi.flow.model.ProtocolFlow;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class FileDPIExporter implements DPIExporter{

    private String exportFileName;

    private ObjectMapper mapper;

    private BufferedWriter bw;

    private Boolean malware;

    public FileDPIExporter(String exportFileName, Boolean malware){
        this.exportFileName = exportFileName;
        this.mapper = new ObjectMapper();
        this.malware = malware;
        open();
    }

    public FileDPIExporter(String exportFileName){
        this.exportFileName = exportFileName;
        this.mapper = new ObjectMapper();
        this.malware = Boolean.FALSE;
        open();
    }

    private void open(){
        try {
            File file = new File(exportFileName);
            if (!file.exists()) {
                file.createNewFile();
            }

            FileWriter fw = new FileWriter(file.getAbsoluteFile());
            bw = new BufferedWriter(fw);
        }catch (IOException e) {
            e.printStackTrace();
        }
    }

    //??
    public void endProcess(){
        try {
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void save(ProtocolFlow protocolFlow) {
        try{
            if (protocolFlow!=null) {
                protocolFlow.setMalware(this.malware);
                bw.write(this.mapper.writeValueAsString(protocolFlow) + "\n");
            }else{
                System.out.println(protocolFlow);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void saveSflow(ProtocolFlow protocolFlow) { }

    @Override
    public void saveSflow(String[] csvLine) {

    }
}
