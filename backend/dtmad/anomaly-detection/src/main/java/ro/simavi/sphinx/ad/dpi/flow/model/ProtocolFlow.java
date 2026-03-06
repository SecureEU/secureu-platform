package ro.simavi.sphinx.ad.dpi.flow.model;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import ro.simavi.sphinx.model.event.*;

import java.io.Serializable;
import java.time.LocalDateTime;

////Delete
@Getter
@Setter
//@JsonSubTypes({
//        @JsonSubTypes.Type(value = DnsFlow.class, name = "dns"),
//        @JsonSubTypes.Type(value = HttpFlow.class, name = "http") ,
//})
//@JsonTypeInfo(
//        use = JsonTypeInfo.Id.NAME,
//        include = JsonTypeInfo.As.PROPERTY,
//        property = "detectedProtocol")
@ToString
public class ProtocolFlow implements Serializable {

    private String id; // e foarte important!!!!

    private String detectedProtocol; // udp, etc

    private long lowerPort; // nu prea e important... pentru ca dns + http au porturi standard... si vin distinct in suricata

    private long upperPort; // nu prea e important... pentru ca dns + http au porturi standard... si vin distinct in suricata

    private String upperIp; // nu sunt asa importate, poate doar in histograme

    private String lowerIp; // nu sunt asa importate, poate doar in histograme

//    private long eventPriorityId; //? am putea sa presupunem ca e 1 tot timpul ...
    private long ipProtocol;

    private long flowDuration;

    private long bytes;

    private long packets;

    private long packetsWithoutPayload;

    private double avgPacketSize;

    private long minPacketSize;

    private long maxPacketSize;

    private double avgInterTime;

    private long packetSize0;

    private long interTime0 = 0;

    private long packetSize1;

    // for http more
    private long interTime1;

    private long packetSize2;

    private long interTime2;

    private long packetSize3;

    private long interTime3;

    private long packetSize4;

    private long interTime4;

    private String hostname;

    private String dnaType;

    private LocalDateTime timeStamp2;

    private Boolean malware = Boolean.FALSE;

    // flags ->
    //  tcp flags
    //  dns flags: e intr-un answer (eve.json)
    //
    private long flags;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDetectedProtocol() {
        return detectedProtocol;
    }

    public void setDetectedProtocol(String detectedProtocol) {
        this.detectedProtocol = detectedProtocol;
    }

    public long getLowerPort() {
        return lowerPort;
    }

    public void setLowerPort(long lowerPort) {
        this.lowerPort = lowerPort;
    }

    public long getUpperPort() {
        return upperPort;
    }

    public void setUpperPort(long upperPort) {
        this.upperPort = upperPort;
    }

    public String getUpperIp() {
        return upperIp;
    }

    public void setUpperIp(String upperIp) {
        this.upperIp = upperIp;
    }

    public String getLowerIp() {
        return lowerIp;
    }

    public void setLowerIp(String lowerIp) {
        this.lowerIp = lowerIp;
    }

    public long getIpProtocol() {
        return ipProtocol;
    }

    public void setIpProtocol(long ipProtocol) {
        this.ipProtocol = ipProtocol;
    }

    public long getFlowDuration() {
        return flowDuration;
    }

    public void setFlowDuration(long flowDuration) {
        this.flowDuration = flowDuration;
    }

    public long getBytes() {
        return bytes;
    }

    public void setBytes(long bytes) {
        this.bytes = bytes;
    }

    public long getPackets() {
        return packets;
    }

    public void setPackets(long packets) {
        this.packets = packets;
    }

    public long getPacketsWithoutPayload() {
        return packetsWithoutPayload;
    }

    public void setPacketsWithoutPayload(long packetsWithoutPayload) {
        this.packetsWithoutPayload = packetsWithoutPayload;
    }

    public double getAvgPacketSize() {
        return avgPacketSize;
    }

    public void setAvgPacketSize(double avgPacketSize) {
        this.avgPacketSize = avgPacketSize;
    }

    public long getMinPacketSize() {
        return minPacketSize;
    }

    public void setMinPacketSize(long minPacketSize) {
        this.minPacketSize = minPacketSize;
    }

    public long getMaxPacketSize() {
        return maxPacketSize;
    }

    public void setMaxPacketSize(long maxPacketSize) {
        this.maxPacketSize = maxPacketSize;
    }

    public double getAvgInterTime() {
        return avgInterTime;
    }

    public void setAvgInterTime(double avgInterTime) {
        this.avgInterTime = avgInterTime;
    }

    public long getPacketSize0() {
        return packetSize0;
    }

    public void setPacketSize0(long packetSize0) {
        this.packetSize0 = packetSize0;
    }

    public long getInterTime0() {
        return interTime0;
    }

    public void setInterTime0(long interTime0) {
        this.interTime0 = interTime0;
    }

    public long getPacketSize1() {
        return packetSize1;
    }

    public void setPacketSize1(long packetSize1) {
        this.packetSize1 = packetSize1;
    }

    public long getInterTime1() {
        return interTime1;
    }

    public void setInterTime1(long interTime1) {
        this.interTime1 = interTime1;
    }

    public long getPacketSize2() {
        return packetSize2;
    }

    public void setPacketSize2(long packetSize2) {
        this.packetSize2 = packetSize2;
    }

    public long getInterTime2() {
        return interTime2;
    }

    public void setInterTime2(long interTime2) {
        this.interTime2 = interTime2;
    }

    public long getPacketSize3() {
        return packetSize3;
    }

    public void setPacketSize3(long packetSize3) {
        this.packetSize3 = packetSize3;
    }

    public long getInterTime3() {
        return interTime3;
    }

    public void setInterTime3(long interTime3) {
        this.interTime3 = interTime3;
    }

    public long getPacketSize4() {
        return packetSize4;
    }

    public void setPacketSize4(long packetSize4) {
        this.packetSize4 = packetSize4;
    }

    public long getInterTime4() {
        return interTime4;
    }

    public void setInterTime4(long interTime4) {
        this.interTime4 = interTime4;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public String getDnaType() {
        return dnaType;
    }

    public void setDnaType(String dnaType) {
        this.dnaType = dnaType;
    }

    public LocalDateTime getTimeStamp2() {
        return timeStamp2;
    }

    public void setTimeStamp2(LocalDateTime timeStamp2) {
        this.timeStamp2 = timeStamp2;
    }

    public Boolean getMalware() {
        return malware;
    }

    public void setMalware(Boolean malware) {
        this.malware = malware;
    }

    public long getFlags() {
        return flags;
    }

    public void setFlags(long flags) {
        this.flags = flags;
    }
}
