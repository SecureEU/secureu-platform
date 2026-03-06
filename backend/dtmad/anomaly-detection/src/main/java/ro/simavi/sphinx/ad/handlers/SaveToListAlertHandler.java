package ro.simavi.sphinx.ad.handlers;

import org.springframework.stereotype.Component;
import ro.simavi.sphinx.ad.kernel.model.AdmlAlert;

import java.util.ArrayList;
import java.util.List;

@Component
public class SaveToListAlertHandler implements AlertHandler {

    private List<AdmlAlert> list = new ArrayList<>();

    @Override
    public void onAlert(AdmlAlert admlAlert) {
        System.out.println(admlAlert);
        list.add(admlAlert);
    }

    @Override
    public List<AdmlAlert> getList() {
        return list;
    }

    @Override
    public void clearList() {
        list.clear();
    }
}
