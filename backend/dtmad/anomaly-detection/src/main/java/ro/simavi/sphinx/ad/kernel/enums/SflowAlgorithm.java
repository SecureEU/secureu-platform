package ro.simavi.sphinx.ad.kernel.enums;

public enum SflowAlgorithm implements AdmlAlgorithmEnum{

    //TOP_TALKERS("topTalkers"),
    SMTP_TALKERS("SMTPTalkers"),
    P2P("p2p"),
    MEDIA_STREAMING("mediaStreaming"),
    ATYPICAL_PORTS("atypicalPorts"),
    ATYPICAL_ALIEN_PORTS("atypicalAlienPorts"),
    ATYPICAL_PAIRS("atypicalPairs"),
    ATYPICAL_DATA("atypicalData"),
    ALIEN("alien"),
    UDP_AMPLIFIER("UDPAmplifier"),
    ABUSED_SMTP("abusedSMTP"),
    DNS_TUNNEL("dnsTunnel"),
    ICMP_TUNNEL("ICMPTunnel"),
    HORIZONTAL_PORT_SCAN("hPortScan"),
    VERTICAL_PORT_SCAN("vPortScan"),
   // DDOS("DDoS"),
    BOT_NET("BotNet");

    String name;

    SflowAlgorithm(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

}
