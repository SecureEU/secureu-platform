package ro.simavi.sphinx.ad.handlers;

import ro.simavi.sphinx.ad.kernel.model.AdmlAlert;

import java.util.List;

public class PrintAlertHandler implements AlertHandler {
    @Override
    public void onAlert(AdmlAlert admlAlert) {
        System.out.println(admlAlert);
    }

    @Override
    public List<AdmlAlert> getList() {
        return null;
    }

    @Override
    public void clearList() {

    }
}
