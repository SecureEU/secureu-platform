package ro.simavi.sphinx.ad.scala.kernel

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
import ro.simavi.sphinx.ad.scala.kernel.initiate.HogInitiate
import ro.simavi.sphinx.ad.scala.kernel.kafka.HogKafka
import ro.simavi.sphinx.ad.scala.kernel.sflow.HogSFlow
import ro.simavi.sphinx.ad.scala.kernel.util.HogGlobalParameter
import ro.simavi.sphinx.ad.services.MessagingSystemService

/**
 * 
 * Keep it useful, simple, robust, and scalable.
 * 
 * 
 */
class SFlowMain {

  def run(config : Config, spark: SparkContext, messagingSystemService: MessagingSystemService, hbaseZookeeperQuorum: String, hbaseZookeeperClientPort: Int):Int=
  {

//    val sparkConf = new SparkConf()
//                          .setMaster("local[*]")
//                          .setAppName("AD-ML")
//                          .set("spark.executor.memory", "4g")
//                          .set("spark.default.parallelism", "160") // 160
//
//    val spark = new SparkContext(sparkConf)

    // init hog kafka
    HogKafka.setMessagingSystemService(messagingSystemService)

    // Get the HBase RDD
    //val hogHBaseRDD = new HogHBaseRDD();
    HogHBaseRDD.setHbaseZookeeperQuorum(hbaseZookeeperQuorum)
    HogHBaseRDD.setHbaseZookeeperClientPort(hbaseZookeeperClientPort)
    HogHBaseRDD.connect(spark);
    HogHBaseRDD.saveSignature();

    // Initiate HogZilla
    HogInitiate.initiate(spark);

    // Prepare the data
    //HogPrepare.prepare(HogRDD) //todo: decoment

    // ============================ Run algorithms for SFlows ============================
    
    val HogRDDSFlow = HogHBaseRDD.connectSFlow(spark);
    val hogSFlow = new HogSFlow(config);
    HogGlobalParameter.count = 0;
    hogSFlow.run(HogRDDSFlow,spark);

    // Stop Spark
   // spark.stop()
    
    // Close the HBase Connection
   // HogHBaseRDD.close();

    return HogGlobalParameter.count;

  }
  
}