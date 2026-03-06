package ro.simavi.sphinx.ad.scala.kernel.kafka

import ro.simavi.sphinx.ad.kernel.model.AdmlAlert
import ro.simavi.sphinx.ad.services.MessagingSystemService

object HogKafka {

  var messagingSystemService: MessagingSystemService = null

  def setMessagingSystemService(messagingSystemService_1:MessagingSystemService)={
    messagingSystemService = messagingSystemService_1;
  }

  def sendAlert(alert:AdmlAlert)={
    if (messagingSystemService!=null){
      messagingSystemService.sendAlert(alert)
    }

  }

}
