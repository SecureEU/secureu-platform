package ro.simavi.sphinx.ad.dpi.flow.enums;

import lombok.Getter;

import java.util.Map;

@Getter
public enum RRType {

    A(1,"Address, 32-bit IPv4 address"),
    AAAA(28,"4A, 128-bit IPv6 address (cf. also A6 and RFC3363)"),
    CNAME(5,"Canonical NAME, name alias"),
    HINFO(13,"HostINFO, CPU and OS description"),
    MX(15,"Mail eXchange, SMTP servers related to a mail address"),
    NS(2,"Name Server, authoritative DNS servers for a zone"),
    PTR(12,"PoinTeR, link between an address under .in-addr.arpa. and an host name"),
    SOA(6,"Start Of Authority, zone database description"),
    SRV(33, "SeRVer selection, references for well-known services : www, ldap, ... (updates WKS)"),
    TXT(16,"TeXT string, free ASCII description (max 255 char.)");

    private int code;

    private String description;

    RRType(int code, String description){
        this.code = code;
        this.description = description;
    }

}
