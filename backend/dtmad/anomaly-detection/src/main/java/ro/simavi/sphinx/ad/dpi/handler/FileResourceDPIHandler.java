package ro.simavi.sphinx.ad.dpi.handler;

import ro.simavi.sphinx.ad.dpi.exporter.DPIExporter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class FileResourceDPIHandler {

    protected DPIHandler dpiHandler;

    protected String filename;

    public FileResourceDPIHandler(String filename, DPIExporter dpiExporter){
        this.dpiHandler = new DPIHandler();
        this.dpiHandler.setDPIExporter(dpiExporter);
        this.filename = filename;
    }

    private InputStream getFileFromResourceAsStream(String fileName) {

        // The class loader that loaded the class
        ClassLoader classLoader = getClass().getClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream(fileName);

        // the stream holding the file content
        if (inputStream == null) {
            throw new IllegalArgumentException("file not found! " + fileName);
        } else {
            return inputStream;
        }

    }

    public void execute() {
        this.execute(getFileFromResourceAsStream(filename));
    }

    // print input stream
    public void execute(InputStream is) {

        try (InputStreamReader streamReader = new InputStreamReader(is, StandardCharsets.UTF_8);
             BufferedReader reader = new BufferedReader(streamReader)) {

            String line;
            while ((line = reader.readLine()) != null) {
                dpiHandler.receiveMessage(line);
            }
           // dpiHandler.endProcess();

        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
