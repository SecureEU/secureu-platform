package ro.simavi.sphinx.dtm.services.impl.suricata;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ro.simavi.sphinx.dtm.model.NetworkInterfaceModel;
import ro.simavi.sphinx.dtm.services.NetworkInterfaceService;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

@Service
@Qualifier("javaNetworkInterfaceService")
public class JavaNetworkInterfaceServiceImpl implements NetworkInterfaceService {

    private static final Logger logger = LoggerFactory.getLogger(JavaNetworkInterfaceServiceImpl.class);

    @Override
    public List<NetworkInterfaceModel> getNetworkInterfaceModelList() {

        List<NetworkInterfaceModel> networkInterfaceModels = new ArrayList<>();

        try {
            Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();
            for (NetworkInterface netint : Collections.list(nets)) {
                NetworkInterfaceModel networkInterfaceModel = getNetworkInterfaceModel(netint);
                if (networkInterfaceModel!=null) {
                    networkInterfaceModels.add(networkInterfaceModel);
                }
            }
        } catch (SocketException e) {
            logger.error(e.getMessage());
        }

        return networkInterfaceModels;
    }

    @Override
    public NetworkInterfaceModel getNetworkInterfaceModel(String fullName) {
        return null;
    }

    private NetworkInterfaceModel getNetworkInterfaceModel(NetworkInterface netint) throws SocketException {
        if (!netint.isUp()){
            return null;
        }
        if (netint.isLoopback()){
            return null;
        }
        NetworkInterfaceModel networkInterfaceModel = new NetworkInterfaceModel();
        networkInterfaceModel.setFullName(netint.getDisplayName());
        networkInterfaceModel.setDisplayName(netint.getDisplayName());

        Enumeration<InetAddress> inetAddresses = netint.getInetAddresses();
        for (InetAddress inetAddress : Collections.list(inetAddresses)) {
            if (networkInterfaceModel.getName()==null) {
                networkInterfaceModel.setName(inetAddress.toString().substring(1));
            }
        }
        return networkInterfaceModel;
    }
}
