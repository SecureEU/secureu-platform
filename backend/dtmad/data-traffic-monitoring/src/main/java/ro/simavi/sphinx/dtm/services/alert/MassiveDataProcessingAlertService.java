package ro.simavi.sphinx.dtm.services.alert;

import ro.simavi.sphinx.dtm.model.ConversationsModel;
import ro.simavi.sphinx.dtm.services.DTMAlertComponentService;

import java.util.HashMap;

public interface MassiveDataProcessingAlertService extends DTMAlertComponentService {

    HashMap<String,ConversationsModel> getConversations();

    void clearConversations();

    long getThresholdAlertValue();

}
