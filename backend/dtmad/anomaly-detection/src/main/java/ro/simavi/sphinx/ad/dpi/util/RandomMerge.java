package ro.simavi.sphinx.ad.dpi.util;

import java.io.*;
import java.util.Random;

public class RandomMerge {

    private String exportNormalFileName;

    private String exportAlertFileName;

    private String exportRandomMergeFileName;

    private BufferedWriter bw;

    public RandomMerge(String exportNormalFileName, String exportAlertFileName, String exportRandomMergeFileName){
        this.exportNormalFileName = exportNormalFileName;
        this.exportAlertFileName = exportAlertFileName;
        this.exportRandomMergeFileName = exportRandomMergeFileName;

        try {
            File file = new File(exportRandomMergeFileName);
            if (!file.exists()) {
                file.createNewFile();
            }
            FileWriter fw = new FileWriter(file.getAbsoluteFile());
            bw = new BufferedWriter(fw);
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    public void execute(){
        Random random = new Random();
        try (FileReader normalFileReader = new FileReader(exportNormalFileName);
             FileReader alertFileReader = new FileReader(exportAlertFileName);
             BufferedReader normalReader = new BufferedReader(normalFileReader);
             BufferedReader alertReader = new BufferedReader(alertFileReader)) {

            boolean doneAlert = false;
            boolean doneNormal = false;
            do{
                boolean putAlert = random.nextBoolean();

                if (putAlert) {
                    String alertLine = alertReader.readLine();
                    if (alertLine != null) {
                        bw.write(alertLine+"\n");
                    }else{
                        doneAlert = true;
                    }
                } else {
                    String normalLine = normalReader.readLine();
                    if (normalLine != null) {
                        bw.write(normalLine+"\n");
                    }else{
                        doneNormal = true;
                    }
                }
            }while (doneAlert==false && doneNormal==false);

            if (!doneAlert) {
                String alertLine = null;
                while ((alertLine = alertReader.readLine()) != null) {
                    bw.write(alertLine+"\n");
                }
            }

            if (!doneNormal) {
                String normalLine = null;
                while ((normalLine = normalReader.readLine()) != null) {
                    bw.write(normalLine+"\n");
                }
            }

            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
