package ro.simavi.sphinx.dtm.services.impl.tshark;

import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Service;
import ro.simavi.sphinx.dtm.configuration.DTMConfigProps;
import ro.simavi.sphinx.dtm.model.ToolModel;
import ro.simavi.sphinx.dtm.services.NetworkPersistService;
import ro.simavi.sphinx.dtm.util.DtmTools;
import ro.simavi.sphinx.dtm.util.SFTPClient;

import java.io.File;
import java.io.IOException;


@Service
public class NetworkPersistServiceImpl implements NetworkPersistService {

    private final DTMConfigProps dtmConfigProps;

    public NetworkPersistServiceImpl(DTMConfigProps dtmConfigProps){
        this.dtmConfigProps = dtmConfigProps;
    }

    @Override
    public Boolean persist() {

        ToolModel toolModel = dtmConfigProps.getToolModelHashMap().get(DtmTools.TSHARK.getName());
        String logDir = toolModel.getProperties().get("logDir");
        String persistDir = toolModel.getProperties().get("persistDir");

        try {
            // source & destination directories
            File src = new File(logDir);
            File dest = new File(persistDir);

            // copy all files and folders from `src` to `dest`
            FileUtils.copyDirectory(src, dest);

            SFTPClient sftpClient = new SFTPClient(toolModel);
            sftpClient.uploadRemote(dtmConfigProps.getInstanceKey());

        } catch (IOException ex) {
            ex.printStackTrace();
            return false;
        }

        return true;

    }

    @Override
    public Boolean clean() {
        ToolModel toolModel = dtmConfigProps.getToolModelHashMap().get(DtmTools.TSHARK.getName());
        String logDir = toolModel.getProperties().get("logDir");
        String persistDir = toolModel.getProperties().get("persistDir");
        try {
            FileUtils.deleteDirectory(new File(logDir));
            FileUtils.forceMkdir(new File(logDir));

            FileUtils.deleteDirectory(new File(persistDir));
            FileUtils.forceMkdir(new File(persistDir));
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }
}
