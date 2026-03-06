package ro.simavi.sphinx.ad.services;

import java.io.Serializable;

public interface ReputationService extends Serializable {

    void updateCnCList();

    void updateReposList();

    void update();
}
