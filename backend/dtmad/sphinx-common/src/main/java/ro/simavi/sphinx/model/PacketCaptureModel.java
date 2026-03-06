package ro.simavi.sphinx.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PacketCaptureModel {

    private String frame;

    private String frameNumber;

    private String ethSrc;

    private String ethDst;

    private String ipSrc;

    private String ipDst;

    private String frameLen;

    private String frameTime;

    private String frameTimeDelta;

    private String ip;

    private String ipProto;

    private String ipSrcHost;

    private String ipDstHost;

}
