package ro.simavi.sphinx.ad;

//import org.junit.jupiter.api.Test;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import ro.simavi.sphinx.ad.services.ReputationService;

@RunWith(SpringRunner.class)
@SpringBootTest
public class ReputationTest {

//    @Autowired
//    private AdConfigProps adConfigProps;
//
//    @Autowired
//    private SparkContext sparkContext;
//
//    @Autowired
//    private AdConfigService adConfigService;
//
//    @Autowired
//    private MessagingSystemService messagingSystemService;
//
    @Autowired
    private ReputationService reputationService;

    @Test
    public void test(){
    //    reputationService.update();
    }
}
