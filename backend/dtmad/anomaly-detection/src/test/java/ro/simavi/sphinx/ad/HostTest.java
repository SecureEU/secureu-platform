package ro.simavi.sphinx.ad;

//import org.junit.jupiter.api.Test;

import inet.ipaddr.IPAddress;
import inet.ipaddr.IPAddressString;
import org.junit.Test;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class HostTest {


    @Test
    public void test() throws UnknownHostException {

        InetAddress[] iaddress
                = InetAddress.getAllByName("proxy.mythic-beasts.com");

        for (InetAddress ipaddresses : iaddress) {
            System.out.println(ipaddresses.toString());
        }

        IPAddressString addrString = new IPAddressString("2001:db8:0:1234:0:567:8:1");
        IPAddress addr = addrString.getAddress();
        System.out.println(addr.toFullString());
    }

}
