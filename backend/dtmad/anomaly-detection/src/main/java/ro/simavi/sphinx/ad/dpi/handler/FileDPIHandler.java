package ro.simavi.sphinx.ad.dpi.handler;

import ro.simavi.sphinx.ad.dpi.exporter.DPIExporter;

import java.io.*;

public class FileDPIHandler extends FileResourceDPIHandler{

    public FileDPIHandler(String filename, DPIExporter dpiExporter){
        super(filename, dpiExporter);
    }

    public void execute() {
        FileInputStream fileInputStream = null;
        try {
            fileInputStream = new FileInputStream(new File(filename));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        this.execute(fileInputStream);

    }

}
