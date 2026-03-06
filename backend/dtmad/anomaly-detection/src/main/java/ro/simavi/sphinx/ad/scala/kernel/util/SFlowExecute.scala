package ro.simavi.sphinx.ad.scala.kernel.util

/*
* Copyright (C) 2015-2016 Paulo Angelo Alves Resende <pa@pauloangelo.com>
*
* This program is free software; you can redistribute it and/or modify
* it under the terms of the GNU General Public License Version 2 as
* published by the Free Software Foundation.  You may not use, modify or
* distribute this program under any other version of the GNU General
* Public License.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program; if not, write to the Free Software
* Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
*/

import com.typesafe.config.Config
import org.apache.spark.SparkContext
import ro.simavi.sphinx.ad.scala.kernel.hbase.HogHBaseRDD
import ro.simavi.sphinx.ad.scala.kernel.kafka.HogKafka
import ro.simavi.sphinx.ad.scala.kernel.sflow.HogSFlow
import ro.simavi.sphinx.ad.services.MessagingSystemService

/**
 * 
 * Keep it useful, simple, robust, and scalable.
 * 
 * 
 */
class SFlowExecute {

  def run(config : Config, spark: SparkContext, messagingSystemService: MessagingSystemService, hbaseZookeeperQuorum: String, hbaseZookeeperClientPort: Int):Int=
  {

    HogKafka.setMessagingSystemService(messagingSystemService)

    HogHBaseRDD.setHbaseZookeeperQuorum(hbaseZookeeperQuorum)
    HogHBaseRDD.setHbaseZookeeperClientPort(hbaseZookeeperClientPort)

    val HogRDDSFlow = HogHBaseRDD.connectSFlow(spark)
    val hogSFlow = new HogSFlow(config)

    HogGlobalParameter.count = 0

    hogSFlow.run(HogRDDSFlow,spark)

    return HogGlobalParameter.count

  }
  
}