package ro.simavi.sphinx.ad.model.simulation;

public enum SflowTestedAlgorithm{

    SMTP_TALKERS("SMTPTalkers", true, "test_SMTP_Talkers"),
    P2P("p2p",true,"test_P2PComunication"),
    MEDIA_STREAMING("mediaStreaming",true, "test_MediaStreamingClient"),
    ATYPICAL_PORTS("atypicalPorts",true, "test_AtypicalTCPPortUsed"),
    ATYPICAL_ALIEN_PORTS("atypicalAlienPorts", true, "test_AtypicalAlienTCPPortUsed"),
    ATYPICAL_PAIRS("atypicalPairs",true, "test_AtypicalNumberOfPairsInThePeriod"),
    ATYPICAL_DATA("atypicalData", true, "test_AtypicalAmountOfDataTransfered"),
    ALIEN("alien",true, "test_AlienAccessingManyHosts"),
    UDP_AMPLIFIER("UDPAmplifier",true, "test_UDPAmplifier"),
    ABUSED_SMTP("abusedSMTP",true,"test_AbusedSMTPServer"),
    DNS_TUNNEL("dnsTunnel",true, "test_DnsTunnel"),
    ICMP_TUNNEL("ICMPTunnel",true, "test_ICMPTunnel"),
    HORIZONTAL_PORT_SCAN("hPortScan",true, "test_HorizontalPortScan"),
    VERTICAL_PORT_SCAN("vPortScan",true,  "test_VerticalPortScan"),
    BOT_NET("BotNet",true, "test_CC_BotNets");

    String name;

    Boolean tested;

    String prefix;

    String disabledProperty;

    SflowTestedAlgorithm(String name, Boolean tested, String prefix) {
        this.name = name;
        this.tested = tested;
        this.prefix = prefix;
    }

    public String getName() {
        return name;
    }

    public Boolean isTested() {
        return tested;
    }

    public String getPrefix() {
        return prefix;
    }
}
