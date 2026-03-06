package ro.simavi.sphinx.ad.services.rest;

import ro.simavi.sphinx.model.PacketCaptureListModel;
import ro.simavi.sphinx.model.SphinxFilter;
import ro.simavi.sphinx.model.SphinxPage;

/**
 * •	AD.I.02: Abnormal and Suspicious Traffic Activity Interface
 * This interface allows the AD to receive information on suspicious traffic data (data traffic packets) from the DTM component to perform anomaly detection analyses.
 *
 * The AD component collects suspicious data traffic and information from the DTM and processes it to identify anomalies.
 *
 * The AD component collects data and information on potential cyber-attacks from the HP and processes it to identify anomalies (new and advanced threats).
 */
public interface InfoService {

    public SphinxPage<PacketCaptureListModel> getAbnormalAndSuspiciousData(SphinxFilter sphinxFilter);

    /* de stabilit cum ne trimite HP datele*/
    public void getPotentialCyberAttacksData();
}
