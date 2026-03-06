package ro.simavi.sphinx.ad;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import ro.simavi.sphinx.ad.services.rest.InfoService;
import ro.simavi.sphinx.model.PacketCaptureListModel;
import ro.simavi.sphinx.model.SphinxFilter;
import ro.simavi.sphinx.model.SphinxPage;

@SpringBootTest
public class InfoServiceTest {

    @Autowired
    private InfoService infoService;

    @Test
    void test() {
        SphinxFilter sphinxFilter = new SphinxFilter();
        sphinxFilter.setFirstId(5);
        //SphinxPage<PacketCaptureListModel> captureListModelSphinxPage = infoService.getAbnormalAndSuspiciousData(sphinxFilter);
    }
}
