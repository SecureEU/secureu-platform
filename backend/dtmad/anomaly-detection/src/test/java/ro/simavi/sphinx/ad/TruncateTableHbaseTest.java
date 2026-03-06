package ro.simavi.sphinx.ad;

import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import ro.simavi.sphinx.ad.kernel.hbase.AdmlHBaseRDD;

@RunWith(SpringRunner.class)
@SpringBootTest
public class TruncateTableHbaseTest {

    @Test
    public void test(){

        // clear hbase
        AdmlHBaseRDD admlHBaseRDD = AdmlHBaseRDD.getInstance();
        if (!admlHBaseRDD.isOk()) {
            return;
        }
        //  sau din hbase shell:
        // truncate 'adml_sflows'
        admlHBaseRDD.truncateAdmlFlows("adml_sflows");

        // logstash -f C:\tools\logstash\config\logstashNf-folder.conf
    }
}
