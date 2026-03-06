package ro.simavi.sphinx.dtm.services;

import ro.simavi.sphinx.dtm.model.NetworkInterfaceModel;

import java.io.IOException;
import java.util.List;

public interface NetworkInterfaceService {

    List<NetworkInterfaceModel> getNetworkInterfaceModelList() throws IOException;

    NetworkInterfaceModel getNetworkInterfaceModel(String fullName);

}
