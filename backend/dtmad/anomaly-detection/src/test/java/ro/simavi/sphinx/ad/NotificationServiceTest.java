package ro.simavi.sphinx.ad;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import ro.simavi.sphinx.model.SphinxNotification;
import ro.simavi.sphinx.model.SphinxNotificationWarningLevel;

import java.util.Date;

@SpringBootTest
public class NotificationServiceTest {

   // @Autowired
   // private NotificationService notificationService;

    @Test
    void testMessage() {
        String message = "AD Alert created: "+ new Date();
  //      notificationService.alert(message);
    }

    @Test
    void testNotification() {
        SphinxNotification sphinxNotification = new SphinxNotification();
        sphinxNotification.setTitle("Alert");
        sphinxNotification.setContent("AD Alert created");
        sphinxNotification.setWarningLevel(SphinxNotificationWarningLevel.ALERT);

  //      notificationService.alert(sphinxNotification);
    }

}
