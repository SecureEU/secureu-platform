package ro.simavi.sphinx.ad;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import ro.simavi.sphinx.ad.configuration.AdConfigProps;

import java.io.File;
import java.io.FilenameFilter;


//@RunWith(SpringRunner.class)
//@SpringBootTest
public class SimulationTest {

  //  @Autowired
  //  private AdConfigProps adConfigProps;

    @Test
    public void simulation(){
        String path = "C:\\work\\SPHINX\\anomaly-detection\\src\\test\\resources\\sflow";

        FilenameFilter filter = new FilenameFilter() {
            @Override
            public boolean accept(File f, String name) {
                return name.endsWith(".csv");
            }
        };

        File f = new File(path);

        String [] pathnames = f.list(filter);

        for (String pathname : pathnames) {
            System.out.println(pathname);
        }
    }
}
