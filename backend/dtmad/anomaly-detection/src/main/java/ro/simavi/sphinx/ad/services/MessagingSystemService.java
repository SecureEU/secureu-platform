package ro.simavi.sphinx.ad.services;

import ro.simavi.sphinx.ad.kernel.model.AdmlAlert;

import java.io.Serializable;

public interface MessagingSystemService extends Serializable {

    void sendAlert(AdmlAlert alertModel);
}
