package ro.simavi.sphinx.ad;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import ro.simavi.sphinx.ad.kernel.hbase.AdmlHBaseRDD;

import java.util.Date;

@RunWith(SpringRunner.class)
@SpringBootTest
public class ClearHbaseTest {

    @Test
    public void dnsKmennsMean(){
        long start = new Date().getTime();

        // clear hbase
        AdmlHBaseRDD admlHBaseRDD = AdmlHBaseRDD.getInstance();
        if (!admlHBaseRDD.isOk()) {
            return;
        }

        long end = new Date().getTime();
        System.out.println("Time:"+(end-start));
    }
}
