package ro.simavi.sphinx.ad.handlers;

import ro.simavi.sphinx.ad.kernel.model.AdmlAlert;

import java.io.Serializable;
import java.util.List;

public interface AlertHandler extends Serializable {

    void onAlert(AdmlAlert admlAlert);

    List<AdmlAlert> getList();

    void clearList();
}
