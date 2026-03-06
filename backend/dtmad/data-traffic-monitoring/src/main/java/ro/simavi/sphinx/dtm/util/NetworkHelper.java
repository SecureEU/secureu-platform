package ro.simavi.sphinx.dtm.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;

public class NetworkHelper {

    private static final Logger logger = LoggerFactory.getLogger(NetworkHelper.class);

    public static final String getUsername(){
        return System.getProperty("user.name");
    }

    public static final String getInstanceKey(){
        String value = System.getenv("dtm.instanceKey");
        return value!=null? value:System.getenv("DTM_INSTANCE_KEY");
    }

    public static final String getHostNameFromEnv(){
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            return System.getenv("COMPUTERNAME");
        } else if (os.contains("nix") || os.contains("nux") || os.contains("mac os x")) {
            return System.getenv("HOSTNAME");
        }
        return null;
    }

    public static final String getHostAddress(){
        try {
            InetAddress ip = InetAddress.getLocalHost();
            return ip.getHostAddress();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static final String getHostName(){
        try {
            InetAddress ip = InetAddress.getLocalHost();
            return ip.getHostName();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static final String getHostName(String interfaceName) throws SocketException {
        String hostName = null;
        Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
        while (interfaces.hasMoreElements()) {
            NetworkInterface nic = interfaces.nextElement();
            Enumeration<InetAddress> addresses = nic.getInetAddresses();
            while (addresses.hasMoreElements()) {
                InetAddress address = addresses.nextElement();
                logger.info(nic.getDisplayName()+" "+address.getHostName()+" "+address.getHostAddress());
                if (!address.isLoopbackAddress()) {
                    hostName = address.getHostName();
                    //address.getHostAddress();
                }
            }
        }

        return hostName;
    }
}
