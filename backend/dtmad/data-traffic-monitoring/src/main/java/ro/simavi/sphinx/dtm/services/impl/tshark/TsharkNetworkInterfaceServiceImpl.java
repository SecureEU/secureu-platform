package ro.simavi.sphinx.dtm.services.impl.tshark;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ro.simavi.sphinx.dtm.configuration.DTMConfigProps;
import ro.simavi.sphinx.dtm.model.NetworkInterfaceModel;
import ro.simavi.sphinx.dtm.services.NetworkInterfaceService;
import ro.simavi.sphinx.dtm.util.DtmTools;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

@Service
@Qualifier("tsharkNetworkInterfaceService")
public class TsharkNetworkInterfaceServiceImpl implements NetworkInterfaceService {

    private final DTMConfigProps dtmConfigProps;

    public TsharkNetworkInterfaceServiceImpl(DTMConfigProps dtmConfigProps){
        this.dtmConfigProps = dtmConfigProps;
    }

    @Override
    public List<NetworkInterfaceModel> getNetworkInterfaceModelList() throws IOException{

        String path = dtmConfigProps.getToolModelHashMap().get(DtmTools.TSHARK.getName()).getPath();

        List<NetworkInterfaceModel> list = new ArrayList<>();

        List<String> commandAndArgs = new ArrayList<>();
        //if (path.startsWith("docker")){
        //    path = path.replaceAll("_#PID","");
        //    commandAndArgs.addAll(Arrays.asList(path.split(" ")));
        //}else {
        String command = path + File.separator + "tshark";
        commandAndArgs.add(command);
        //}
        commandAndArgs.add("-l");
        commandAndArgs.add("-D");

        ProcessBuilder pb = new ProcessBuilder(commandAndArgs);
        Process process = pb.start();

        BufferedReader br = null;
        try {
            br = new BufferedReader(new InputStreamReader(process.getInputStream()), 1);
            String line = null;
            while ((line = br.readLine()) != null) {
                list.add(getNetworkInterfaceModel(line));
            }

            br = new BufferedReader(new InputStreamReader(process.getErrorStream()), 1);
            line = null;
            StringBuilder stringBuilder = new StringBuilder();
            while ((line = br.readLine()) != null) {
                stringBuilder.append(line);
            }

        } catch(Exception e){

        }

        return list;

    }

    public NetworkInterfaceModel getNetworkInterfaceModel(String fullName){
        String name = fullName;
        String shortName = fullName;

        String[] names = fullName.split(" ", 3);
        if (names.length == 3) {
            name = names[1];
            shortName = names[2];
            shortName = shortName.substring(1, shortName.length() - 1);
        }
        if (names.length == 2) {
            name = names[1];
            shortName = names[1];
        }

        Long id = -1L;
        try{
            id = Long.parseLong(names[0]);
        }catch (Exception e){

        }

        NetworkInterfaceModel tsharkInterfaceModel = new NetworkInterfaceModel(id, name, shortName, fullName);

        return tsharkInterfaceModel;

    }

}
