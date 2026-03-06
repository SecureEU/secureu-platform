package ro.simavi.sphinx.util;

public class PackageFields {

    public static final int FRAME_NUMBER = 0;
    public static final int FRAME_TIME_DELTA = 1;
    public static final int FRAME_TIME = 2;

    public static final int FRAME_INTERFACE_NAME = 3;
    public static final int FRAME_INTERFACE_ID = 4;
    public static final int FRAME_INTERFACE_DESCRIPTION = 5;

    public static final int FRAME_CAP_LEN = 6;
    public static final int FRAME_LEN = 7;
    public static final int FRAME_PROTOCOLS = 8;
    public static final int ETH_SRC = 9;
    public static final int ETH_DST = 10;

    public static final int IP_SRC = 11;
    public static final int IP_DST = 12;
    public static final int IP = 13;
    public static final int IP_PROTO = 14;
    public static final int IP_SRC_HOST = 15;
    public static final int IP_DST_HOST = 16;

    public static final int TCP_PORT = 17;
    public static final int UDP_PORT = 18;
    public static final int IPV6 = 19;
    public static final int IPV6_ADDR = 20;
    public static final int IPV6_SRC = 21;
    public static final int IPV6_DST = 22;

    public static final int HTTP_HOST = 23;
    public static final int DNS_QRY_NAME = 24;

    public static final int TCP_STREAM = 25;
    public static final int TCP_SRC_PORT = 26;
    public static final int TCP_DST_PORT = 27;
    public static final int UDP_SCR_PORT= 28;
    public static final int UDP_DST_PORT = 29;

    public static final int WS_COL_INFO = 30;

    public static final int USERNAME = 31;

    public static final int HOSTNAME = 32;

    public static final int FILTER_CODE = 33;

    public static final int FROM_PCAP=34;

    public static final int PCAP_FILE=35;

    public final static String[] fields = new String[]{
            "frame.number", //0
            "frame.time_delta", //1
            "frame.time", //2
            "frame.interface_name", //3
            "frame.interface_id", //4
            "frame.interface_description", //5
            "frame.cap_len", //6
            "frame.len", //7
            "frame.protocols", //8
            "eth.src", //9
            "eth.dst", //10
            "ip.src", //11
            "ip.dst", //12
            "ip", //13
            "ip.proto", //14
            "ip.src_host", //15
            "ip.dst_host",//16
            "tcp.port",//17
            "udp.port",//18
            "ipv6", // 19
            "ipv6.addr", // 20
            "ipv6.src", // 21
            "ipv6.dst",// 22
            "http.host", // 23
            "dns.qry.name", // 24
            "tcp.stream", //25
            "tcp.srcport", // 26
            "tcp.dstport", //27
            "udp.srcport", // 28
            "udp.dstport", //29
            "_ws.col.info", //30

            "username", //31
            "hostname", //32
            "filterCode" //33
    };

}
