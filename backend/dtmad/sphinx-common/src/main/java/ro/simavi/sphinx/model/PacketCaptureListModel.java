package ro.simavi.sphinx.model;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class PacketCaptureListModel {

    List<PacketCaptureModel> packetCaptureModels;
}
