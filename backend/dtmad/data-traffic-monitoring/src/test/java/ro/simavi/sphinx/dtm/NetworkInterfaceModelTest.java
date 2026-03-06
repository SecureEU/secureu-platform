package ro.simavi.sphinx.dtm;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collections;
import java.util.Enumeration;

import static java.lang.System.out;
public class NetworkInterfaceModelTest {

    //@Test
    public void test() throws SocketException {
        Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();
        for (NetworkInterface netint : Collections.list(nets)) {
            displayInterfaceInformation(netint);
        }
    }

    static void displayInterfaceInformation(NetworkInterface netint) throws SocketException {
        if (!netint.isUp()){
            return;
        }
        if (netint.isLoopback()){
            return;
        }
        out.printf("Display name: %s\n", netint.getDisplayName());
        out.printf("Name: %s\n", netint.getName());
        out.printf("Index: %s\n", netint.getIndex());
      //  out.printf("isLoopback: %s\n",netint.isLoopback());
      //  out.printf("isVirtual: %s\n",netint.isVirtual());
      //  out.printf("isUp: %s\n",netint.isUp());
      //  out.printf("isPointToPoint: %s\n",netint.isPointToPoint());
        Enumeration<InetAddress> inetAddresses = netint.getInetAddresses();
        for (InetAddress inetAddress : Collections.list(inetAddresses)) {
            out.printf("InetAddress: %s\n", inetAddress);
        }
        out.printf("\n");
    }
}
