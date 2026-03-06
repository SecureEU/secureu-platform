package ro.simavi.sphinx.ad;

import com.typesafe.config.Config;
import org.apache.spark.SparkContext;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.junit4.SpringRunner;
import ro.simavi.sphinx.ad.configuration.AdConfigProps;
import ro.simavi.sphinx.ad.kernel.enums.AdTools;
import ro.simavi.sphinx.ad.scala.kernel.SFlowMain;
import ro.simavi.sphinx.ad.services.AdConfigService;
import ro.simavi.sphinx.ad.services.MessagingSystemService;

import java.util.Map;

@RunWith(SpringRunner.class)
@SpringBootTest
public class SFlowScalaTest {

    @Autowired
    private AdConfigProps adConfigProps;

    @Autowired
    private SparkContext sparkContext;

    @Autowired
    private AdConfigService adConfigService;

    @Autowired
    private MessagingSystemService messagingSystemService;

    @Test
    public void test(){

        Environment environment = SpringContext.getBean(Environment.class);
        String hbaseZookeeperQuorum = environment.getProperty("hbase.zookeeper.quorum");
        String clientPortString = environment.getProperty("hbase.zookeeper.property.clientPort");
        Integer hbaseZookeeperClientPort = 2181;
        try{
            hbaseZookeeperClientPort = Integer.parseInt(clientPortString);
        }catch (Exception e){

        }

        Map<String, Object> map = adConfigProps.getMap(AdTools.SFLOW);
        Config config = adConfigService.getMergeConfig(map);

        SFlowMain sFlowMain = new SFlowMain();
        int count = sFlowMain.run(config, sparkContext, messagingSystemService, hbaseZookeeperQuorum, hbaseZookeeperClientPort );
        System.out.println(count);
    }
}
