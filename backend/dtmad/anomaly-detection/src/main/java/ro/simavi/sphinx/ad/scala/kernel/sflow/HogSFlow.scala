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
/**
 *  REFERENCES:
 *   - http://ids-hogzilla.org/xxx/826000101
 */


package ro.simavi.sphinx.ad.scala.kernel.sflow

import java.io.File
import java.net.InetAddress
import java.sql.Timestamp
import java.time.LocalDateTime

import org.apache.hadoop.hbase.client.Scan
import org.apache.hadoop.hbase.util.Bytes
import org.apache.spark.SparkContext
import org.apache.spark.rdd.{PairRDDFunctions, RDD}
import org.apache.spark.rdd.RDD.rddToPairRDDFunctions
import ro.simavi.sphinx.ad.scala.kernel.event.{HogEvent, HogSignature}
import ro.simavi.sphinx.ad.scala.kernel.hbase.{HogHBaseHistogram, HogHBaseInventory, HogHBaseRDD, HogHBaseReputation}
import ro.simavi.sphinx.ad.scala.kernel.histogram.{Histograms, HogHistogram}
import ro.simavi.sphinx.ad.scala.kernel.util.{HogConfig, HogFlow, HogGlobalParameter}

//import com.typesafe.scalalogging.Logger
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.mutable.{HashMap, HashSet, Map}
import scala.math.{floor, log}
import com.typesafe.config.{Config, ConfigFactory}
import ro.simavi.sphinx.ad.kernel.model.algorithm.AlgorithmDetails
import ro.simavi.sphinx.ad.kernel.model.algorithm.sflow._

/**
 * 
 */
class HogSFlow extends Serializable() {

  val logger: Logger = LoggerFactory.getLogger(classOf[HogSFlow])

  var alienThreshold: Int = 20
  var SMTPTalkersThreshold: Long = 20971520L
  var atypicalTCPPort: Set[String] = Set("80", "443", "587", "465", "993", "995")
  var AtypicalTCPMinPkts: Int = 2
  var atypicalPairsThresholdMIN: Int = 300
  var atypicalAmountDataThresholdMIN: Long = 5737418240L
  var AtypicalAlienTCPMinPkts: Int = 2
  var p2pPairsThreshold: Int = 5
  var p2pMyPortsThreshold : Int =4
  var abusedSMTPBytesThreshold: Long = 50000000L
  var p2pBytes2ndMethodThreshold: Long = 10000000L
  var p2pPairs2ndMethodThreshold: Int = 10
  var p2pDistinctPorts2ndMethodThreshold : Int = 10
  var mediaClientCommunicationDurationThreshold : Int = 300
  var mediaClientCommunicationDurationMAXThreshold: Int = 7200
  var mediaClientPairsThreshold = p2pPairs2ndMethodThreshold
  var mediaClientUploadThreshold :Long = 10000000L
  var mediaClientDownloadThreshold:Long = 10000000L
  var mediaClientExcludedPorts:  Set[String] = Set("1194")
  var dnsTunnelThreshold :Long = 50000000L
  var bigProviderThreshold :Long = 1073741824L
  var icmpTunnelThreshold : Int = 200
  var icmpTotalTunnelThreshold :Long = 100000000L
  var hPortScanMinFlowsThreshold : Int = 100
  var hPortScanExceptionPorts : Set[String] = Set("80", "443", "53")
  var hPortScanExceptionInternalPorts :Set[String] = Set("123")
  var vPortScanMinPortsThreshold: Int = 3
  var vPortScanPortIntervalThreshold: Int = 1024
  var ddosMinConnectionsThreshold: Int = 50
  var ddosMinPairsThreshold: Int = 20
  var ddosExceptionAlienPorts: Set[String] = Set("80", "443", "587", "465", "993", "995")
  var FlowListLimit: Int = 1000
  var CCminPktsPerFlow : Int = 20

  var disable_abusedSMTP: Int = 0
  var disable_alien: Int = 0
  var disable_atypicalAlienPorts: Int = 0
  var disable_atypicalData: Int = 0
  var disable_atypicalPairs: Int = 0
  var disable_atypicalPorts: Int = 0
  var disable_BotNet: Int = 0
  var disable_DDoS : Int = 1
  var disable_UDPAmplifier: Int = 0
  var disable_dnsTunnel: Int = 0
  var disable_hPortScan : Int = 0
  var disable_ICMPTunnel: Int = 0
  var disable_mediaStreaming : Int = 0
  var disable_p2p : Int = 0
  var disable_SMTPTalkers : Int = 0
  //var disable_topTalkers : Int = 0
  var disable_vPortScan: Int = 0

  var generalExcludedIPs: Set[String] = Set()
  var abusedSMTPExcludedIPs : Set[String] = Set()
  var alienExcludedIPs : Set[String] = Set()
  var atypicalAlienPortsExcludedIPs : Set[String] = Set()
  var atypicalDataExcludedIPs: Set[String] = Set()
  var atypicalPairsExcludedIPs: Set[String] = Set()
  var atypicalPortsExcludedIPs: Set[String] = Set()
  var BotNetExcludedIPs: Set[String] = Set()
  var DDoSExcludedIPs : Set[String] = Set()
  var dnsTunnelExcludedIPs: Set[String] = Set()
  var hPortScanExcludedIPs: Set[String] = Set()
  var ICMPTunnelExcludedIPs: Set[String] = Set()
  var mediaStreamingExcludedIPs : Set[String] = Set()
  var p2pExcludedIPs : Set[String] = Set()
  var SMTPTalkersExcludedIPs : Set[String] = Set()
 // var topTalkersExcludedIPs : Set[String] = Set()
  var vPortScanExcludedIPs: Set[String] = Set()
  var UDPAmplifierExcludedIPs : Set[String] = Set()

//  var signature: (HogSignature,HogSignature,HogSignature,HogSignature,HogSignature,HogSignature,HogSignature,HogSignature,
//    HogSignature,HogSignature,HogSignature,HogSignature,HogSignature,HogSignature,HogSignature,HogSignature,HogSignature)
//  =(null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null)

  var signature = (HogSignature(3, "AD: Top talker identified", 2, 1, 826001001, 826), //1 - disabled
    HogSignature(3, "AD: SMTP talker identified", 1, 1, 826001002, 826), //2 #tested
    HogSignature(3, "AD: Atypical TCP port used", 2, 1, 826001003, 826), //3 #tested
    HogSignature(3, "AD: Atypical alien TCP port used", 2, 1, 826001004, 826), //4 #tested
    HogSignature(3, "AD: Atypical number of pairs in the period", 2, 1, 826001005, 826), //5 #tested
    HogSignature(3, "AD: Atypical amount of data transferred", 2, 1, 826001006, 826), //6 #tested
    HogSignature(3, "AD: Alien accessing too much hosts", 3, 1, 826001007, 826), //7 #tested
    HogSignature(3, "AD: P2P communication", 3, 1, 826001008, 826), //8 #tested
    HogSignature(3, "AD: UDP amplifier (DDoS)", 1, 1, 826001009, 826), //9 #tested
    HogSignature(3, "AD: Abused SMTP Server", 2, 1, 826001010, 826), //10 #tested
    HogSignature(3, "AD: Media streaming client", 3, 1, 826001011, 826), //11 #tested
    HogSignature(3, "AD: DNS Tunnel", 1, 1, 826001012, 826), //12 #tested
    HogSignature(3, "AD: ICMP Tunnel", 1, 1, 826001013, 826), //13 #tested
    HogSignature(3, "AD: Horizontal portscan", 2, 1, 826001014, 826), //14 #tested
    HogSignature(3, "AD: Vertical portscan", 2, 1, 826001015, 826), //15 #tested
    HogSignature(3, "AD: Server under DDoS attack", 1, 1, 826001016, 826), //16 ?  //TODO: Review. You should count the number of equals beginTime and discover why its generating error.
    HogSignature(3, "AD: C&C BotNet communication", 1, 1, 826001017, 826)) //17 #tested

  // Auxiliary Constructor
  def this(defaultConfig:Config)
  {
    this()
    this.init(defaultConfig);

    signature.productIterator
        .map(_.asInstanceOf[HogSignature])
        .foreach(s=>s.saveHBase())
  }

  def init(defaultConfig:Config) {

    ///home/hogzilla/hogzilla/conf/
    logger.info("**Pe metodata init")
    var file = new File("sflow.conf");
    var config:Config = null
    if (file.exists()){
      config = ConfigFactory.parseFile(file)
    }

    if (config==null){
      config = defaultConfig;
    }

    alienThreshold = HogConfig.getInt(config, "alien.minPairs", 20) // number of pairs accessed for a single alien. Alert above it.
    SMTPTalkersThreshold = HogConfig.getLong(config, "SMTPTalkers.minBytes", 20971520L) // (20*1024*1024 = 20M)
    atypicalTCPPort = HogConfig.getSetString(config, "atypicalPorts.excludePorts", Set("80", "443", "587", "465", "993", "995"))
    AtypicalTCPMinPkts = HogConfig.getInt(config, "atypicalPorts.minPacketsPerFlow", 2)
    atypicalPairsThresholdMIN = HogConfig.getInt(config, "atypicalPairs.minPairs", 300)
    atypicalAmountDataThresholdMIN = HogConfig.getLong(config, "atypicalData.minBytes", 5737418240L) //(10*1024*1024*1024 = 5G)
    AtypicalAlienTCPMinPkts = HogConfig.getInt(config, "atypicalAlienPorts.minPacketsPerFlow", 2)
    p2pPairsThreshold = HogConfig.getInt(config, "p2p.minPairs", 5)
    p2pMyPortsThreshold = HogConfig.getInt(config, "p2p.minPorts", 4)
    abusedSMTPBytesThreshold = HogConfig.getLong(config, "abusedSMTP.minBytes", 50000000L)
    //config.getString("abusedSMTPBytesThreshold").toLong // ~50 MB
    p2pBytes2ndMethodThreshold = HogConfig.getLong(config, "p2p.minBytes2nd", 10000000L) // ~10 MB
    p2pPairs2ndMethodThreshold = HogConfig.getInt(config, "p2p.minPairs2nd", 10)
    p2pDistinctPorts2ndMethodThreshold = HogConfig.getInt(config, "p2p.minPorts2nd", 10)
    mediaClientCommunicationDurationThreshold = HogConfig.getInt(config, "mediaStreaming.minDuration", 300) // 5min (300s)
    mediaClientCommunicationDurationMAXThreshold = HogConfig.getInt(config, "mediaStreaming.maxDuration", 7200) // 2h 7200
    mediaClientPairsThreshold = p2pPairs2ndMethodThreshold
    mediaClientUploadThreshold = HogConfig.getLong(config, "mediaStreaming.maxUploadBytes", 10000000L) // ~10MB
    mediaClientDownloadThreshold = HogConfig.getLong(config, "mediaStreaming.minDownloadBytes", 1000000L) // 1MB
    mediaClientExcludedPorts = HogConfig.getSetString(config, "mediaStreaming.excludePorts", Set("1194"))
    dnsTunnelThreshold = HogConfig.getLong(config, "dnsTunnel.minBytes", 50000000L) // ~50 MB
    bigProviderThreshold = HogConfig.getLong(config, "bigProviders.minBytes", 1073741824L) // (1*1024*1024*1024 = 1G)
    icmpTunnelThreshold = HogConfig.getInt(config, "ICMPTunnel.minPacket", 200) // 200b
    icmpTotalTunnelThreshold = HogConfig.getLong(config, "ICMPTunnel.minBytes", 100000000L) // ~100MB
    hPortScanMinFlowsThreshold = HogConfig.getInt(config, "hPortScan.minFlows", 100)
    hPortScanExceptionPorts = HogConfig.getSetString(config, "hPortScan.excludeAlienPorts", Set("80", "443", "53"))
    hPortScanExceptionInternalPorts = HogConfig.getSetString(config, "hPortScan.excludeMyPorts", Set("123"))
    vPortScanMinPortsThreshold = HogConfig.getInt(config, "vPortScan.minPorts", 3)
    vPortScanPortIntervalThreshold = HogConfig.getInt(config, "vPortScan.maxPortNumber", 1024) // 1 to 1023
 //   ddosMinConnectionsThreshold = HogConfig.getInt(config, "DDoS.minFlows", 50) // Over this, can be considered
  //  ddosMinPairsThreshold = HogConfig.getInt(config, "DDoS.minPairs", 20)
  //  ddosExceptionAlienPorts = HogConfig.getSetString(config, "hPortScan.excludeAlienPorts", Set("80", "443", "587", "465", "993", "995"))
    FlowListLimit = HogConfig.getInt(config, "alert.maxFlowList", 1000)
    CCminPktsPerFlow = HogConfig.getInt(config, "BotNet.minPktsPerFlow", 20)

    disable_abusedSMTP = HogConfig.getInt(config, "abusedSMTP.disabled", 0)
    disable_alien = HogConfig.getInt(config, "alien.disabled", 0)
    disable_atypicalAlienPorts = HogConfig.getInt(config, "atypicalAlienPorts.disabled", 0)
    disable_atypicalData = HogConfig.getInt(config, "atypicalData.disabled", 0)
    disable_atypicalPairs = HogConfig.getInt(config, "atypicalPairs.disabled", 0)
    disable_atypicalPorts = HogConfig.getInt(config, "atypicalPorts.disabled", 0)
    disable_BotNet = HogConfig.getInt(config, "BotNet.disabled", 0)
   // disable_DDoS = HogConfig.getInt(config, "DDoS.disabled", 0)
    disable_UDPAmplifier = HogConfig.getInt(config, "UDPAmplifier.disabled", 0)
    disable_dnsTunnel = HogConfig.getInt(config, "dnsTunnel.disabled", 0)
    disable_hPortScan = HogConfig.getInt(config, "hPortScan.disabled", 0)
    disable_ICMPTunnel = HogConfig.getInt(config, "ICMPTunnel.disabled", 0)
    disable_mediaStreaming = HogConfig.getInt(config, "mediaStreaming.disabled", 0)
    disable_p2p = HogConfig.getInt(config, "p2p.disabled", 0)
    disable_SMTPTalkers = HogConfig.getInt(config, "SMTPTalkers.disabled", 0)
    //disable_topTalkers = HogConfig.getInt(config, "topTalkers.disabled", 0)
    disable_vPortScan = HogConfig.getInt(config, "vPortScan.disabled", 0)

    generalExcludedIPs = HogConfig.getSetString(config, "general.excludeIPs", Set())
    abusedSMTPExcludedIPs = HogConfig.getSetString(config, "abusedSMTP.excludeIPs", Set()) ++ generalExcludedIPs
    alienExcludedIPs = HogConfig.getSetString(config, "alien.excludeIPs", Set()) ++ generalExcludedIPs
    atypicalAlienPortsExcludedIPs = HogConfig.getSetString(config, "atypicalAlienPorts.excludeIPs", Set()) ++ generalExcludedIPs
    atypicalDataExcludedIPs = HogConfig.getSetString(config, "atypicalData.excludeIPs", Set()) ++ generalExcludedIPs
    atypicalPairsExcludedIPs = HogConfig.getSetString(config, "atypicalPairs.excludeIPs", Set()) ++ generalExcludedIPs
    atypicalPortsExcludedIPs = HogConfig.getSetString(config, "atypicalPorts.excludeIPs", Set()) ++ generalExcludedIPs
    BotNetExcludedIPs = HogConfig.getSetString(config, "BotNet.excludeIPs", Set()) ++ generalExcludedIPs
    DDoSExcludedIPs = HogConfig.getSetString(config, "DDoS.excludeIPs", Set()) ++ generalExcludedIPs
    dnsTunnelExcludedIPs = HogConfig.getSetString(config, "dnsTunnel.excludeIPs", Set()) ++ generalExcludedIPs
    hPortScanExcludedIPs = HogConfig.getSetString(config, "hPortScan.excludeIPs", Set()) ++ generalExcludedIPs
    ICMPTunnelExcludedIPs = HogConfig.getSetString(config, "ICMPTunnel.excludeIPs", Set()) ++ generalExcludedIPs
    mediaStreamingExcludedIPs = HogConfig.getSetString(config, "mediaStreaming.excludeIPs", Set()) ++ generalExcludedIPs
    p2pExcludedIPs = HogConfig.getSetString(config, "p2p.excludeIPs", Set()) ++ generalExcludedIPs
    SMTPTalkersExcludedIPs = HogConfig.getSetString(config, "SMTPTalkers.excludeIPs", Set()) ++ generalExcludedIPs
 //   topTalkersExcludedIPs = HogConfig.getSetString(config, "topTalkers.excludeIPs", Set()) ++ generalExcludedIPs
    vPortScanExcludedIPs = HogConfig.getSetString(config, "vPortScan.excludeIPs", Set()) ++ generalExcludedIPs
    UDPAmplifierExcludedIPs = HogConfig.getSetString(config, "UDPAmplifier.excludeIPs", Set()) ++ generalExcludedIPs
  }

  /**
   * 
   * 
   * 
   */
  def run(HogRDD: RDD[(org.apache.hadoop.hbase.io.ImmutableBytesWritable,org.apache.hadoop.hbase.client.Result)],spark:SparkContext)
  {
   // TopTalkers, SMTP Talkers, XXX: Organize it!
    logger.info("** Pe metoda run ")
   top(HogRDD,spark)
 
  }
  
  /* Disabled
  def populateTopTalker(event:HogEvent):HogEvent =
  {
    val hostname:String = event.data.get("hostname")
    val bytesUp:String = event.data.get("bytesUp")
    val bytesDown:String = event.data.get("bytesDown")
    val threshold:String = event.data.get("threshold")
    val numberPkts:String = event.data.get("numberPkts")
    val stringFlows:String = event.data.get("stringFlows")
    val pairs:String = event.data.get("pairs")
    
    event.title = "AD: Top talker identified ("+pairs+" pairs, "+humanBytes(bytesUp)+")"
    
    event.text = "This IP was detected by AD performing an abnormal activity. In what follows, you can see more information.\n"+
                  "Abnormal behaviour: Large amount of sent data (>"+threshold+")\n"+
                  "IP: "+hostname+"\n"+
                  "Bytes Up: "+humanBytes(bytesUp)+"\n"+
                  "Bytes Down: "+humanBytes(bytesDown)+"\n"+
                  "Packets: "+numberPkts+"\n"+
                  "Flows"+stringFlows
                    
    event.signature_id = signature._1.signature_id       
    event
  }
  * */
  
  def populateSMTPTalker(event:HogEvent):HogEvent =
  {
    logger.info("**Populare SMTP")
    HogGlobalParameter.count+=1;
    val hostname:String = event.data.get("hostname")
    val bytesUp:String = event.data.get("bytesUp")
    val bytesDown:String = event.data.get("bytesDown")
    val numberPkts:String = event.data.get("numberPkts")
    val stringFlows:String = event.data.get("stringFlows")
    val connections:String = event.data.get("connections")

    event.sflowAlgorithm = new SMTPTalkerAlgorithmDetails(hostname, bytesUp, bytesDown, numberPkts, stringFlows, connections)

    event.title = "AD: SMTP talker identified ("+connections+" flows, "+humanBytes(bytesUp)+")"
    
    event.text = "This IP was detected by AD performing an abnormal activity. In what follows, you can see more information.\n"+
                  "Abnormal behaviour: SMTP communication\n"+
                  "IP: "+hostname+"\n"+
                  "Bytes Up: "+humanBytes(bytesUp)+"\n"+
                  "Bytes Down: "+humanBytes(bytesDown)+"\n"+
                  "Packets: "+numberPkts+"\n"+
                  "Connections: "+connections+"\n"+
                  "Flows"+stringFlows
                  
    event.signature_id = signature._2.signature_id       
    event
  }
  
  
  def populateAtypicalTCPPortUsed(event:HogEvent):HogEvent =
  {
    HogGlobalParameter.count+=1;
    logger.info("**Populare TCP")

    val tcpport:String = event.data.get("tcpport")
    val myIP:String = event.data.get("myIP")
    val bytesUp:String = event.data.get("bytesUp")
    val bytesDown:String = event.data.get("bytesDown")
    val numberPkts:String = event.data.get("numberPkts")
    val stringFlows:String = event.data.get("stringFlows")

    event.sflowAlgorithm = new AtypicalTCPPortUsedAlgorithmDetails(tcpport, myIP, bytesUp, bytesDown, numberPkts, stringFlows)

    event.title = f"AD: Atypical TCP port used ($tcpport)"
    
    event.ports = "TCP: "+tcpport
    
    event.text = "This IP was detected by AD performing an abnormal activity. In what follows, you can see more information.\n"+
                  "Abnormal behaviour: Atypical TCP port used ("+tcpport+")\n"+
                  "IP: "+myIP+"\n"+
                  "Bytes Up: "+humanBytes(bytesUp)+"\n"+
                  "Bytes Down: "+humanBytes(bytesDown)+"\n"+
                  "Packets: "+numberPkts+"\n"+
                  "Flows"+stringFlows
                  
    event.signature_id = signature._3.signature_id       
    event
  }
  
  def populateAtypicalAlienTCPPortUsed(event:HogEvent):HogEvent =
  {
    HogGlobalParameter.count+=1;
    logger.info("**Populare Alien")
    val tcpport:String = event.data.get("tcpport")
    val myIP:String = event.data.get("myIP")
    val bytesUp:String = event.data.get("bytesUp")
    val bytesDown:String = event.data.get("bytesDown")
    val numberPkts:String = event.data.get("numberPkts")
    val stringFlows:String = event.data.get("stringFlows")

    event.sflowAlgorithm = new AtypicalAlienTCPPortUsedAlgorithmDetails(tcpport, myIP, bytesUp, bytesDown, numberPkts, stringFlows)
    
    event.title = f"AD: Atypical Alien TCP port used ($tcpport)"
    
    event.ports = "TCP: "+tcpport

    
    event.text = "This IP was detected by AD performing an abnormal activity. In what follows, you can see more information.\n"+
                  "Abnormal behaviour: Atypical alien TCP port used ("+tcpport+")\n"+
                  "IP: "+myIP+"\n"+
                  "Bytes Up: "+humanBytes(bytesUp)+"\n"+
                  "Bytes Down: "+humanBytes(bytesDown)+"\n"+
                  "Total packets: "+numberPkts+"\n"+
                  "Flows matching the atypical ports"+stringFlows
                  
    event.signature_id = signature._4.signature_id       
    event
  }
  
  
  def populateAtypicalNumberOfPairs(event:HogEvent):HogEvent =
  {
    HogGlobalParameter.count+=1;
    val numberOfPairs:String = event.data.get("numberOfPairs")
    val myIP:String = event.data.get("myIP")
    val bytesUp:String = event.data.get("bytesUp")
    val bytesDown:String = event.data.get("bytesDown")
    val numberPkts:String = event.data.get("numberPkts")
    val stringFlows:String = event.data.get("stringFlows")
    val pairsMean:String = event.data.get("pairsMean")
    val pairsStdev:String = event.data.get("pairsStdev")

    event.sflowAlgorithm = new AtypicalNumberOfPairsAlgorithmDetails(numberOfPairs, myIP, bytesUp, bytesDown, numberPkts, stringFlows, pairsMean, pairsStdev)

    event.title = f"AD: Atypical number of pairs in the period ($numberOfPairs)"
    
    
    event.text = "This IP was detected by AD performing an abnormal activity. In what follows, you can see more information.\n"+
                  "Abnormal behaviour: Atypical number of pairs in the period ("+numberOfPairs+")\n"+
                  "IP: "+myIP+"\n"+
                  "Bytes Up: "+humanBytes(bytesUp)+"\n"+
                  "Bytes Down: "+humanBytes(bytesDown)+"\n"+
                  "Packets: "+numberPkts+"\n"+
                  "Number of pairs: "+numberOfPairs+"\n"+
                  "Pairs Mean/Stddev (all MyHosts): "+pairsMean+"/"+pairsStdev+"\n"+
                  "Flows"+stringFlows
                  
    event.signature_id = signature._5.signature_id       
    event
  }
  
            
          
  def populateAtypicalAmountData(event:HogEvent):HogEvent =
  {
    HogGlobalParameter.count+=1;
    val numberOfPairs:String = event.data.get("numberOfPairs")
    val myIP:String = event.data.get("myIP")
    val bytesUp:String = event.data.get("bytesUp")
    val bytesDown:String = event.data.get("bytesDown")
    val numberPkts:String = event.data.get("numberPkts")
    val stringFlows:String = event.data.get("stringFlows")
    val dataMean:String = event.data.get("dataMean")
    val dataStdev:String = event.data.get("dataStdev")

    event.sflowAlgorithm = new AtypicalAmountDataAlgorithmDetails(numberOfPairs, myIP, bytesUp, bytesDown, numberPkts, stringFlows, dataMean, dataStdev)

    event.title = "AD: Atypical amount of data transferred ("+humanBytes(bytesUp)+"/"+humanBytes(bytesDown)+")"
    
    event.text = "This IP was detected by AD performing an abnormal activity. In what follows, you can see more information.\n"+
                  "Abnormal behaviour: Atypical amount of data uploaded ("+bytesUp+" bytes)\n"+
                  "IP: "+myIP+"\n"+
                  "Bytes Up: "+humanBytes(bytesUp)+"\n"+
                  "Bytes Down: "+humanBytes(bytesDown)+"\n"+
                  "Bytes Up Mean/Stddev (all MyHosts): "+humanBytes(dataMean)+"/"+humanBytes(dataStdev)+"\n"+
                  "Packets: "+numberPkts+"\n"+
                  "Number of pairs: "+numberOfPairs+"\n"+
                  "Flows"+stringFlows
                  
    event.signature_id = signature._6.signature_id       
    event
  }  
  
  
  def populateAlienAccessingManyHosts(event:HogEvent):HogEvent =
  {
    HogGlobalParameter.count+=1;
    val numberOfPairs:String = event.data.get("numberOfPairs")
    val alienIP:String = event.data.get("alienIP")
    val bytesUp:String = event.data.get("bytesUp")
    val bytesDown:String = event.data.get("bytesDown")
    val numberPkts:String = event.data.get("numberPkts")
    val stringFlows:String = event.data.get("stringFlows")
    val ports:String = event.data.get("ports")

    event.sflowAlgorithm = new AlienAccessingManyHostsAlgorithmDetails(numberOfPairs, alienIP , bytesUp, bytesDown, numberPkts, stringFlows, ports)

    event.title = "AD: Horizontal scan on ports "+ports
    
    event.ports = "Ports: "+ports
    
    event.text = "This IP was detected by AD performing an abnormal activity. In what follows, you can see more information.\n"+
                  "Abnormal behaviour: Alien accessing too much hosts ("+numberOfPairs+"). Possibly a horizontal port scan.\n"+
                  "AlienIP: "+alienIP+"\n"+
                  "Bytes Up: "+humanBytes(bytesUp)+"\n"+
                  "Bytes Down: "+humanBytes(bytesDown)+"\n"+
                  "Packets: "+numberPkts+"\n"+
                  "Number of pairs: "+numberOfPairs+"\n"+
                  "Flows"+stringFlows
                  
    event.signature_id = signature._7.signature_id       
    event
  }  
  
  
  def populateP2PCommunication(event:HogEvent):HogEvent =
  {
    HogGlobalParameter.count+=1;
    logger.info("**Populare P2P")
    val numberOfPairs:String = event.data.get("numberOfPairs")
    val myIP:String = event.data.get("myIP")
    val bytesUp:String = event.data.get("bytesUp")
    val bytesDown:String = event.data.get("bytesDown")
    val numberPkts:String = event.data.get("numberPkts")
    val stringFlows:String = event.data.get("stringFlows")

    event.sflowAlgorithm = new P2PCommunicationAlgorithmDetails(numberOfPairs, myIP, bytesUp, bytesDown, numberPkts, stringFlows)

    event.title = "AD: P2P communication"

    
    event.text = "This IP was detected by AD performing an abnormal activity. In what follows, you can see more information.\n"+
                  "Abnormal behaviour: P2P Communication\n"+
                  "MyIP: "+myIP+"\n"+
                  "Bytes Up: "+humanBytes(bytesUp)+"\n"+
                  "Bytes Down: "+humanBytes(bytesDown)+"\n"+
                  "Packets: "+numberPkts+"\n"+
                  "Number of pairs: "+numberOfPairs+"\n"+
                  "Flows"+stringFlows
                  
    event.signature_id = signature._8.signature_id       
    event
  }  
  
  def populateUDPAmplifier(event:HogEvent):HogEvent =
  {
    HogGlobalParameter.count+=1;
    val hostname:String = event.data.get("hostname")
    val bytesUp:String = event.data.get("bytesUp")
    val bytesDown:String = event.data.get("bytesDown")
    val numberPkts:String = event.data.get("numberPkts")
    val stringFlows:String = event.data.get("stringFlows")
    val connections:String = event.data.get("connections")

    event.sflowAlgorithm = new UDPAmplifierAlgorithmDetails(hostname, bytesUp, bytesDown, numberPkts, stringFlows, connections)

    event.text = "This IP was detected by AD performing an abnormal activity. In what follows, you can see more information.\n"+
                  "Abnormal behaviour: Host is sending too many big UDP packets. May be a DDoS.\n"+
                  "IP: "+hostname+"\n"+
                  "Bytes Up: "+humanBytes(bytesUp)+"\n"+
                  "Bytes Down: "+humanBytes(bytesDown)+"\n"+
                  "Packets: "+numberPkts+"\n"+
                  "Connections: "+connections+"\n"+
                  "Flows"+stringFlows
                  
    event.signature_id = signature._9.signature_id       
    event
  }
  
  def populateAbusedSMTP(event:HogEvent):HogEvent =
  {
    HogGlobalParameter.count+=1;
    logger.info("**Populare AbusedSMTP")

    val hostname:String = event.data.get("hostname")
    val bytesUp:String = event.data.get("bytesUp")
    val bytesDown:String = event.data.get("bytesDown")
    val numberPkts:String = event.data.get("numberPkts")
    val stringFlows:String = event.data.get("stringFlows")
    val connections:String = event.data.get("connections")

    event.sflowAlgorithm = new AbusedSMTPAlgorithmDetails(hostname, bytesUp, bytesDown, numberPkts, stringFlows, connections)

    event.text = "This IP was detected by AD performing an abnormal activity. In what follows, you can see more information.\n"+
                  "Abnormal behaviour: Host is receiving too many e-mail submissions. May be an abused SMTP server. \n"+
                  "IP: "+hostname+"\n"+
                  "Bytes Up: "+humanBytes(bytesUp)+"\n"+
                  "Bytes Down: "+humanBytes(bytesDown)+"\n"+
                  "Packets: "+numberPkts+"\n"+
                  "Connections: "+connections+"\n"+
                  "Flows"+stringFlows
                  
    event.signature_id = signature._10.signature_id       
    event
  }
  
 
   
  def populateMediaClient(event:HogEvent):HogEvent =
  {
    HogGlobalParameter.count+=1;
    val hostname:String = event.data.get("hostname")
    val bytesUp:String = event.data.get("bytesUp")
    val bytesDown:String = event.data.get("bytesDown")
    val numberPkts:String = event.data.get("numberPkts")
    val stringFlows:String = event.data.get("stringFlows")
    val connections:String = event.data.get("connections")

    event.sflowAlgorithm = new MediaStreamingClientAlgorithmDetails(hostname, bytesUp, bytesDown, numberPkts, stringFlows, connections)

    event.text = "This IP was detected by AD performing an abnormal activity. In what follows, you can see more information.\n"+
                  "Abnormal behaviour: Appears to be a media streaming client.\n"+
                  "IP: "+hostname+"\n"+
                  "Bytes Up: "+humanBytes(bytesUp)+"\n"+
                  "Bytes Down: "+humanBytes(bytesDown)+"\n"+
                  "Packets: "+numberPkts+"\n"+
                  "Connections: "+connections+"\n"+
                  "Flows"+stringFlows
            
    event.signature_id = signature._11.signature_id          
    event
  }
  
     
  def populateDNSTunnel(event:HogEvent):HogEvent =
  {
    HogGlobalParameter.count+=1;
    logger.info("**Populare DNS")
    val hostname:String = event.data.get("hostname")
    val bytesUp:String = event.data.get("bytesUp")
    val bytesDown:String = event.data.get("bytesDown")
    val numberPkts:String = event.data.get("numberPkts")
    val stringFlows:String = event.data.get("stringFlows")
    val connections:String = event.data.get("connections")

    event.sflowAlgorithm = new DnsTunnelAlgorithmDetails(hostname, bytesUp, bytesDown, numberPkts, stringFlows, connections)

    event.text = "This IP was detected by AD performing an abnormal activity. In what follows, you can see more information.\n"+
                  "Abnormal behaviour: Host has DNS communication with large amount of data. \n"+
                  "IP: "+hostname+"\n"+
                  "Bytes Up: "+humanBytes(bytesUp)+"\n"+
                  "Bytes Down: "+humanBytes(bytesDown)+"\n"+
                  "Packets: "+numberPkts+"\n"+
                  "Connections: "+connections+"\n"+
                  "Flows"+stringFlows
                  
    event.signature_id = signature._12.signature_id       
    event
  }
  
     
  def populateICMPTunnel(event:HogEvent):HogEvent =
  {
    HogGlobalParameter.count+=1;
    val hostname:String = event.data.get("hostname")
    val bytesUp:String = event.data.get("bytesUp")
    val bytesDown:String = event.data.get("bytesDown")
    val numberPkts:String = event.data.get("numberPkts")
    val stringFlows:String = event.data.get("stringFlows")
    val connections:String = event.data.get("connections")

    event.sflowAlgorithm = new ICMPTunnelAlgorithmDetails(hostname, bytesUp, bytesDown, numberPkts, stringFlows, connections)

    event.text = "This IP was detected by AD performing an abnormal activity. In what follows, you can see more information.\n"+
                  "Abnormal behaviour: Host has DNS communication with large amount of data. \n"+
                  "IP: "+hostname+"\n"+
                  "Bytes Up: "+humanBytes(bytesUp)+"\n"+
                  "Bytes Down: "+humanBytes(bytesDown)+"\n"+
                  "Packets: "+numberPkts+"\n"+
                  "Connections: "+connections+"\n"+
                  "Flows"+stringFlows
                  
    event.signature_id = signature._13.signature_id       
    event
  }
  
  
  def populateHorizontalPortScan(event:HogEvent):HogEvent =
  {
    HogGlobalParameter.count+=1;
    val numberOfFlows:String = event.data.get("numberOfFlows")
    val numberOfFlowsPerPort:String = event.data.get("numberOfFlowsPerPort")
    val myIP:String = event.data.get("myIP")
    val bytesUp:String = event.data.get("bytesUp")
    val bytesDown:String = event.data.get("bytesDown")
    val numberPkts:String = event.data.get("numberPkts")
    val stringFlows:String = event.data.get("stringFlows")
    val flowsMean:String = event.data.get("flowsMean")
    val flowsStdev:String = event.data.get("flowsStdev")
    val numberOfFlowsAlienPort:String = event.data.get("numberOfFlowsAlienPort")
    val ports:String = event.data.get("ports")

    event.sflowAlgorithm = new HorizontalPortScanAlgorithmDetails(numberOfFlows, numberOfFlowsPerPort, myIP, bytesUp, bytesDown, numberPkts, stringFlows, flowsMean, flowsStdev, numberOfFlowsAlienPort, ports)

    event.title = "AD: Horizontal scan on ports "+ports
    
    event.ports = "Ports: "+ports

    
    event.text = "This IP was detected by AD performing an abnormal activity. In what follows, you can see more information.\n"+
                  "Abnormal behaviour: Horizontal Port Scan \n"+
                  "IP: "+myIP+"\n"+
                  "Number of flows: "+numberOfFlows+"\n"+
                  "Number of flows per AlienPort: "+numberOfFlowsPerPort+"\n"+
                  "Number of flows per distinct AlienIP/AlienPort: "+numberOfFlowsAlienPort+"\n"+
                  "Mean/Stddev of flows per AlienPort (all flows for this IP): "+flowsMean+"/"+flowsStdev+"\n"+
                  "Bytes Up: "+humanBytes(bytesUp)+"\n"+
                  "Bytes Down: "+humanBytes(bytesDown)+"\n"+
                  "Packets: "+numberPkts+"\n"+
                  "Flows"+stringFlows
                  
    event.signature_id = signature._14.signature_id  
    event
  }
  
  
  def populateVerticalPortScan(event:HogEvent):HogEvent =
  {
    HogGlobalParameter.count+=1;
    val numberOfFlows:String = event.data.get("numberOfFlows")
    val numberOfPorts:String = event.data.get("numberOfPorts")
    val myIP:String = event.data.get("myIP")
    val alienIP:String = event.data.get("alienIP")
    val bytesUp:String = event.data.get("bytesUp")
    val bytesDown:String = event.data.get("bytesDown")
    val numberPkts:String = event.data.get("numberPkts")
    val stringFlows:String = event.data.get("stringFlows")
    val portsMean:String = event.data.get("portsMean")
    val portsStdev:String = event.data.get("portsStdev")

    event.sflowAlgorithm = new VerticalPortScanAlgorithmDetails(numberOfFlows, numberOfPorts, myIP, bytesUp, bytesDown, numberPkts, stringFlows, portsMean, portsStdev)

    event.text = "This IP was detected by AD performing an abnormal activity. In what follows, you can see more information.\n"+
                  "Abnormal behaviour: Vertical Port Scan \n"+
                  "IP: "+myIP+"\n"+
                  "Scanned AlienIP: "+alienIP+"\n"+
                  "Number of flows: "+numberOfFlows+"\n"+
                  "Number of distinct AlienPorts: "+numberOfPorts+"\n"+
                  "Mean/Stddev of AlienPorts per AlienIP (all flows for this IP): "+portsMean+"/"+portsStdev+"\n"+
                  "Bytes Up: "+humanBytes(bytesUp)+"\n"+
                  "Bytes Down: "+humanBytes(bytesDown)+"\n"+
                  "Packets: "+numberPkts+"\n"+
                  "Flows"+stringFlows
                  
    event.signature_id = signature._15.signature_id  
    event
  }
  
  
  
  def populateDDoSAttack(event:HogEvent):HogEvent =
  {
    HogGlobalParameter.count+=1;
    val numberOfFlows:String = event.data.get("numberOfFlows")
    val numberOfAttackers:String = event.data.get("numberOfAttackers")
    val myIP:String = event.data.get("myIP")
    val bytesUp:String = event.data.get("bytesUp")
    val bytesDown:String = event.data.get("bytesDown")
    val numberPkts:String = event.data.get("numberPkts")
    val stringFlows:String = event.data.get("stringFlows")
    val flowsMean:String = event.data.get("flowsMean")
    val flowsStdev:String = event.data.get("flowsStdev")
    
    
    event.text = "This IP was detected by AD performing an abnormal activity. In what follows, you can see more information.\n"+
                  "Abnormal behaviour: Host possibly under DDoS attack.\n"+
                  "IP: "+myIP+"\n"+
                  "Number of Attackers: "+numberOfAttackers+"\n"+
                  "Number of flows: "+numberOfFlows+"\n"+
                  "Mean/Stddev of flows per AlienIP (all flows for this IP): "+flowsMean+"/"+flowsStdev+"\n"+
                  "Bytes Up: "+humanBytes(bytesUp)+"\n"+
                  "Bytes Down: "+humanBytes(bytesDown)+"\n"+
                  "Packets: "+numberPkts+"\n"+
                  "Flows"+stringFlows
                  
    event.signature_id = signature._16.signature_id  
    event
  }
 
      
  def populateCCBotNet(event:HogEvent):HogEvent =
  {
    HogGlobalParameter.count+=1;
    val hostname:String = event.data.get("hostname")
    val bytesUp:String = event.data.get("bytesUp")
    val bytesDown:String = event.data.get("bytesDown")
    val numberPkts:String = event.data.get("numberPkts")
    val stringFlows:String = event.data.get("stringFlows")
    val connections:String = event.data.get("connections")
    val aliens:String = event.data.get("aliens")

    event.sflowAlgorithm = new CCBotNetAlgorithmDetails(hostname, bytesUp, bytesDown, numberPkts, stringFlows, connections, aliens)

    event.title = "AD: C&C BotNet communication - "+hostname+" <?> "+aliens
    
    event.text = "This IP was detected by AD performing an abnormal activity. In what follows, you can see more information.\n"+
                  "Abnormal behaviour: Host C&C BotNet communication. \n"+
                  "IP: "+hostname+"\n"+
                  "Blacklisted aliens: "+aliens+"\n"+
                  "Bytes Up: "+humanBytes(bytesUp)+"\n"+
                  "Bytes Down: "+humanBytes(bytesDown)+"\n"+
                  "Packets: "+numberPkts+"\n"+
                  "Connections: "+connections+"\n"+aliens.split(",")
                  .mkString("VirusTotal ref.: https://www.virustotal.com/en/ip-address/",
                      "\n VirusTotal ref.: https://www.virustotal.com/en/ip-address/", "\n")
                  "Flows"+stringFlows
                  
    event.signature_id = signature._17.signature_id       
    event
  }
  
  def setFlows2String(flowSet:HashSet[(String,String,String,String,String,Long,Long,Long,Int,Long,Long,Long,Int)]):String =
  {
     flowSet.toList.sortBy({case (srcIP1,srcPort1,dstIP1,dstPort1,proto1,bytesUP,bytesDOWN,numberPkts1,direction1,beginTime,endTime,sampleRate,status) =>  bytesUP+bytesDOWN })
            .reverse
            .take(FlowListLimit)
            ./:("")({ case (c,(srcIP1,srcPort1,dstIP1,dstPort1,proto1,bytesUP,bytesDOWN,numberPkts1,direction1,beginTime,endTime,sampleRate,status)) 
                        => 
                          val statusInd = { if(status>0) "[!]" else ""}
                          if(direction1>0)
                          {
                           c+"\n"+
                           srcIP1+":"+srcPort1+" => "+dstIP1+":"+dstPort1+" "+statusInd+" ("+proto1+", Up: "+humanBytes(bytesUP*sampleRate)+", Down: "+humanBytes(bytesDOWN*sampleRate)+","+numberPkts1+" pkts, duration: "+(endTime-beginTime)+"s, sampling: 1/"+sampleRate+")"
                          }else if(direction1<0)
                          {  
                           c+"\n"+
                           srcIP1+":"+srcPort1+" <= "+dstIP1+":"+dstPort1+" "+statusInd+" ("+proto1+", Down: "+humanBytes(bytesUP*sampleRate)+", Up: "+humanBytes(bytesDOWN*sampleRate)+","+numberPkts1+" pkts, duration: "+(endTime-beginTime)+"s, sampling: 1/"+sampleRate+")"
                          }else
                          {  
                           c+"\n"+
                           srcIP1+":"+srcPort1+" <?> "+dstIP1+":"+dstPort1+" "+statusInd+" ("+proto1+", L-to-R: "+humanBytes(bytesUP*sampleRate)+", R-to-L: "+humanBytes(bytesDOWN*sampleRate)+","+numberPkts1+" pkts, duration: "+(endTime-beginTime)+"s, sampling: 1/"+sampleRate+")"
                          }
                    })
  }
  
  
  def setFlowsICMP2String(flowSet:HashSet[(String,String,String,String,String,Long,Long,Long,Int,Long,Long,Long,Int)]):String =
  {
     flowSet
      .toList
      .sortBy({case (srcIP1,srcPort1,dstIP1,dstPort1,proto1,bytesUP,bytesDOWN,numberPkts1,direction1,beginTime,endTime,sampleRate,status) =>  
                        bytesUP+bytesDOWN 
             })
      .reverse 
      .take(FlowListLimit)
      ./:("")({ case (c,(srcIP1,icmpType,dstIP1,icmpCode,proto1,bytesUP,bytesDOWN,numberPkts1,direction1,beginTime,endTime,sampleRate,status)) 
                     => 
                     if(direction1>0)
                     {
                       c+"\n"+
                       srcIP1+" -> "+dstIP1+" ("+proto1+", Type/Code: "+icmpType+"/"+icmpCode+", L-to-R: "+humanBytes(bytesUP*sampleRate)+", R-to-L: "+humanBytes(bytesDOWN*sampleRate)+","+numberPkts1+" pkts, duration: "+(endTime-beginTime)+"s, sampling: 1/"+sampleRate+")"
                     }else if(direction1<0)
                     { 
                       c+"\n"+
                       srcIP1+" <- "+dstIP1+" ("+proto1+", Type/Code: "+icmpType+"/"+icmpCode+", L-to-R: "+humanBytes(bytesUP*sampleRate)+", R-to-L: "+humanBytes(bytesDOWN*sampleRate)+","+numberPkts1+" pkts, duration: "+(endTime-beginTime)+"s, sampling: 1/"+sampleRate+")"
                     }else
                     {
                       c+"\n"+
                       srcIP1+" <?> "+dstIP1+" ("+proto1+", Type/Code: "+icmpType+"/"+icmpCode+", L-to-R: "+humanBytes(bytesUP*sampleRate)+", R-to-L: "+humanBytes(bytesDOWN*sampleRate)+","+numberPkts1+" pkts, duration: "+(endTime-beginTime)+"s, sampling: 1/"+sampleRate+")"
                     }
                     
             })
  }
  
  def formatIPtoBytes(ip:String):Array[Byte] =
  {
    // Eca! Snorby doesn't support IPv6 yet. See https://github.com/Snorby/snorby/issues/65
    if(ip.contains(":"))
      InetAddress.getByName("255.255.6.6").getAddress
    else  
      InetAddress.getByName(ip).getAddress
  }
 

  def isMyIP(ip:String,myNets:Set[String]):Boolean =
  {
    myNets.map ({ net =>  if( ip.startsWith(net) )
                              { true } 
                          else{false} 
                }).contains(true)
  }
  
  def ipSignificantNetwork(ip:String):String =
  {
    if(ip.contains("."))
      return ip.substring(0,ip.lastIndexOf("."))
    
    if(ip.contains(":"))
      return ip.substring(0,ip.lastIndexOf(":"))
      
    ip
  }
  
   def humanBytes(b:Any):String =
   {
    val bytes=b.toString().toLong
    val unit = 1024L
    if (bytes < unit) return bytes + " B";
    val exp = (log(bytes) / log(unit)).toInt;
    val pre = "KMGTPE".charAt(exp-1)
    "%.1f%sB".format(bytes / math.pow(unit, exp), pre);
  }

  def top(HogRDD: RDD[(org.apache.hadoop.hbase.io.ImmutableBytesWritable,org.apache.hadoop.hbase.client.Result)],spark:SparkContext)
  {
    val myNetsTemp =  new HashSet[String]

    val it = HogHBaseRDD.hogzilla_mynets.getScanner(new Scan()).iterator()
    while(it.hasNext())
    {
      myNetsTemp.add(Bytes.toString(it.next().getValue(Bytes.toBytes("net"),Bytes.toBytes("prefix"))))
    }

    val myNets:scala.collection.immutable.Set[String] = myNetsTemp.toSet
    
   println("=========================== [HogSFlow.scala] => Filtering SflowRDD...")
   println("=========================== [HogSFlow.scala] => My networks:")
    logger.info("=========================== [HogSFlow.scala] => My networks:")
   myNets.foreach { println(_) }
    myNets.foreach {logger.info(_) }
   
  /*
  * 
  * SFlow Summary
  * 
  */

  println("=========================== [HogSFlow.scala] => try to count....")
    logger.info("=========================== [HogSFlow.scala] => try to count....")
    System.out.println("=========================== [HogSFlow.scala] => try to count....")

  var initialSize = HogRDD.count()
  println("=========================== [HogSFlow.scala] => sflowSummary has "+initialSize+" rows!")
    logger.info("=========================== [HogSFlow.scala] => sflowSummary has "+initialSize+" rows!")
    System.out.println("=========================== [HogSFlow.scala] => sflowSummary has "+initialSize+" rows!")

  //Directions
  val UNKNOWN   = 0  
  val LEFTRIGHT = 1
  val RIGHTLEFT = -1
  val OCCURRED = 1
  // filtreaza sflow-urile dupa protocol TCP ("6") or UDP ("17) + calculeaza direction (UNKNOWN, LEFTRIGHT, RIGHTLEFT ), status (UNKNOWN, OCCURRED), protoName ("UDP", "TCP")
  val sflowSummary1: PairRDDFunctions[(String,String,String,String,String), (Long,Long,Long,Int,Long,Long,Long,Int)] 
                      = HogRDD
                        .map ({  case (id,result) => 
                                     /* Performance
                                      val map: Map[String,String] = new HashMap[String,String]
                                      map.put("flow:id",Bytes.toString(id.get).toString())
                                      HogHBaseRDD.columnsSFlow.foreach { column => 
                                      val ret = result.getValue(Bytes.toBytes(column.split(":")(0).toString()),Bytes.toBytes(column.split(":")(1).toString()))
                                      map.put(column, Bytes.toString(ret)) 
                                      }
                                      */
                                      val srcIP       = Bytes.toString(result.getValue(Bytes.toBytes("flow"),Bytes.toBytes("srcIP")))
                                      val srcPort     = Bytes.toString(result.getValue(Bytes.toBytes("flow"),Bytes.toBytes("srcPort")))
                                      val dstIP       = Bytes.toString(result.getValue(Bytes.toBytes("flow"),Bytes.toBytes("dstIP")))
                                      val dstPort     = Bytes.toString(result.getValue(Bytes.toBytes("flow"),Bytes.toBytes("dstPort")))
                                      val packetSizeDouble:Double = Bytes.toString(result.getValue(Bytes.toBytes("flow"),Bytes.toBytes("packetSize"))).toDouble
                                      val packetSize  = packetSizeDouble.toLong
                                      val tcpFlags    = Bytes.toString(result.getValue(Bytes.toBytes("flow"),Bytes.toBytes("tcpFlags")))
                                      val IPprotocol  = Bytes.toString(result.getValue(Bytes.toBytes("flow"),Bytes.toBytes("IPprotocol")))
                                      // TODO retirar possível "\x00" no final

                                      val timestampStr = Bytes.toString(result.getValue(Bytes.toBytes("flow"),Bytes.toBytes("timestamp")))
                                      val stLocalDateTime:LocalDateTime = LocalDateTime.parse(timestampStr)
                                      val timeStamp: Timestamp = Timestamp.valueOf(stLocalDateTime)
                                      val timestamp   = timeStamp.getTime()

                                      val sampleRate   = Bytes.toString(result.getValue(Bytes.toBytes("flow"),Bytes.toBytes("samplingRate"))).toLong

                                      var direction = UNKNOWN
                                      var protoName="UDP" // We filter below TCP or UDP
                                      var status = UNKNOWN
                                      
                                      if(IPprotocol.equals("6")) // If is TCP
                                      {
                                        protoName="TCP"
                                        if(tcpFlags.equals("2")) // Is a SYN pkt  "0x02"
                                          direction = LEFTRIGHT
                                        if(tcpFlags.equals("12")) // Is a SYN-ACK pkt "0x12"
                                        {
                                          direction = RIGHTLEFT
                                          status = OCCURRED
                                        }
                                        
                                        if(tcpFlags.equals("18")) // Is a PSH-ACK pkt "0x18"
                                        {
                                          status = OCCURRED
                                        }
                                        
                                        if(tcpFlags.equals("10") & isMyIP(srcIP,myNets)) // Is a ACK pkt originated by a MyHost  // 0x10
                                        {
                                          status = OCCURRED
                                        }
                                          
                                        // Suppose that ports < 1024 would not used for clients
                                        if(direction==UNKNOWN)
                                        {
                                          if(dstPort.toInt<1024)
                                            direction = LEFTRIGHT
                                            
                                          if(srcPort.toInt<1024)
                                            direction = RIGHTLEFT
                                        }
                                      }
                                      
                               if(!isMyIP(srcIP,myNets))
                               {
                                 ((  dstIP,
                                       dstPort,
                                       srcIP,
                                       srcPort, 
                                       protoName ), 
                                    (0L, packetSize, 1L,-direction,timestamp,timestamp,IPprotocol,sampleRate,status)
                                   )
                               }else
                               {
                                  ((  srcIP,
                                       srcPort,
                                       dstIP,
                                       dstPort, 
                                       protoName ), 
                                    (packetSize, 0L, 1L, direction,timestamp,timestamp,IPprotocol,sampleRate,status)
                                   )
                                   
                               } 
                           })
                           .filter({case ((myIP,myPort,alienIP,alienPort, proto ),(bytesUP,bytesDown,numberOfPkts,direction,beginTime,endTime,iPprotocolNumber,sampleRate,status))
                                             =>  iPprotocolNumber.equals("6") || iPprotocolNumber.equals("17") // TCP or UDP
                                  })
                           .map({case ((myIP,myPort,alienIP,alienPort, proto ),(bytesUP,bytesDown,numberOfPkts,direction,beginTime,endTime,iPprotocol,sampleRate,status))
                                    =>((myIP,myPort,alienIP,alienPort, proto ),(bytesUP,bytesDown,numberOfPkts,direction,beginTime,endTime,sampleRate,status))
                                })


    // (srcIP, srcPort, dstIP, dstPort, totalBytes, numberOfPkts)
  val sflowSummary = 
      sflowSummary1
      .reduceByKey({ case ((bytesUpA,bytesDownA,pktsA,directionA,beginTimeA,endTimeA,sampleRateA,statusA),(bytesUpB,bytesDownB,pktsB,directionB,beginTimeB,endTimeB,sampleRateB,statusB)) => 
                           (bytesUpA+bytesUpB,bytesDownA+bytesDownB,pktsA+pktsB,directionA+directionB,beginTimeA.min(beginTimeB),endTimeA.max(endTimeB),(sampleRateA+sampleRateB)/2,statusA+statusB)
                  })
      .cache
      
      
  val sflowSummaryICMP1: PairRDDFunctions[(String,String,String,String,String), (Long,Long,Long,Int,Long,Long,Long)] 
                      = HogRDD
                        .map ({  case (id,result) => 
                                     
                                      val srcIP       = Bytes.toString(result.getValue(Bytes.toBytes("flow"),Bytes.toBytes("srcIP")))
                                      val icmpType    = Bytes.toString(result.getValue(Bytes.toBytes("flow"),Bytes.toBytes("srcPort")))
                                      val dstIP       = Bytes.toString(result.getValue(Bytes.toBytes("flow"),Bytes.toBytes("dstIP")))
                                      val icmpCode    = Bytes.toString(result.getValue(Bytes.toBytes("flow"),Bytes.toBytes("dstPort")))
                                      val packetSizeDouble:Double = Bytes.toString(result.getValue(Bytes.toBytes("flow"),Bytes.toBytes("packetSize"))).toDouble
                                      val packetSize  = packetSizeDouble.toLong
                                      val IPprotocol  = Bytes.toString(result.getValue(Bytes.toBytes("flow"),Bytes.toBytes("IPprotocol")))
                                   //   val timestamp   = Bytes.toString(result.getValue(Bytes.toBytes("flow"),Bytes.toBytes("timestamp"))).toLong

                                      val timestampStr = Bytes.toString(result.getValue(Bytes.toBytes("flow"),Bytes.toBytes("timestamp")))
                                      val stLocalDateTime:LocalDateTime = LocalDateTime.parse(timestampStr)
                                      val timeStamp2: Timestamp = Timestamp.valueOf(stLocalDateTime)
                                      val timestamp = timeStamp2.getTime()

                                      val sampleRate   = Bytes.toString(result.getValue(Bytes.toBytes("flow"),Bytes.toBytes("samplingRate"))).toLong

                                      var protoName="ICMPv6"
                                      
                                      if(IPprotocol.equals("1")) // If is ICMP
                                      {
                                        protoName="ICMP"
                                      }
                                      
                               if(!isMyIP(srcIP,myNets))
                               {
                                 ((  dstIP,
                                       icmpType,
                                       srcIP,
                                       icmpCode, 
                                       protoName ), 
                                    (0L, packetSize, 1L,-1,timestamp,timestamp,IPprotocol,sampleRate)
                                   )
                               }else
                               {
                                  ((  srcIP,
                                       icmpType,
                                       dstIP,
                                       icmpCode, 
                                       protoName ), 
                                    (packetSize, 0L, 1L, 1,timestamp,timestamp,IPprotocol,sampleRate)
                                   )
                                   
                               } 
                           })
                           .filter({case ((myIP,icmpType,alienIP,icmpCode, proto ),(bytesUP,bytesDown,numberOfPkts,direction,beginTime,endTime,iPprotocolNumber,sampleRate))
                                             =>  iPprotocolNumber.equals("1") || iPprotocolNumber.equals("58") // ICMP or ICMPv6
                                  })
                           .map({case ((myIP,icmpType,alienIP,icmpCode, proto ),(bytesUP,bytesDown,numberOfPkts,direction,beginTime,endTime,iPprotocol,sampleRate))
                                    =>((myIP,icmpType,alienIP,icmpCode, proto ),(bytesUP,bytesDown,numberOfPkts,direction,beginTime,endTime,sampleRate))
                                })


  val sflowSummaryICMP = 
      sflowSummaryICMP1
      .reduceByKey({ case ((bytesUpA,bytesDownA,pktsA,directionA,beginTimeA,endTimeA,sampleRateA),(bytesUpB,bytesDownB,pktsB,directionB,beginTimeB,endTimeB,sampleRateB)) => 
                           (bytesUpA+bytesUpB,bytesDownA+bytesDownB,pktsA+pktsB,directionA+directionB,beginTimeA.min(beginTimeB),endTimeA.max(endTimeB),(sampleRateA+sampleRateB)/2)
                  })
      .cache

      
  println("Counting sflowSummary...")
    logger.info("Counting sflowSummary...")
    System.out.println("Counting sflowSummary...")

  val RDDtotalSize= sflowSummary.count()
  println("Filtered sflowSummary has "+RDDtotalSize+" rows!")
    logger.info("Filtered sflowSummary has "+RDDtotalSize+" rows!")
    System.out.println("Filtered sflowSummary has "+RDDtotalSize+" rows!")

  println("Counting sflowSummaryICMP...")
    logger.info("Counting sflowSummaryICMP...")
    System.out.println("Counting sflowSummaryICMP...")

  val RDDsflowSummaryICMPSize = sflowSummaryICMP.count()
  println("Filtered sflowSummaryICMP has "+RDDsflowSummaryICMPSize+" rows!")
    logger.info("Filtered sflowSummaryICMP has "+RDDsflowSummaryICMPSize+" rows!")
    System.out.println("Filtered sflowSummaryICMP has "+RDDsflowSummaryICMPSize+" rows!")

  if(RDDtotalSize==0 && RDDsflowSummaryICMPSize==0) {
    return;
  }

    
 /*
  * 
  * SMTP Talkers - tested
  * 
  */

    logger.info("=================SMTP Talkers ======== " + disable_SMTPTalkers)
    System.out.println("======== SMTP Talkers =====")

  if(disable_SMTPTalkers==0)
  {
    
  val whiteSMTPTalkers =  HogHBaseReputation.getReputationList("MX","whitelist")

  println("")
  println("SMTP Talkers:")
  println("(SRC IP, DST IP, Bytes, Qtd Flows)")
  
    
   val SMTPTalkersCollection: PairRDDFunctions[String, (Long,Long,Long,HashSet[(String,String,String,String,String,Long,Long,Long,Int,Long,Long,Long,Int)],Long,Long)] = 
    sflowSummary
    .filter({case ((myIP,myPort,alienIP,alienPort,proto),(bytesUp,bytesDown,numberPkts,direction,beginTime,endTime,sampleRate,status)) 
                  => {
                    System.out.println("========SMTP Talkers:"+alienPort+" / "+numberPkts)
                    alienPort.equals("25") &
                      numberPkts > 3 &
                      !isMyIP(alienIP, myNets) & // Exclude internal communication
                      !SMTPTalkersExcludedIPs.contains(myIP) &
                      !SMTPTalkersExcludedIPs.contains(alienIP)
                  }
           })
    .map({
           case ((myIP,myPort,alienIP,alienPort,proto),(bytesUp,bytesDown,numberPkts,direction,beginTime,endTime,sampleRate,status)) =>
                val flowSet:HashSet[(String,String,String,String,String,Long,Long,Long,Int,Long,Long,Long,Int)] = new HashSet()
                flowSet.add((myIP,myPort,alienIP,alienPort,proto,bytesUp,bytesDown,numberPkts,direction,beginTime,endTime,sampleRate,status))
                (myIP,(bytesUp,bytesDown,numberPkts,flowSet,1L,sampleRate))
        })
  
  
  SMTPTalkersCollection
  .reduceByKey({
          case ((bytesUpA,bytesDownA,numberPktsA,flowSetA,connectionsA,sampleRateA),(bytesUpB,bytesDownB,numberPktsB,flowSetB,connectionsB,sampleRateB)) =>
               (bytesUpA+bytesUpB,bytesDownA+bytesDownB, numberPktsA+numberPktsB, flowSetA++flowSetB, connectionsA+connectionsB,(sampleRateA+sampleRateB)/2)
              })
  .sortBy({ 
              case   (myIP,(bytesUp,bytesDown,numberPkts,flowSet,connections,sampleRate)) =>    bytesUp  
          }, false, 15
         )
  .filter({ case (myIP,(bytesUp,bytesDown,numberPkts,flowSet,connections,sampleRate)) => 
                   {  
                     /*!whiteSMTPTalkers.map { net => if( myIP.startsWith(net) )
                                                      { true } else{false} 
                                           }.contains(true) &
                     */

                     System.out.println("========SMTP Talkers [2]:"+connections+" / "+(bytesUp+bytesDown)+"/"+SMTPTalkersThreshold+"/"+numberPkts)

                     connections > 1 & // Consider just MyIPs that generated more than 2 SMTP connections
                     (bytesUp+bytesDown)*sampleRate > SMTPTalkersThreshold &
                     numberPkts > 20 &
                     { val savedLastHogHistogram=HogHBaseHistogram.getHistogram("HIST01-"+myIP)
                       !Histograms.isTypicalEvent(savedLastHogHistogram.histMap, "25")// Exclude SMTP servers
                     } &
                     { val savedLastHogHistogram2=HogHBaseHistogram.getHistogram("HIST02-"+myIP)
                       !Histograms.isTypicalEvent(savedLastHogHistogram2.histMap, "25")// Exclude if sent before
                     }
                   }
          })
  .take(100)
  .foreach{ case (myIP,(bytesUp,bytesDown,numberPkts,flowSet,connections,sampleRate)) => 
                    println("("+myIP+","+bytesUp+")" ) 
                    val flowMap: Map[String,String] = new HashMap[String,String]
                    flowMap.put("flow:id",System.currentTimeMillis.toString)
                    val event = new HogEvent(new HogFlow(flowMap,myIP,"255.255.255.255"))
                    
                    event.data.put("hostname", myIP)
                    event.data.put("bytesUp", (bytesUp*sampleRate).toString)
                    event.data.put("bytesDown", (bytesDown*sampleRate).toString)
                    event.data.put("numberPkts", numberPkts.toString)
                    event.data.put("connections", connections.toString)
                    event.data.put("stringFlows", setFlows2String(flowSet))
                    
                    populateSMTPTalker(event).alert()
           }

  }
  
 /*
  * FTP, etc.. Talkers - tested
  *   
  */ 
  println("")
  println("FTP Talker")

    logger.info("=================FTP Talker ======== ")
    System.out.println("======== FTP Talker =====")

  val ftpTalkersCollection:PairRDDFunctions[(String,String), 
                                            (Long,Long,Long,HashSet[(String,String,String,String,String,Long,Long,Long,Int,Long,Long,Long,Int)],Long,Long)] = 
  sflowSummary
  .filter({case ((myIP,myPort,alienIP,alienPort,proto),(bytesUp,bytesDown,numberPkts,direction,beginTime,endTime,sampleRate,status)) 
                  =>  proto.equals("TCP") &
                      ( myPort.equals("21") |
                        alienPort.equals("21") ) 
         })
  .map({
      case ((myIP,myPort,alienIP,alienPort,proto),(bytesUp,bytesDown,numberPkts,direction,beginTime,endTime,sampleRate,status)) =>
         val flowSet:HashSet[(String,String,String,String,String,Long,Long,Long,Int,Long,Long,Long,Int)] = new HashSet()
         flowSet.add((myIP,myPort,alienIP,alienPort,proto,bytesUp,bytesDown,numberPkts,direction,beginTime,endTime,sampleRate,status))
        ((myIP,alienIP),(bytesUp,bytesDown,numberPkts,flowSet,1L,sampleRate))
      })
  
  val ftpTalkers =
  ftpTalkersCollection
  .reduceByKey({
        case ((bytesUpA,bytesDownA,numberPktsA,flowSetA,numberOfflowsA,sampleRateA),(bytesUpB,bytesDownB,numberPktsB,flowSetB,numberOfflowsB,sampleRateB)) =>
             (bytesUpA+bytesUpB,bytesDownA+bytesDownB, numberPktsA+numberPktsB, flowSetA++flowSetB, numberOfflowsA+numberOfflowsB,(sampleRateA+sampleRateB)/2)
              })
  .map({
         case ((myIP,alienIP),(bytesUp,bytesDown,numberPkts,flowSet,numberOfflows,sampleRate)) =>
              println("FTP Communication "+myIP+ " <?> "+alienIP)
              (myIP,alienIP)
      }).collect().toSet
  
      
      
  println("")
  println("FTP Servers")

    logger.info("=================FTP Servers ======== ")
    System.out.println("======== FTP Servers =====")

  val ftpServers = HogHBaseHistogram.getIPListHIST01(spark,"21")
  
      
      
 /*
  * P2P Communication - tested
  *   
  */
  
  println("")
  println("P2P Communication")

    logger.info("=================P2P Communication = always = " + disable_p2p)
    System.out.println("======== P2P Communication =====")

  val p2pTalkersCollection:PairRDDFunctions[(String,String), 
                                            (Long,Long,Long,HashSet[(String,String,String,String,String,Long,Long,Long,Int,Long,Long,Long,Int)],Long,Long)] = 
  sflowSummary
  .filter({case ((myIP,myPort,alienIP,alienPort,proto),(bytesUp,bytesDown,numberPkts,direction,beginTime,endTime,sampleRate,status)) =>
              myPort.toInt > 10000 &
              alienPort.toInt > 10000 &
              numberPkts > 1 &
              !p2pExcludedIPs.contains(myIP) &
              !p2pExcludedIPs.contains(alienIP)
         })
  .map({
      case ((myIP,myPort,alienIP,alienPort,proto),(bytesUp,bytesDown,numberPkts,direction,beginTime,endTime,sampleRate,status)) =>
         val flowSet:HashSet[(String,String,String,String,String,Long,Long,Long,Int,Long,Long,Long,Int)] = new HashSet()
         flowSet.add((myIP,myPort,alienIP,alienPort,proto,bytesUp,bytesDown,numberPkts,direction,beginTime,endTime,sampleRate,status))
        ((myIP,alienIP),(bytesUp,bytesDown,numberPkts,flowSet,1L,sampleRate))
      })
  
  val p2pTalkers1st =
  p2pTalkersCollection
  .reduceByKey({
        case ((bytesUpA,bytesDownA,numberPktsA,flowSetA,numberOfflowsA,sampleRateA),(bytesUpB,bytesDownB,numberPktsB,flowSetB,numberOfflowsB,sampleRateB)) =>
             (bytesUpA+bytesUpB,bytesDownA+bytesDownB, numberPktsA+numberPktsB, flowSetA++flowSetB, numberOfflowsA+numberOfflowsB,(sampleRateA+sampleRateB)/2)
              })
  .filter({
         case ((myIP,alienIP),(bytesUp,bytesDown,numberPkts,flowSet,numberOfflows,sampleRate)) =>
             !isMyIP(alienIP, myNets) & // Avoid internal communication
             !ftpTalkers.contains((myIP, alienIP)) // Avoid FTP communication
          })
  .map({
         case ((myIP,alienIP),(bytesUp,bytesDown,numberPkts,flowSet,numberOfflows,sampleRate)) =>
              (myIP,(bytesUp,bytesDown,numberPkts,flowSet,numberOfflows,1L,sampleRate))
      })
  .reduceByKey({
          case ((bytesUpA,bytesDownA,numberPktsA,flowSetA,numberOfflowsA,pairsA,sampleRateA),(bytesUpB,bytesDownB,numberPktsB,flowSetB,numberOfflowsB,pairsB,sampleRateB)) =>
               (bytesUpA+bytesUpB,bytesDownA+bytesDownB, numberPktsA+numberPktsB, flowSetA++flowSetB, numberOfflowsA+numberOfflowsB, pairsA+pairsB,(sampleRateA+sampleRateB)/2)
              })
  .filter({ case (myIP,(bytesUp,bytesDown,numberPkts,flowSet,numberOfflows,pairs,sampleRate)) => {
              val ports =  flowSet
                .map({ case (myIP, myPort, alienIP, alienPort, proto, bytesUp, bytesDown, numberPkts, direction, beginTime, endTime, sampleRate, status) => myPort })
                .toList.distinct.size

              val test = pairs > p2pPairsThreshold &
                ports > p2pMyPortsThreshold &
                !ftpServers.contains(myIP)

              test
            }
         })
  .map({
    case (myIP, (bytesUp, bytesDown, numberPkts, flowSet, numberOfflows, numberOfPairs, sampleRate)) =>
      {
        //p2pTalkers.add(myIP)

          println("MyIP: " + myIP + " - P2P Communication, number of pairs: " + numberOfPairs)

          val flowMap: Map[String, String] = new HashMap[String, String]
          flowMap.put("flow:id", System.currentTimeMillis.toString)
          val event = new HogEvent(new HogFlow(flowMap, myIP, "255.255.255.255"))
          event.data.put("numberOfPairs", numberOfPairs.toString)
          event.data.put("myIP", myIP)
          event.data.put("bytesUp", (bytesUp * sampleRate).toString)
          event.data.put("bytesDown", (bytesDown * sampleRate).toString)
          event.data.put("numberPkts", numberPkts.toString)
          event.data.put("stringFlows", setFlows2String(flowSet))

          if (disable_p2p == 0) {
            populateP2PCommunication(event).alert()
          }
          myIP

      }
     }).collect().toSet



    logger.info("=================P2P Communication - 2nd method = always = " + disable_p2p)
    System.out.println("======== P2P Communication - 2nd method =====")

  println("P2P Communication - 2nd method")
  val p2pTalkers2ndMethodCollection:PairRDDFunctions[(String,String), (Long,Long,Long,HashSet[(String,String,String,String,String,Long,Long,Long,Int,Long,Long,Long,Int)],Long,Long)] =
    sflowSummary
    .filter({case ((myIP,myPort,alienIP,alienPort,proto),(bytesUp,bytesDown,numberPkts,direction,beginTime,endTime,sampleRate,status))
                  => {
                    System.out.println("P2P Communication-v2:"+proto+" "+myPort+ " " + alienPort + " " + numberPkts)
                    var test =
                    proto.equals("UDP") &
                      myPort.toInt < 10000 &
                      myPort.toInt > 1000 &
                      alienPort.toInt < 10000 &
                      alienPort.toInt > 1000 &
                      numberPkts > 1 &
                      !p2pExcludedIPs.contains(myIP) &
                      !p2pExcludedIPs.contains(alienIP)
                    test
                  }
           })
    .map({
      case ((myIP,myPort,alienIP,alienPort,proto),(bytesUp,bytesDown,numberPkts,direction,beginTime,endTime,sampleRate,status)) =>
         val flowSet:HashSet[(String,String,String,String,String,Long,Long,Long,Int,Long,Long,Long,Int)] = new HashSet()
         flowSet.add((myIP,myPort,alienIP,alienPort,proto,bytesUp,bytesDown,numberPkts,direction,beginTime,endTime,sampleRate,status))
        ((myIP,alienIP),(bytesUp,bytesDown,numberPkts,flowSet,1L,sampleRate))
        })

  val p2pTalkers2nd = p2pTalkers2ndMethodCollection
  .reduceByKey({
         case ((bytesUpA,bytesDownA,numberPktsA,flowSetA,numberOfflowsA,sampleRateA),(bytesUpB,bytesDownB,numberPktsB,flowSetB,numberOfflowsB,sampleRateB)) =>
              (bytesUpA+bytesUpB,bytesDownA+bytesDownB, numberPktsA+numberPktsB, flowSetA++flowSetB, numberOfflowsA+numberOfflowsB,(sampleRateA+sampleRateB)/2)
              })
  .filter({
         case ((myIP,alienIP),(bytesUp,bytesDown,numberPkts,flowSet,numberOfflows,sampleRate)) =>
               !isMyIP(alienIP,myNets) &
               !p2pTalkers1st.contains(myIP)
          })
  .map({
         case ((myIP,alienIP),(bytesUp,bytesDown,numberPkts,flowSet,numberOfflows,sampleRate)) =>
              (myIP,(bytesUp,bytesDown,numberPkts,flowSet,numberOfflows,1L,sampleRate))
      })
  .reduceByKey({
         case ((bytesUpA,bytesDownA,numberPktsA,flowSetA,numberOfflowsA,pairsA,sampleRateA),(bytesUpB,bytesDownB,numberPktsB,flowSetB,numberOfflowsB,pairsB,sampleRateB)) =>
              (bytesUpA+bytesUpB,bytesDownA+bytesDownB, numberPktsA+numberPktsB, flowSetA++flowSetB, numberOfflowsA+numberOfflowsB, pairsA+pairsB,(sampleRateA+sampleRateB)/2)
              })
  .filter({ case (myIP,(bytesUp,bytesDown,numberPkts,flowSet,numberOfflows,pairs,sampleRate)) => {
                System.out.println("P2P Communication -v2-filter:"+pairs+" "+p2pPairs2ndMethodThreshold+" "+p2pDistinctPorts2ndMethodThreshold)
                var test = pairs > p2pPairs2ndMethodThreshold &
                  flowSet.map(_._4).toList.distinct.size > p2pDistinctPorts2ndMethodThreshold &
                  bytesDown + bytesUp > p2pBytes2ndMethodThreshold &
                  !ftpServers.contains(myIP)
                test
              }
          })
    .map({
      case (myIP,(bytesUp,bytesDown,numberPkts,flowSet,numberOfflows,numberOfPairs,sampleRate)) => {
        println("MyIP: " + myIP + " - P2P Communication 2nd method, number of pairs: " + numberOfPairs)

        val flowMap: Map[String, String] = new HashMap[String, String]
        flowMap.put("flow:id", System.currentTimeMillis.toString)
        val event = new HogEvent(new HogFlow(flowMap, myIP, "255.255.255.255"))
        event.data.put("numberOfPairs", numberOfPairs.toString)
        event.data.put("myIP", myIP)
        event.data.put("bytesUp", (bytesUp * sampleRate).toString)
        event.data.put("bytesDown", (bytesDown * sampleRate).toString)
        event.data.put("numberPkts", numberPkts.toString)
        event.data.put("stringFlows", setFlows2String(flowSet))

        if (disable_p2p == 0)
          populateP2PCommunication(event).alert()

        myIP
      }
    }).collect().toSet
    
    val p2pTalkers = p2pTalkers1st ++ p2pTalkers2nd
  
  
  /**
   * 
   * Media Streaming Client - tested
   * 
   */

    logger.info("=================Media streaming clients = " + disable_mediaStreaming)
    System.out.println("======== Media streaming clients =====")

  println("Media streaming clients")
  val mediaClientCollection:PairRDDFunctions[(String,String), (Long,Long,Long,HashSet[(String,String,String,String,String,Long,Long,Long,Int,Long,Long,Long,Int)],
                                            Long,Long,Long,Long)] = 
    sflowSummary
    .filter({case ((myIP,myPort,alienIP,alienPort,proto),(bytesUp,bytesDown,numberPkts,direction,beginTime,endTime,sampleRate,status)) 
                  =>  {
           // System.out.println("Media streaming clients"+proto+" "+myPort+" "+numberPkts)
            var test = proto.equals("TCP")      &
              myPort.toInt > 1000      &
              alienPort.toInt < 10000  &
              alienPort.toInt > 1000   &
              numberPkts > 1           &
              !myPort.equals("1194")   &
              !alienPort.equals("1194") &
              !mediaStreamingExcludedIPs.contains(myIP) &
              !mediaStreamingExcludedIPs.contains(alienIP)
             test
            }
           })
    .map({
      case ((myIP,myPort,alienIP,alienPort,proto),(bytesUp,bytesDown,numberPkts,direction,beginTime,endTime,sampleRate,status)) =>
           val flowSet:HashSet[(String,String,String,String,String,Long,Long,Long,Int,Long,Long,Long,Int)] = new HashSet()
           flowSet.add((myIP,myPort,alienIP,alienPort,proto,bytesUp,bytesDown,numberPkts,direction,beginTime,endTime,sampleRate,status))
           ((myIP,alienIP),(bytesUp,bytesDown,numberPkts,flowSet,1L,beginTime,endTime,sampleRate))
        })
  
  val mediaStreamingClients = 
  mediaClientCollection
  .reduceByKey({
      case ((bytesUpA,bytesDownA,numberPktsA,flowSetA,numberOfflowsA,beginTimeA,endTimeA,sampleRateA),(bytesUpB,bytesDownB,numberPktsB,flowSetB,numberOfflowsB,beginTimeB,endTimeB,sampleRateB)) =>
           (bytesUpA+bytesUpB,bytesDownA+bytesDownB, numberPktsA+numberPktsB, flowSetA++flowSetB, numberOfflowsA+numberOfflowsB,beginTimeA.min(beginTimeB),endTimeA.max(endTimeB),(sampleRateA+sampleRateB)/2)
              })
  .filter({
           case ((myIP,alienIP),(bytesUp,bytesDown,numberPkts,flowSet,numberOfflows,beginTime,endTime,sampleRate)) => {
             var test = !isMyIP(alienIP, myNets) &
               !p2pTalkers.contains(myIP) &
               (endTime - beginTime) > mediaClientCommunicationDurationThreshold &
               (endTime - beginTime) < mediaClientCommunicationDurationMAXThreshold
             test
           }
          })
  .map({
          case ((myIP,alienIP),(bytesUp,bytesDown,numberPkts,flowSet,numberOfflows,beginTime,endTime,sampleRate)) =>
               (myIP,(bytesUp,bytesDown,numberPkts,flowSet,numberOfflows,1L,sampleRate))
      })
  .reduceByKey({
          case ((bytesUpA,bytesDownA,numberPktsA,flowSetA,numberOfflowsA,pairsA,sampleRateA),(bytesUpB,bytesDownB,numberPktsB,flowSetB,numberOfflowsB,pairsB,sampleRateB)) =>
               (bytesUpA+bytesUpB,bytesDownA+bytesDownB, numberPktsA+numberPktsB, flowSetA++flowSetB, numberOfflowsA+numberOfflowsB, pairsA+pairsB,(sampleRateA+sampleRateB)/2)
              })
  .filter({ case (myIP,(bytesUp,bytesDown,numberPkts,flowSet,numberOfflows,pairs,sampleRate)) => {
          var test = pairs < mediaClientPairsThreshold &
            bytesUp * sampleRate < mediaClientUploadThreshold &
            bytesDown * sampleRate >= mediaClientDownloadThreshold
          test
        }
          })
    .map({ 
      case (myIP,(bytesUp,bytesDown,numberPkts,flowSet,numberOfflows,numberOfPairs,sampleRate)) => {
        println("MyIP: " + myIP + " - Media streaming client, number of pairs: " + numberOfPairs)

        val flowMap: Map[String, String] = new HashMap[String, String]
        flowMap.put("flow:id", System.currentTimeMillis.toString)
        val event = new HogEvent(new HogFlow(flowMap, myIP, "255.255.255.255"))
        event.data.put("numberOfPairs", numberOfPairs.toString)
        event.data.put("myIP", myIP)
        event.data.put("bytesUp", (bytesUp * sampleRate).toString)
        event.data.put("bytesDown", (bytesDown * sampleRate).toString)
        event.data.put("numberPkts", numberPkts.toString)
        event.data.put("connections", flowSet.size.toString)
        event.data.put("stringFlows", setFlows2String(flowSet))

        if (disable_mediaStreaming == 0) {
          populateMediaClient(event).alert()
        }

        myIP
      }
    }).collect().toSet

  
  
 
 /*
  * 
  * Port Histogram - Atypical TCP port used - tested
  * 
  * 
  */
  
 val proxyServers = HogHBaseReputation.getReputationList("ProxyServer", "whitelist")

    logger.info("=================Atypical TCP port used = always = " + disable_atypicalPorts)
    System.out.println("======== Atypical TCP port used =====")

 if(disable_atypicalPorts<=1)
 {
  println("")
  println("Atypical TCP port used")
  
  
 val atypicalTCPCollection: PairRDDFunctions[String, (Long,Long,Long,HashSet[(String,String,String,String,String,Long,Long,Long,Int,Long,Long,Long,Int)],
                                             Map[String,Double],Long,Long)] = 
    sflowSummary
    .filter({case ((myIP,myPort,alienIP,alienPort,proto),(bytesUp,bytesDown,numberPkts,direction,beginTime,endTime,sampleRate,status)) 
                  => { //direction  < 0 & // Algorithm implemented below to dig this information in another interesting form

                var test = !ftpTalkers.contains((myIP, alienIP)) &
                  numberPkts >= AtypicalTCPMinPkts &
                  //bytesUp > 0 &
                  //bytesDown > 0 &
                  status > 0 & // PSH-ACK or SYN-ACK flags or ACK from MyHost
                  !atypicalPortsExcludedIPs.contains(myIP) &
                  !atypicalPortsExcludedIPs.contains(alienIP)
                test
              }
           })
    .map({
      case ((myIP,myPort,alienIP,alienPort,proto),(bytesUp,bytesDown,numberPkts,direction,beginTime,endTime,sampleRate,status)) =>
         val flowSet:HashSet[(String,String,String,String,String,Long,Long,Long,Int,Long,Long,Long,Int)] = new HashSet()
         flowSet.add((myIP,myPort,alienIP,alienPort,proto,bytesUp,bytesDown,numberPkts,direction,beginTime,endTime,sampleRate,status))
         
         val histogram: Map[String,Double] = new HashMap()
         histogram.put(myPort,1D)
         
        (myIP,(bytesUp,bytesDown,numberPkts,flowSet,histogram,1L,sampleRate))
        
        })
  
  
     atypicalTCPCollection
     .reduceByKey({
       case ((bytesUpA,bytesDownA,numberPktsA,flowSetA,histogramA,numberOfFlowsA,sampleRateA),
             (bytesUpB,bytesDownB,numberPktsB,flowSetB,histogramB,numberOfFlowsB,sampleRateB)) =>
      
               histogramB./:(0){case  (c,(key,qtdH))=> val qtdH2 = {if(histogramA.get(key).isEmpty) 0D else histogramA.get(key).get }
                                                        histogramA.put(key,  qtdH2 + qtdH) 
                                                        0
                                 }
               (bytesUpA+bytesUpB, bytesDownA+bytesDownB, numberPktsA+numberPktsB, flowSetA++flowSetB, histogramA, numberOfFlowsA+numberOfFlowsB,(sampleRateA+sampleRateB)/2)
            })
     .map({ case (myIP,(bytesUp,bytesDown,numberPkts,flowSet,histogram,numberOfFlows,sampleRate)) =>
    
                            (myIP,(bytesUp,bytesDown,numberPkts,flowSet,histogram.map({ case (port,qtdC) => (port,qtdC/numberOfFlows.toDouble) }),numberOfFlows,sampleRate))
          })
    .filter({case (myIP,(bytesUp,bytesDown,numberPkts,flowSet,histogram,numberOfFlows,sampleRate)) =>
                   !p2pTalkers.contains(myIP)  // Avoid P2P talkers
           })
    .foreach{case (myIP,(bytesUp,bytesDown,numberPkts,flowSet,histogram1,numberOfFlows,sampleRate)) => 
      
                 // Remove ports used to connect as client, and not to serve
                    val newHistogram = 
                    histogram1.filter({
                        case (port,weight) =>
                          
                          if(proxyServers.contains(myIP)) // Proxies have a different approach
                          {
                            if( flowSet
                                 .filter({case (myIP,myPort,alienIP,alienPort,proto,bytesUp,bytesDown,numberPkts,direction,beginTime,endTime,sampleRate,status) =>
                                             myPort.equals(port) &
                                             direction < 0
                                        }).size >0 )
                            {
                              true
                            }else
                            {
                              false
                            }
                          }else
                          {
                              val alienPorts =
                              flowSet
                                .filter({
                                        case (myIP,myPort,alienIP,alienPort,proto,bytesUp,bytesDown,numberPkts,direction,beginTime,endTime,sampleRate,status) =>
                                             myPort.equals(port)
                                       })
                                .map({
                                      case (myIP,myPort,alienIP,alienPort,proto,bytesUp,bytesDown,numberPkts,direction,beginTime,endTime,sampleRate,status) =>
                                           (alienPort,1L)
                                    })
                                .groupBy(_._1)
                                .map({
                                      case (group,traversable) =>
                                           traversable.reduce({(a,b) => (a._1,a._2+b._2)})
                                   })
                                .map({
                                      case (alienPort,qtd) =>
                                           alienPort
                                   })
                                
                               val qtdAlienPorts = alienPorts.size
    
                                
                               val numberOfFlowsUsingThisMyPort =
                                 flowSet.filter({case (myIP,myPort,alienIP,alienPort,proto,bytesUp,bytesDown,numberPkts,direction,beginTime,endTime,sampleRate,status) =>
                                   myPort.equals(port)               
                                 }).size
                              
                               if( ( qtdAlienPorts >3 ) &
                                   ( (qtdAlienPorts.toDouble/numberOfFlowsUsingThisMyPort.toDouble) > 0.2  )  
                                 ) {

                                 val alienPortsSize = alienPorts.filter({ p => p.toLong < 1024 }).size

                                 if (alienPortsSize > 2 &
                                   ((qtdAlienPorts.toDouble / alienPortsSize.toDouble) > 0.5)
                                 ) // Proxy detected
                                 {
                                   // Save Proxy
                                   HogHBaseReputation.saveReputationList("ProxyServer", "whitelist", myIP)
                                   false
                                 } else {
                                   true
                                 }
                               }
                               else
                                 false
                          }
                    })  
                    
                    
                    val hogHistogram=HogHBaseHistogram.getHistogram("HIST01-"+myIP)
                    
                    if(hogHistogram.histSize < 100)
                    {
                      //println("IP: "+dstIP+ "  (N:"+qtd+",S:"+hogHistogram.histSize+") - Learn More!")
                      HogHBaseHistogram.saveHistogram(Histograms.merge(hogHistogram, new HogHistogram("",numberOfFlows,newHistogram)))
                    }else
                    {
                          //val KBDistance = Histograms.KullbackLiebler(hogHistogram.histMap, map)
                          val atypical   = Histograms.atypical(hogHistogram.histMap, newHistogram)
                                                     .filter { port =>   !atypicalTCPPort.contains(port) & // Exclude highly common FP
                                                                       ( !Histograms.isTypicalEvent(hogHistogram.histMap,"21") |
                                                                         port.toInt < 1024
                                                                        ) // Avoid FTP servers
                                                              } 

                          if(atypical.size>0)
                          {
                            println("Source IP: "+myIP+ "  (N:"+numberOfFlows+",S:"+hogHistogram.histSize+") - Atypical (open) source ports: "+atypical.mkString(","))
                            
                            
                            val flowMap: Map[String,String] = new HashMap[String,String]
                            flowMap.put("flow:id",System.currentTimeMillis.toString)
                            val event = new HogEvent(new HogFlow(flowMap,myIP,"255.255.255.255"))
                            event.data.put("myIP", myIP)
                            event.data.put("tcpport", atypical.mkString(","))
                            event.data.put("bytesUp", (bytesUp*sampleRate).toString)
                            event.data.put("bytesDown", (bytesDown*sampleRate).toString)
                            event.data.put("numberPkts", numberPkts.toString)
                            event.data.put("stringFlows", setFlows2String(flowSet.filter({p => atypical.contains(p._2)})))
                    
                             if(disable_atypicalPorts==0) {
                               populateAtypicalTCPPortUsed(event).alert()
                             }
                          }
                      HogHBaseHistogram.saveHistogram(Histograms.merge(hogHistogram, new HogHistogram("",numberOfFlows,newHistogram)))
                    }
             }
  
 }  
    
 /*
  * 
  * Port Histogram - Atypical alien TCP port used - tested
  * 
  * 
  */

    logger.info("=================Atypical alien TCP port used = always = " + disable_atypicalAlienPorts)
    System.out.println("======== Atypical alien TCP port used =====")

if(disable_atypicalAlienPorts<=1)
 {
  println("")
  println("Atypical alien TCP port used")
  
 val atypicalAlienTCPCollection: PairRDDFunctions[String, (Long,Long,Long,HashSet[(String,String,String,String,String,Long,Long,Long,Int,Long,Long,Long,Int)],
                                                  Map[String,Double],Long,Long)] = 
    sflowSummary
    .filter({case ((myIP,myPort,alienIP,alienPort,proto),(bytesUp,bytesDown,numberPkts,direction,beginTime,endTime,sampleRate,status)) 
                  =>  numberPkts >= AtypicalAlienTCPMinPkts     & 
                      alienPort.toLong < 10000 &
                      direction > -1           &
                      myPort.toLong > 1024     &
                      !myPort.equals("8080")   &
                      !isMyIP(alienIP,myNets)  &
                      !ftpTalkers.contains((myIP,alienIP)) & // Avoid FTP communication
                      proto.equals("TCP") &
                      status > 0 &
                      !atypicalAlienPortsExcludedIPs.contains(myIP) &
                      !atypicalAlienPortsExcludedIPs.contains(alienIP)
           })         
    .map({
      case ((myIP,myPort,alienIP,alienPort,proto),(bytesUp,bytesDown,numberPkts,direction,beginTime,endTime,sampleRate,status)) =>
         val flowSet:HashSet[(String,String,String,String,String,Long,Long,Long,Int,Long,Long,Long,Int)] = new HashSet()
         flowSet.add((myIP,myPort,alienIP,alienPort,proto,bytesUp,bytesDown,numberPkts,direction,beginTime,endTime,sampleRate,status))
         
         val histogram: Map[String,Double] = new HashMap()
         histogram.put(alienPort,1D)
         
        (myIP,(bytesUp,bytesDown,numberPkts,flowSet,histogram,1L,sampleRate))
        
        })
  
  
     atypicalAlienTCPCollection
     .reduceByKey({
       case ((bytesUpA,bytesDownA,numberPktsA,flowSetA,histogramA,numberOfFlowsA,sampleRateA),
             (bytesUpB,bytesDownB,numberPktsB,flowSetB,histogramB,numberOfFlowsB,sampleRateB)) =>
      
               histogramB./:(0){case  (c,(key,qtdH))=> val qtdH2 = {if(histogramA.get(key).isEmpty) 0D else histogramA.get(key).get }
                                                        histogramA.put(key,  qtdH2 + qtdH) 
                                                        0
                                 }
               (bytesUpA+bytesUpB, bytesDownA+bytesDownB, numberPktsA+numberPktsB, flowSetA++flowSetB, histogramA, numberOfFlowsA+numberOfFlowsB,(sampleRateA+sampleRateB)/2)
            })
     .map({ case (myIP,(bytesUp,bytesDown,numberPkts,flowSet,histogram,numberOfFlows,sampleRate)) =>
                 (myIP,(bytesUp,bytesDown,numberPkts,flowSet,
                      histogram.map({ case (port,qtdC) => (port,qtdC/numberOfFlows.toDouble) }),
                      numberOfFlows,sampleRate))
          })
    .filter({case (myIP,(bytesUp,bytesDown,numberPkts,flowSet,histogram,numberOfFlows,sampleRate)) =>
                   !p2pTalkers.contains(myIP) & // Avoid P2P talkers
                   !mediaStreamingClients.contains(myIP)  // Avoid media streaming clients
           })
    .foreach{case (myIP,(bytesUp,bytesDown,numberPkts,flowSet,histogram1,numberOfFlows,sampleRate)) => 
      
                    // Remove alienPorts used to connect by Aliens as clients
                    val alienClientPortsList=
                      flowSet.map({
                        case (myIP,myPort,alienIP,alienPort,proto,bytesUp,bytesDown,numberPkts,direction,beginTime,endTime,sampleRate,status) =>
                          (myPort,Set(alienPort),1L)
                      })
                      .groupBy(_._1)
                      .map({
                        case (group,traversable) =>
                         traversable.reduce({(a,b) => (a._1,a._2++b._2,a._3+b._3)})
                      })
                      .filter({case (myPort,alienPorts,qtd) => qtd>1})
                      .map(_._2)
                      
                    val alienClientPorts = 
                    {
                      if(alienClientPortsList.size>0)
                        alienClientPortsList.reduce({(a,b) => a++b})
                      else
                        Set("")
                    }
                      
      
                    val newHistogram = 
                    histogram1
                    .filter({
                        case (port,weight) =>
                          
                          if(alienClientPorts.contains(port))  
                                 false
                              else
                                 true
                    })  
                    
                    
                    val savedHogHistogram=HogHBaseHistogram.getHistogram("HIST02-"+myIP)
                    
                    if(savedHogHistogram.histSize < 1000)
                    {
                      //println("IP: "+dstIP+ "  (N:"+qtd+",S:"+hogHistogram.histSize+") - Learn More!")
                      HogHBaseHistogram.saveHistogram(Histograms.merge(savedHogHistogram, new HogHistogram("",numberOfFlows,newHistogram)))
                    }else
                    {
                         val savedLastHogHistogram=HogHBaseHistogram.getHistogram("HIST02.1-"+myIP)
                         
                         if(savedLastHogHistogram.histSize > 0)
                         {
                           
                           //val KBDistance = Histograms.KullbackLiebler(hogHistogram.histMap, map)
                           val atypical   = Histograms.atypical(savedHogHistogram.histMap, newHistogram)
                           val typical   = Histograms.typical(savedLastHogHistogram.histMap, newHistogram)

                            val newAtypical = 
                            atypical.filter({ atypicalAlienPort =>
                                               {
                                                  typical.contains(atypicalAlienPort)
                                                } &
                                                {
                                                  flowSet.filter(p => p._4.equals(atypicalAlienPort))
                                                  .map({ case (myIP,myPort,alienIP,alienPort,proto,bytesUp,bytesDown,numberPkts,direction,beginTime,endTime,sampleRate,status) => 
                                                             var savedAlienHistogram = new HogHistogram("",0,new HashMap[String,Double])
                                                             if(isMyIP(alienIP,myNets))
                                                             {
                                                               savedAlienHistogram = HogHBaseHistogram.getHistogram("HIST01-"+alienIP)
                                                             }                                                             
                                                             else
                                                             {                                                              
                                                               savedAlienHistogram = HogHBaseHistogram.getHistogram("HIST05-"+ipSignificantNetwork(alienIP))
                                                             }
                                                             
                                                             if(savedAlienHistogram.histSize<21)
                                                               false
                                                             
                                                             val histogramAlien      = new HashMap[String,Double]
                                                             histogramAlien.put(alienPort, 1D)
                                                             val atypicalAlien   = Histograms.atypical(savedAlienHistogram.histMap, histogramAlien)
                                                             if(atypicalAlien.size>0)
                                                                 true // Indeed, an atypical access
                                                             else
                                                                 false // No! The Alien was accessed before by someone else. It's not an atypical flow.
                                                           }).contains(true)
                                                }
                                            })
                            
                            if(newAtypical.size>0)
                            {
                            println("MyIP: "+myIP+ "  (N:"+numberOfFlows+",S:"+savedHogHistogram.histSize+") - Atypical alien ports: "+newAtypical.mkString(","))
                            
                            /*
                            println("Saved:")
                            hogHistogram.histMap./:(0){case  (c,(key,qtd))=>
                            println(key+": "+ qtd)
                            0
                            } 
                            println("Now:")
                            map./:(0){case  (c,(key,qtd))=>
                            println(key+": "+ qtd)
                            0
                            } 
                            * 
                            */
                            
                            val flowMap: Map[String,String] = new HashMap[String,String]
                            flowMap.put("flow:id",System.currentTimeMillis.toString)
                            val event = new HogEvent(new HogFlow(flowMap,myIP,"255.255.255.255"))
                            event.data.put("myIP", myIP)
                            event.data.put("tcpport", newAtypical.mkString(","))
                            event.data.put("bytesUp", (bytesUp*sampleRate).toString)
                            event.data.put("bytesDown", (bytesDown*sampleRate).toString)
                            event.data.put("numberPkts", numberPkts.toString)
                            event.data.put("stringFlows", setFlows2String(flowSet.filter({p => newAtypical.contains(p._4)})))
                    
                            if(disable_atypicalAlienPorts==0) {
                              populateAtypicalAlienTCPPortUsed(event).alert()
                            }
                          }
                           
                          HogHBaseHistogram.saveHistogram(Histograms.merge(savedHogHistogram, savedLastHogHistogram))

                         }
                      
                          
                      HogHBaseHistogram.saveHistogram(new HogHistogram("HIST02.1-"+myIP,numberOfFlows,newHistogram))
                    }
                    
             }
 
 }

      
 /*
  * 
  * Atypical number of pairs in the period - tested
  * 
  * 
  */

    logger.info("=================Atypical number of pairs in the period = always = " + disable_atypicalPairs)
    System.out.println("======== Atypical number of pairs in the period =====")

 if(disable_atypicalPairs<=1)
 {
  
  println("")
  println("Atypical number of pairs in the period")
 
  val atypicalNumberPairsCollection: PairRDDFunctions[(String,String), (Long,Long,Long,HashSet[(String,String,String,String,String,Long,Long,Long,Int,Long,Long,Long,Int)],Long,Long)] = 
    sflowSummary
    .filter({case ((myIP,myPort,alienIP,alienPort,proto),(bytesUp,bytesDown,numberPkts,direction,beginTime,endTime,sampleRate,status)) 
                  =>  numberPkts > 1 &
                      !atypicalPairsExcludedIPs.contains(myIP) &
                      !atypicalPairsExcludedIPs.contains(alienIP) 
           })
    .map({
      case ((myIP,myPort,alienIP,alienPort,proto),(bytesUp,bytesDown,numberPkts,direction,beginTime,endTime,sampleRate,status)) =>
         val flowSet:HashSet[(String,String,String,String,String,Long,Long,Long,Int,Long,Long,Long,Int)] = new HashSet()
         flowSet.add((myIP,myPort,alienIP,alienPort,proto,bytesUp,bytesDown,numberPkts,direction,beginTime,endTime,sampleRate,status))
        ((myIP,alienIP),(bytesUp,bytesDown,numberPkts,flowSet,1L,sampleRate))
        })
  
  val atypicalNumberPairsCollectionFinal=
  atypicalNumberPairsCollection
  .reduceByKey({
    case ((bytesUpA,bytesDownA,numberPktsA,flowSetA,numberOfflowsA,sampleRateA),(bytesUpB,bytesDownB,numberPktsB,flowSetB,numberOfflowsB,sampleRateB)) =>
      (bytesUpA+bytesUpB,bytesDownA+bytesDownB, numberPktsA+numberPktsB, flowSetA++flowSetB, numberOfflowsA+numberOfflowsB, (sampleRateA+sampleRateB)/2)
  })
  .map({
     case ((myIP,alienIP),(bytesUp,bytesDown,numberPkts,flowSet,numberOfflows,sampleRate)) =>
    
       (myIP,(bytesUp,bytesDown,numberPkts,flowSet,numberOfflows,1L,sampleRate))
  })
  .reduceByKey({
    case ((bytesUpA,bytesDownA,numberPktsA,flowSetA,numberOfflowsA,pairsA,sampleRateA),(bytesUpB,bytesDownB,numberPktsB,flowSetB,numberOfflowsB,pairsB,sampleRateB)) =>
      (bytesUpA+bytesUpB,bytesDownA+bytesDownB, numberPktsA+numberPktsB, flowSetA++flowSetB, numberOfflowsA+numberOfflowsB, pairsA+pairsB,(sampleRateA+sampleRateB)/2)
  })
  .filter{case (myIP,(bytesUp,bytesDown,numberPkts,flowSet,numberOfflows,numberOfPairs,sampleRate)) =>
                   !p2pTalkers.contains(myIP)// Avoid P2P talkers
           }.cache
  
  val pairsStats = 
  atypicalNumberPairsCollectionFinal
  .map({case (myIP,(bytesUp,bytesDown,numberPkts,flowSet,numberOfflows,numberOfPairs,sampleRate)) =>
    numberOfPairs
      }).stats()
  
  
  atypicalNumberPairsCollectionFinal
  .filter{case (myIP,(bytesUp,bytesDown,numberPkts,flowSet,numberOfflows,numberOfPairs,sampleRate)) =>
                   numberOfPairs > atypicalPairsThresholdMIN
           }
  .foreach{case  (myIP,(bytesUp,bytesDown,numberPkts,flowSet,numberOfflows,numberOfPairs,sampleRate)) => 
                    val savedHistogram=HogHBaseHistogram.getHistogram("HIST03-"+myIP)

                    val histogram = new HashMap[String,Double]
                    val key = floor(log(numberOfPairs.*(1000)+1D)).toString
                    histogram.put(key, 1D)
                    
                    if(savedHistogram.histSize< 10)
                    {
                      //println("MyIP: "+myIP+ "  (N:1,S:"+hogHistogram.histSize+") - Learn More!")
                      HogHBaseHistogram.saveHistogram(Histograms.merge(savedHistogram, new HogHistogram("",1L,histogram)))
                    }else
                    {
                          //val KBDistance = Histograms.KullbackLiebler(hogHistogram.histMap, map)
                          val atypical   = Histograms.atypical(savedHistogram.histMap, histogram)
                          val histMapFiltredSize = savedHistogram.histMap.filter({case (key,value) => value > 0.001D}).size
                          if(atypical.size>0 & histMapFiltredSize <5)
                          {
                            println("MyIP: "+myIP+ "  (N:1,S:"+savedHistogram.histSize+") - Atypical number of pairs in the period: "+numberOfPairs)
                            
                            val flowMap: Map[String,String] = new HashMap[String,String]
                            flowMap.put("flow:id",System.currentTimeMillis.toString)
                            val event = new HogEvent(new HogFlow(flowMap,myIP,"255.255.255.255"))
                            event.data.put("numberOfPairs",numberOfPairs.toString)
                            event.data.put("myIP", myIP)
                            event.data.put("bytesUp",   (bytesUp*sampleRate).toString)
                            event.data.put("bytesDown", (bytesDown*sampleRate).toString)
                            event.data.put("numberPkts", numberPkts.toString)
                            event.data.put("stringFlows", setFlows2String(flowSet))
                            event.data.put("pairsMean", pairsStats.mean.round.toString)
                            event.data.put("pairsStdev", pairsStats.stdev.round.toString)
                            
                            if(disable_atypicalPairs==0) {
                              populateAtypicalNumberOfPairs(event).alert()
                            }
                          }
                          
                          HogHBaseHistogram.saveHistogram(Histograms.merge(savedHistogram, new HogHistogram("",1L,histogram)))
                    }
             }
  
     
 } 
       
 /*
  * 
  * Atypical amount of data transfered - tested
  * 
  */

  logger.info("=================Atypical amount of data transfered = always = " + disable_atypicalData)
  System.out.println("======== Atypical amount of data transfered =====")

 if(disable_atypicalData<=1)
 {
  println("")
  println("Atypical amount of data transfered")
  
  val bigProviderNets = HogHBaseReputation.getReputationList("BigProvider", "whitelist" )
  
  val atypicalAmountDataCollection: PairRDDFunctions[(String,String), (Long,Long,Long,HashSet[(String,String,String,String,String,Long,Long,Long,Int,Long,Long,Long,Int)]
                                                      ,Long,Long)] = 
    sflowSummary
    .filter({case ((myIP,myPort,alienIP,alienPort,proto),(bytesUp,bytesDown,numberPkts,direction,beginTime,endTime,sampleRate,status)) 
                  => { //alienPort.toLong < 1204  &&
                  val test = direction > -1 &
                  myPort.toLong > 1024 &
                  !myPort.equals("8080") &
                  !isMyIP(alienIP, myNets) & // Exclude internal communication
                  !isMyIP(alienIP, bigProviderNets) & // Exclude bigProviders
                  !atypicalDataExcludedIPs.contains(myIP) &
                  !atypicalDataExcludedIPs.contains(alienIP)
                  test
              }
           })
    .map({
          case ((myIP,myPort,alienIP,alienPort,proto),(bytesUp,bytesDown,numberPkts,direction,beginTime,endTime,sampleRate,status)) =>
               val flowSet:HashSet[(String,String,String,String,String,Long,Long,Long,Int,Long,Long,Long,Int)] = new HashSet()
               flowSet.add((myIP,myPort,alienIP,alienPort,proto,bytesUp,bytesDown,numberPkts,direction,beginTime,endTime,sampleRate,status))
               ((myIP,alienIP),(bytesUp,bytesDown,numberPkts,flowSet,1L,sampleRate))
        })
  
  val atypicalAmountDataCollectionFinal =
  atypicalAmountDataCollection
  .reduceByKey({
                case ((bytesUpA,bytesDownA,numberPktsA,flowSetA,numberOfflowsA,sampleRateA),(bytesUpB,bytesDownB,numberPktsB,flowSetB,numberOfflowsB,sampleRateB)) =>
                     (bytesUpA+bytesUpB,bytesDownA+bytesDownB, numberPktsA+numberPktsB, flowSetA++flowSetB, numberOfflowsA+numberOfflowsB,(sampleRateA+sampleRateB)/2)
              })
  .map({
     case ((myIP,alienIP),(bytesUp,bytesDown,numberPkts,flowSet,numberOfflows,sampleRate)) =>
          (myIP,(bytesUp,bytesDown,numberPkts,flowSet,numberOfflows,1L,sampleRate))
  })
  .reduceByKey({
    case ((bytesUpA,bytesDownA,numberPktsA,flowSetA,numberOfflowsA,pairsA,sampleRateA),(bytesUpB,bytesDownB,numberPktsB,flowSetB,numberOfflowsB,pairsB,sampleRateB)) =>
         (bytesUpA+bytesUpB,bytesDownA+bytesDownB, numberPktsA+numberPktsB, flowSetA++flowSetB, numberOfflowsA+numberOfflowsB, pairsA+pairsB,(sampleRateA+sampleRateB)/2)
  })
  .filter{case (myIP,(bytesUp,bytesDown,numberPkts,flowSet,numberOfflows,numberOfPairs,sampleRate)) =>
                   !p2pTalkers.contains(myIP) & // Avoid P2P talkers
                   !mediaStreamingClients.contains(myIP)  // Avoid media streaming clients
         }.cache
  
  val dataStats = 
  atypicalAmountDataCollectionFinal
  .map({case (myIP,(bytesUp,bytesDown,numberPkts,flowSet,numberOfflows,numberOfPairs,sampleRate)) =>
        bytesUp
      }).stats()
  
  
  atypicalAmountDataCollectionFinal
  .filter{case (myIP,(bytesUp,bytesDown,numberPkts,flowSet,numberOfflows,numberOfPairs,sampleRate)) =>
               bytesUp*sampleRate > atypicalAmountDataThresholdMIN
         }
  .foreach{case  (myIP,(bytesUp,bytesDown,numberPkts,flowSet,numberOfflows,numberOfPairs,sampleRate)) => 
                    
                    val savedHistogram=HogHBaseHistogram.getHistogram("HIST04-"+myIP)
                    
                    val histogram = new HashMap[String,Double]
                    val key = floor(log(bytesUp.*(0.0001)+1D)).toString
                    histogram.put(key, 1D)
                    
                    if(savedHistogram.histSize< 30 )
                    {
                      //println("MyIP: "+myIP+ "  (N:1,S:"+hogHistogram.histSize+") - Learn More!")
                      HogHBaseHistogram.saveHistogram(Histograms.merge(savedHistogram, new HogHistogram("",1L,histogram)))
                    }else
                    {
                      

                          //val KBDistance = Histograms.KullbackLiebler(hogHistogram.histMap, map)
                          val atypical   = Histograms.atypical(savedHistogram.histMap, histogram)
                          val histMapFiltredSize = savedHistogram.histMap.filter({case (key,value) => value > 0.001D}).size

                          if(atypical.size>0 & histMapFiltredSize <5)
                          {
                            println("MyIP: "+myIP+ "  (N:1,S:"+savedHistogram.histSize+") - Atypical amount of sent bytes: "+bytesUp)
                            
                            val flowMap: Map[String,String] = new HashMap[String,String]
                            flowMap.put("flow:id",System.currentTimeMillis.toString)
                            val event = new HogEvent(new HogFlow(flowMap,myIP,"255.255.255.255"))
                            event.data.put("numberOfPairs",numberOfPairs.toString)
                            event.data.put("myIP", myIP)
                            event.data.put("bytesUp",   (bytesUp*sampleRate).toString)
                            event.data.put("bytesDown", (bytesDown*sampleRate).toString)
                            event.data.put("numberPkts", numberPkts.toString)
                            event.data.put("stringFlows", setFlows2String(flowSet))
                            event.data.put("dataMean", dataStats.mean.round.toString)
                            event.data.put("dataStdev", dataStats.stdev.round.toString)
                            
                            if(disable_atypicalData==0) {
                              populateAtypicalAmountData(event).alert()
                            }
                          }
                          
                          HogHBaseHistogram.saveHistogram(Histograms.merge(savedHistogram, new HogHistogram("",1L,histogram)))
                    }
             }
  
 }
 
   
 /*
  * 
  * Port Histogram - Atypical TCP port used
  * 
  * 
  */

  logger.info("=================Atypical TCP port used by Alien Network = always")
  System.out.println("======== Atypical TCP port used by Alien Network =====")

  println("")
  println("Atypical TCP port used by Alien Network")
          
 val atypicalAlienReverseTCPCollection: PairRDDFunctions[String, (Long,Long,Long,HashSet[(String,String,String,String,String,Long,Long,Long,Int,Long,Long,Long,Int)],
                                                        Map[String,Double],Long,Long)] = 
    sflowSummary
    .filter({case ((myIP,myPort,alienIP,alienPort,proto),(bytesUp,bytesDown,numberPkts,direction,beginTime,endTime,sampleRate,status)) 
                  => // numberPkts  >1   & // XXX: Test
                      myPort.toLong >1024 &
                      alienPort.toLong <10000 &
                      proto.equals("TCP") &
                      !isMyIP(alienIP,myNets) &  // Flow InternalIP <-> ExternalIP
                      !p2pTalkers.contains(myIP) & // Avoid P2P communication
                      !ftpTalkers.contains((myIP,alienIP))  
           })
    .map({
      case ((myIP,myPort,alienIP,alienPort,proto),(bytesUp,bytesDown,numberPkts,direction,beginTime,endTime,sampleRate,status)) =>
         val flowSet:HashSet[(String,String,String,String,String,Long,Long,Long,Int,Long,Long,Long,Int)] = new HashSet()
         flowSet.add((myIP,myPort,alienIP,alienPort,proto,bytesUp,bytesDown,numberPkts,direction,beginTime,endTime,sampleRate,status))
         
         val histogram: Map[String,Double] = new HashMap()
         histogram.put(alienPort,1D)
         
        (ipSignificantNetwork(alienIP),(bytesUp,bytesDown,numberPkts,flowSet,histogram,1L,sampleRate))
        
        })
  
  
     atypicalAlienReverseTCPCollection
     .reduceByKey({
       case ((bytesUpA,bytesDownA,numberPktsA,flowSetA,histogramA,numberOfFlowsA,sampleRateA),(bytesUpB,bytesDownB,numberPktsB,flowSetB,histogramB,numberOfFlowsB,sampleRateB)) =>
      
               histogramB./:(0){case  (c,(key,qtdH))=> val qtdH2 = {if(histogramA.get(key).isEmpty) 0D else histogramA.get(key).get }
                                                        histogramA.put(key,  qtdH2 + qtdH) 
                                                        0
                                 }
               (bytesUpA+bytesUpB, bytesDownA+bytesDownB, numberPktsA+numberPktsB, flowSetA++flowSetB, histogramA, numberOfFlowsA+numberOfFlowsB,(sampleRateA+sampleRateB)/2)
            })
     .map({ case (alienNetwork,(bytesUp,bytesDown,numberPkts,flowSet,histogram,numberOfFlows,sampleRate)) =>
                 (alienNetwork,(bytesUp,bytesDown,numberPkts,flowSet,
                     histogram.map({ case (port,qtdC) => (port,qtdC/numberOfFlows.toDouble) }),
                     numberOfFlows,sampleRate))
          })
    .foreach{case (alienNetwork,(bytesUp,bytesDown,numberPkts,flowSet,histogram,numberOfFlows,sampleRate)) => 
                    
                    //  Ports
                    val savedHogHistogram=HogHBaseHistogram.getHistogram("HIST05-"+alienNetwork)
                    
                    // Bytes
                    if({
                          flowSet
                          .map({case (myIP,myPort,alienIP,alienPort,proto,bytesUp,bytesDown,numberPkts,direction,beginTime,endTime,sampleRate,status) => myIP})
                          .toList.distinct.size>4
                    }) // Consider just if there are more than 4 distinct MyHosts as pairs 
                    {
                    
                        val histogramBytes = new HashMap[String,Double]
                        flowSet
                           .filter({case (myIP,myPort,alienIP,alienPort,proto,bytesUp,bytesDown,numberPkts,direction,beginTime,endTime,sampleRate,status) =>
                                    myPort.toInt > 1023
                                })
                           .map({case (myIP,myPort,alienIP,alienPort,proto,bytesUp,bytesDown,numberPkts,direction,beginTime,endTime,sampleRate,status) => 
                                         (floor(log((bytesUp*sampleRate).*(0.0001)+1D)).toString,1D)
                                })
                           .toMap
                           .groupBy(_._1)
                           .map({
                            case (group,traversable) =>
                                  traversable.reduce({(a,b) => (a._1,a._2+b._2)})
                               })
                           .map({case (key,value) => histogramBytes.put(key,value)})
                          
                          val savedHogHistogramBytes=HogHBaseHistogram.getHistogram("HIST06-"+alienNetwork)
                          HogHBaseHistogram.saveHistogram(Histograms.merge(savedHogHistogramBytes, new HogHistogram("",numberOfFlows,histogramBytes)))
                          
                          val maxBytesUp=
                            flowSet
                           .map({case (myIP,myPort,alienIP,alienPort,proto,bytesUp,bytesDown,numberPkts,direction,beginTime,endTime,sampleRate,status) => 
                                         bytesUp*sampleRate
                                }).max
                           if(maxBytesUp>bigProviderThreshold)
                           {
                              HogHBaseReputation.saveReputationList("BigProvider", "whitelist", alienNetwork)
                           }
                               
                       
                        // Bytes End
                        
                    
                    
                        if(savedHogHistogram.histSize < 1000)
                        {
                          //println("IP: "+dstIP+ "  (N:"+qtd+",S:"+hogHistogram.histSize+") - Learn More!")
                          HogHBaseHistogram.saveHistogram(Histograms.merge(savedHogHistogram, new HogHistogram("",numberOfFlows,histogram)))
                        }else
                        {
                              //val KBDistance = Histograms.KullbackLiebler(hogHistogram.histMap, map)
                              val atypical   = Histograms.atypical(savedHogHistogram.histMap, histogram)
    
                              if(atypical.size>0)
                              {
                                println("Alien Network: "+alienNetwork+ "  (N:"+numberOfFlows+",S:"+savedHogHistogram.histSize+") - Atypical alien network port used: "+atypical.mkString(","))
                                
                                /*
                                val flowMap: Map[String,String] = new HashMap[String,String]
                                flowMap.put("flow:id",System.currentTimeMillis.toString)
                                val event = new HogEvent(new HogFlow(flowMap,formatIPtoBytes(alienNetwork+".0"),
                                                                             InetAddress.getByName("255.255.255.255").getAddress))
                                event.data.put("myIP", myIP)
                                event.data.put("tcpport", atypical.mkString(","))
                                event.data.put("bytesUp", bytesUp.toString)
                                event.data.put("bytesDown", bytesDown.toString)
                                event.data.put("numberPkts", numberPkts.toString)
                                event.data.put("stringFlows", setFlows2String(flowSet.filter({p => atypical.contains(p._2)})))
                        
                                populateAtypicalTCPPortUsed(event).alert(hogHBaseRDD)
                                */
                              }
                          HogHBaseHistogram.saveHistogram(Histograms.merge(savedHogHistogram, new HogHistogram("",numberOfFlows,histogram)))
                        }
                        
                     }
             }
  
  
       
 /*
  * 
  * Alien accessing too much hosts - tested
  * 
  */

  logger.info("================= Aliens accessing  = always ="+disable_alien)
  System.out.println("======== Aliens accessing =====")

 if(disable_alien<=1)
 {   
  println("")
  println("Aliens accessing more than "+alienThreshold+" hosts")
 
  
   val alienTooManyPairsCollection: PairRDDFunctions[(String,String), (Long,Long,Long,HashSet[(String,String,String,String,String,Long,Long,Long,Int,Long,Long,Long,Int)],
                                                     Long,Long)] = 
    sflowSummary
    .filter({ case ((myIP,myPort,alienIP,alienPort,proto),(bytesUp,bytesDown,numberPkts,direction,beginTime,endTime,sampleRate,status)) 
                 => {
      val test = direction < 0 &
        !isMyIP(alienIP, myNets) &
        !alienExcludedIPs.contains(myIP) &
        !alienExcludedIPs.contains(alienIP)
      System.out.println("===Alien accessing too much hosts=="+direction+" "+test)
      test
    }
            })
    .map({
      case ((myIP,myPort,alienIP,alienPort,proto),(bytesUp,bytesDown,numberPkts,direction,beginTime,endTime,sampleRate,status)) =>
         val flowSet:HashSet[(String,String,String,String,String,Long,Long,Long,Int,Long,Long,Long,Int)] = new HashSet()
         flowSet.add((myIP,myPort,alienIP,alienPort,proto,bytesUp,bytesDown,numberPkts,direction,beginTime,endTime,sampleRate,status))
        ((myIP,alienIP),(bytesUp,bytesDown,numberPkts,flowSet,1L,sampleRate))
        })
  
  alienTooManyPairsCollection
  .reduceByKey({
       case ((bytesUpA,bytesDownA,numberPktsA,flowSetA,numberOfflowsA,sampleRateA),(bytesUpB,bytesDownB,numberPktsB,flowSetB,numberOfflowsB,sampleRateB)) =>
            (bytesUpA+bytesUpB, bytesDownA+bytesDownB, numberPktsA+numberPktsB, flowSetA++flowSetB, numberOfflowsA+numberOfflowsB, (sampleRateA+sampleRateB)/2)
              })
  .map({
       case ((myIP,alienIP),(bytesUp,bytesDown,numberPkts,flowSet,numberOfflows,sampleRate)) =>
            (alienIP,(bytesUp,bytesDown,numberPkts,flowSet,numberOfflows,1L,sampleRate))
      })
  .reduceByKey({
       case ((bytesUpA,bytesDownA,numberPktsA,flowSetA,numberOfflowsA,pairsA,sampleRateA),(bytesUpB,bytesDownB,numberPktsB,flowSetB,numberOfflowsB,pairsB,sampleRateB)) =>
            (bytesUpA+bytesUpB, bytesDownA+bytesDownB, numberPktsA+numberPktsB, flowSetA++flowSetB, numberOfflowsA+numberOfflowsB, pairsA+pairsB, (sampleRateA+sampleRateB)/2)
  })
  .foreach{case  (alienIP,(bytesUp,bytesDown,numberPkts,flowSet,numberOfflows,numberOfPairs,sampleRate)) => 
                    if(numberOfPairs > alienThreshold && !alienIP.equals("0.0.0.0") )
                    {                     
                            println("AlienIP: "+alienIP+ " more than "+alienThreshold+" pairs in the period: "+numberOfPairs)
                         
                            val flowMap: Map[String,String] = new HashMap[String,String]
                            flowMap.put("flow:id",System.currentTimeMillis.toString)
                            val event = new HogEvent(new HogFlow(flowMap,alienIP,"255.255.255.255"))
                            
                            event.data.put("numberOfPairs",numberOfPairs.toString)
                            event.data.put("alienIP", alienIP)
                            event.data.put("bytesUp",   (bytesUp*sampleRate).toString)
                            event.data.put("bytesDown", (bytesDown*sampleRate).toString)
                            event.data.put("numberPkts", numberPkts.toString)
                            event.data.put("stringFlows", setFlows2String(flowSet))
                            event.data.put("ports",flowSet
                                              .map({case (myIP,myPort,alienIP,alienPort,proto,bytesUp,bytesDown,numberPkts,direction,beginTime,endTime,sampleRate,status) =>
                                                    (proto,myPort)
                                                   }).toArray
                                                   .distinct
                                                   .map({case (proto,myPort) => proto+"/"+myPort})
                                                   .mkString(", ")
                                            )
                            
                            if(disable_alien==0) {
                              populateAlienAccessingManyHosts(event).alert()
                            }
                    }
           }
 }
 
  /*
   * 
   * UDP amplifier (DDoS) - tested
   * 
   */

 logger.info("================= UDP amplifier (DDoS) = always ="+disable_UDPAmplifier)
 System.out.println("======== UDP amplifier (DDoS) =====")

 if(disable_UDPAmplifier<=1)
 { 
  println("")
  println("UDP amplifier (DDoS)")
  
   val udpAmplifierCollection: PairRDDFunctions[String, 
                                                (Long,Long,Long,HashSet[(String,String,String,String,String,Long,Long,Long,Int,Long,Long,Long,Int)],
                                                Long,Long)] = sflowSummary
    .filter({case ((myIP,myPort,alienIP,alienPort,proto),(bytesUp,bytesDown,numberPkts,direction,beginTime,endTime,sampleRate,status)) 
                  => (  myPort.equals("19")   |
                        myPort.equals("53")   |
                        myPort.equals("123")  |
                        myPort.equals("1900")
                      ) &
                      proto.equals("UDP")      &
                      bytesUp/numberPkts > 250   &
                      !isMyIP(alienIP,myNets)  &
                    !UDPAmplifierExcludedIPs.contains(myIP) &
                    !UDPAmplifierExcludedIPs.contains(alienIP)                     
           })
    .map({
          case ((myIP,myPort,alienIP,alienPort,proto),(bytesUp,bytesDown,numberPkts,direction,beginTime,endTime,sampleRate,status)) =>
               val flowSet:HashSet[(String,String,String,String,String,Long,Long,Long,Int,Long,Long,Long,Int)] = new HashSet()
               flowSet.add((myIP,myPort,alienIP,alienPort,proto,bytesUp,bytesDown,numberPkts,direction,beginTime,endTime,sampleRate,status))
               (myIP,(bytesUp,bytesDown,numberPkts,flowSet,1L,sampleRate))
        })
  
  
  udpAmplifierCollection
    .reduceByKey({
                   case ((bytesUpA,bytesDownA,numberPktsA,flowSetA,connectionsA,sampleRateA),(bytesUpB,bytesDownB,numberPktsB,flowSetB,connectionsB,sampleRateB)) =>
                        (bytesUpA+bytesUpB,bytesDownA+bytesDownB, numberPktsA+numberPktsB, flowSetA++flowSetB, connectionsA+connectionsB,(sampleRateA+sampleRateB)/2)
                })
    .filter({ case  (myIP,(bytesUp,bytesDown,numberPkts,flowSet,connections,sampleRate)) => 
                    numberPkts>400
           })
    .sortBy({ 
              case   (myIP,(bytesUp,bytesDown,numberPkts,flowSet,connections,sampleRate)) =>    bytesUp  
            }, false, 15
           )
  .foreach{ case (myIP,(bytesUp,bytesDown,numberPkts,flowSet,connections,sampleRate)) => 
                    println("("+myIP+","+bytesUp+")" ) 
                    val flowMap: Map[String,String] = new HashMap[String,String]
                    flowMap.put("flow:id",System.currentTimeMillis.toString)
                    val event = new HogEvent(new HogFlow(flowMap,myIP,"255.255.255.255"))
                    
                    event.data.put("hostname", myIP)
                    event.data.put("bytesUp", (bytesUp*sampleRate).toString)
                    event.data.put("bytesDown", (bytesDown*sampleRate).toString)
                    event.data.put("numberPkts", numberPkts.toString)
                    event.data.put("connections", connections.toString)
                    event.data.put("stringFlows", setFlows2String(flowSet))
                    
                    if(disable_UDPAmplifier==0)
                    populateUDPAmplifier(event).alert()
           }
 } 
  
  /*
   * 
   *  Abused SMTP Server - tested
   * 
   */
  logger.info("================= Abused SMTP Server = always ="+disable_abusedSMTP)
  System.out.println("======== Abused SMTP Server =====")

  if(disable_abusedSMTP<=1)
 {  
  println("")
  println("Abused SMTP Server")
   val abusedSMTPCollection: PairRDDFunctions[(String, String), (Long,Long,Long,HashSet[(String,String,String,String,String,Long,Long,Long,Int,Long,Long,Long,Int)],Long,Long)] = sflowSummary
    .filter({case ((myIP,myPort,alienIP,alienPort,proto),(bytesUp,bytesDown,numberPkts,direction,beginTime,endTime,sampleRate,status)) 
                  =>  
                      ( myPort.equals("465") |
                        myPort.equals("587")
                      ) &
                      proto.equals("TCP")  &
                      !isMyIP(alienIP,myNets) &
                    !abusedSMTPExcludedIPs.contains(myIP) &
                    !abusedSMTPExcludedIPs.contains(alienIP) 
                      
           })
    .map({
          case ((myIP,myPort,alienIP,alienPort,proto),(bytesUp,bytesDown,numberPkts,direction,beginTime,endTime,sampleRate,status)) =>
               val flowSet:HashSet[(String,String,String,String,String,Long,Long,Long,Int,Long,Long,Long,Int)] = new HashSet()
               flowSet.add((myIP,myPort,alienIP,alienPort,proto,bytesUp,bytesDown,numberPkts,direction,beginTime,endTime,sampleRate,status))
               ((myIP,alienIP),(bytesUp,bytesDown,numberPkts,flowSet,1L,sampleRate))
        })
  
  
  abusedSMTPCollection
    .reduceByKey({
                   case ((bytesUpA,bytesDownA,numberPktsA,flowSetA,connectionsA,sampleRateA),(bytesUpB,bytesDownB,numberPktsB,flowSetB,connectionsB,sampleRateB)) =>
                        (bytesUpA+bytesUpB,bytesDownA+bytesDownB, numberPktsA+numberPktsB, flowSetA++flowSetB, connectionsA+connectionsB,(sampleRateA+sampleRateB)/2)
                })
    .filter({ case  ((myIP,alienIP),(bytesUp,bytesDown,numberPkts,flowSet,connections,sampleRate)) => {
      System.out.println("====Abused SMTP Server"+connections)
      connections > 50 &
        bytesDown * sampleRate > abusedSMTPBytesThreshold
    }
           })
    .sortBy({ 
              case   ((myIP,alienIP),(bytesUp,bytesDown,numberPkts,flowSet,connections,sampleRate)) =>    bytesDown  
            }, false, 15
           )
  .take(100)
  .foreach{ case ((myIP,alienIP),(bytesUp,bytesDown,numberPkts,flowSet,connections,sampleRate)) => 
                    println("("+myIP+","+alienIP+","+bytesUp+")" ) 
                    val flowMap: Map[String,String] = new HashMap[String,String]
                    flowMap.put("flow:id",System.currentTimeMillis.toString)
                    val event = new HogEvent(new HogFlow(flowMap,myIP,alienIP))
                    
                    event.data.put("hostname", myIP)
                    event.data.put("bytesUp",   (bytesUp*sampleRate).toString)
                    event.data.put("bytesDown", (bytesDown*sampleRate).toString)
                    event.data.put("numberPkts", numberPkts.toString)
                    event.data.put("connections", connections.toString)
                    event.data.put("stringFlows", setFlows2String(flowSet))
                    
                     if(disable_abusedSMTP==0) {
                       populateAbusedSMTP(event).alert()
                     }
           }
 }
 
   /*
   * 
   *  DNS tunnels - tested
   * 
   */

  logger.info("================= DNS tunnels="+disable_dnsTunnel)
  System.out.println("======== DNS tunnels =====")

  if(disable_dnsTunnel==0)
  { 
   
  println("")
  println("DNS tunnels")
  val dnsTunnelCollection: PairRDDFunctions[String, (Long,Long,Long,HashSet[(String,String,String,String,String,Long,Long,Long,Int,Long,Long,Long,Int)],Long,Long)] =
  sflowSummary
  .filter({case ((myIP,myPort,alienIP,alienPort,proto),(bytesUp,bytesDown,numberPkts,direction,beginTime,endTime,sampleRate,status)) 
                  =>  
                      alienPort.equals("53") &
                      proto.equals("UDP")  &
                      (bytesUp+bytesDown)*sampleRate > dnsTunnelThreshold &
                      !isMyIP(alienIP,myNets) &
                    !dnsTunnelExcludedIPs.contains(myIP) &
                    !dnsTunnelExcludedIPs.contains(alienIP) 
           })
    .map({
          case ((myIP,myPort,alienIP,alienPort,proto),(bytesUp,bytesDown,numberPkts,direction,beginTime,endTime,sampleRate,status)) =>
               val flowSet:HashSet[(String,String,String,String,String,Long,Long,Long,Int,Long,Long,Long,Int)] = new HashSet()
               flowSet.add((myIP,myPort,alienIP,alienPort,proto,bytesUp,bytesDown,numberPkts,direction,beginTime,endTime,sampleRate,status))
               (myIP,(bytesUp,bytesDown,numberPkts,flowSet,1L,sampleRate))
        })
  
  
  dnsTunnelCollection
    .reduceByKey({
                   case ((bytesUpA,bytesDownA,numberPktsA,flowSetA,connectionsA,sampleRateA),(bytesUpB,bytesDownB,numberPktsB,flowSetB,connectionsB,sampleRateB)) =>
                        (bytesUpA+bytesUpB,bytesDownA+bytesDownB, numberPktsA+numberPktsB, flowSetA++flowSetB, connectionsA+connectionsB,(sampleRateA+sampleRateB)/2)
                })
    .sortBy({ 
              case   (myIP,(bytesUp,bytesDown,numberPkts,flowSet,connections,sampleRate)) =>    bytesUp+bytesDown  
            }, false, 15
           )
  .take(30)
  .foreach{ case (myIP,(bytesUp,bytesDown,numberPkts,flowSet,connections,sampleRate)) => 
                    println("("+myIP+","+bytesUp+")" ) 
                    val flowMap: Map[String,String] = new HashMap[String,String]
                    flowMap.put("flow:id",System.currentTimeMillis.toString)
                    val event = new HogEvent(new HogFlow(flowMap,myIP,"255.255.255.255"))
                    
                    event.data.put("hostname", myIP)
                    event.data.put("bytesUp",   (bytesUp*sampleRate).toString)
                    event.data.put("bytesDown", (bytesDown*sampleRate).toString)
                    event.data.put("numberPkts", numberPkts.toString)
                    event.data.put("connections", connections.toString)
                    event.data.put("stringFlows", setFlows2String(flowSet.filter({p => p._4.equals("53")}))) // 4: alienPort
                    
                    populateDNSTunnel(event).alert()
           }
  }
  
  
  
  
   /*
   * 
   *  ICMP tunnels - tested
   * 
   */

  logger.info("================= ICMP tunnels="+disable_ICMPTunnel)
  System.out.println("======== ICMP tunnels =====")

  if(disable_ICMPTunnel==0)
  { 
   
  println("")
  println("ICMP tunnels")
  val icmpTunnelCollection: PairRDDFunctions[String, (Long,Long,Long,HashSet[(String,String,String,String,String,Long,Long,Long,Int,Long,Long,Long,Int)],Long,Long)] =
  sflowSummaryICMP
  .filter({case ((srcIP,icmpType,dstIP,icmpCode, proto ),(bytesUp,bytesDown,numberOfPkts,direction,beginTime,endTime,sampleRate)) 
                  =>  
                      (bytesUp+bytesDown)/numberOfPkts > icmpTunnelThreshold &
                    !ICMPTunnelExcludedIPs.contains(srcIP) &
                    !ICMPTunnelExcludedIPs.contains(dstIP) 
           })
    .map({
          case ((srcIP,icmpType,dstIP,icmpCode, proto ),(bytesUp,bytesDown,numberOfPkts,direction,beginTime,endTime,sampleRate)) =>
               val flowSet:HashSet[(String,String,String,String,String,Long,Long,Long,Int,Long,Long,Long,Int)] = new HashSet()
               flowSet.add((srcIP,icmpType,dstIP,icmpCode,proto,bytesUp,bytesDown,numberOfPkts,direction,beginTime,endTime,sampleRate,0))
               (srcIP,(bytesUp,bytesDown,numberOfPkts,flowSet,1L,sampleRate))
        })
  
  
  icmpTunnelCollection
    .reduceByKey({
                   case ((bytesUpA,bytesDownA,numberPktsA,flowSetA,connectionsA,sampleRateA),(bytesUpB,bytesDownB,numberPktsB,flowSetB,connectionsB,sampleRateB)) =>
                        (bytesUpA+bytesUpB,bytesDownA+bytesDownB, numberPktsA+numberPktsB, flowSetA++flowSetB, connectionsA+connectionsB,(sampleRateA+sampleRateB)/2)
                })
    .filter({
      case   (srcIP,(bytesUp,bytesDown,numberPkts,flowSet,connections,sampleRate)) =>
         (bytesUp+bytesDown)*sampleRate > icmpTotalTunnelThreshold
    })
    .sortBy({ 
              case   (srcIP,(bytesUp,bytesDown,numberPkts,flowSet,connections,sampleRate)) =>    bytesUp+bytesDown  
            }, false, 15
           )
  .take(30)
  .foreach{ case (srcIP,(bytesUp,bytesDown,numberPkts,flowSet,connections,sampleRate)) => 
                    println("("+srcIP+","+bytesUp+")" )
                    val flowMap: Map[String,String] = new HashMap[String,String]
                    flowMap.put("flow:id",System.currentTimeMillis.toString)
                    val event = new HogEvent(new HogFlow(flowMap,srcIP,"255.255.255.255"))
                    
                    event.data.put("hostname", srcIP)
                    event.data.put("bytesUp",   (bytesUp*sampleRate).toString)
                    event.data.put("bytesDown", (bytesDown*sampleRate).toString)
                    event.data.put("numberPkts", numberPkts.toString)
                    event.data.put("connections", connections.toString)
                    event.data.put("stringFlows", setFlowsICMP2String(flowSet)) 
                    
                    populateICMPTunnel(event).alert()
           }
  }
  
  
 /*
  * 
  * Horizontal PortScans - tested
  * 
  * 
  */
  //val hPortScanMinFlowsThreshold = 300

  logger.info("================= Horizontal portscan = always ="+disable_hPortScan)
  System.out.println("======== Horizontal portscan =====")

  if(disable_hPortScan<=1)
  {   
  println("")
  println("Horizontal portscan")
 
  val hPortScanCollection: PairRDDFunctions[(String,String,String), (Long,Long,Long,HashSet[(String,String,String,String,String,Long,Long,Long,Int,Long,Long,Long,Int)],Long,Long,Long)] = 
    sflowSummary
    .filter({case ((myIP,myPort,alienIP,alienPort,proto),(bytesUp,bytesDown,numberPkts,direction,beginTime,endTime,sampleRate,status)) 
                  => !hPortScanExceptionPorts.contains(alienPort)  & // Avoid common ports
                     (   !isMyIP(alienIP, myNets) ||
                         !hPortScanExceptionInternalPorts.contains(alienPort) ) & // cred cred ca trebuie sa fie myPort
                     numberPkts < 5 & // few pkts per flow
                    !hPortScanExcludedIPs.contains(myIP) &
                    !hPortScanExcludedIPs.contains(alienIP) 
           })
    .map({
      case ((myIP,myPort,alienIP,alienPort,proto),(bytesUp,bytesDown,numberPkts,direction,beginTime,endTime,sampleRate,status)) =>
         val flowSet:HashSet[(String,String,String,String,String,Long,Long,Long,Int,Long,Long,Long,Int)] = new HashSet()
         flowSet.add((myIP,myPort,alienIP,alienPort,proto,bytesUp,bytesDown,numberPkts,direction,beginTime,endTime,sampleRate,status))
        ((myIP,alienIP,alienPort),(bytesUp,bytesDown,numberPkts,flowSet,1L,1L,sampleRate))
        })
  
  val hPortScanCollectionFinal=
  hPortScanCollection
  .reduceByKey({
    case ((bytesUpA,bytesDownA,numberPktsA,flowSetA,numberOfflowsA,numberOffPairsA,sampleRateA),(bytesUpB,bytesDownB,numberPktsB,flowSetB,numberOfflowsB,numberOffPairsB,sampleRateB)) =>
      (bytesUpA+bytesUpB,bytesDownA+bytesDownB, numberPktsA+numberPktsB, flowSetA++flowSetB, numberOfflowsA+numberOfflowsB,1L, (sampleRateA+sampleRateB)/2)
  })
  .map({
     case ((myIP,alienIP,alienPort),(bytesUp,bytesDown,numberPkts,flowSet,numberOfflows,numberOffPairsPort,sampleRate)) =>
        ((myIP,alienPort),(bytesUp,bytesDown,numberPkts,flowSet,numberOfflows,numberOffPairsPort,sampleRate))
  })
  .reduceByKey({
    case ((bytesUpA,bytesDownA,numberPktsA,flowSetA,numberOfflowsA,numberOffPairsA,sampleRateA),(bytesUpB,bytesDownB,numberPktsB,flowSetB,numberOfflowsB,numberOffPairsB,sampleRateB)) =>
      (bytesUpA+bytesUpB,bytesDownA+bytesDownB, numberPktsA+numberPktsB, flowSetA++flowSetB, numberOfflowsA+numberOfflowsB,numberOffPairsA+numberOffPairsB, (sampleRateA+sampleRateB)/2)
  })
  .cache
  
  val hPortScanStats = 
  hPortScanCollectionFinal
  .map({case ((myIP,alienPort),(bytesUp,bytesDown,numberPkts,flowSet,numberOfflows,numberOffPairsPort,sampleRate)) =>
        numberOffPairsPort
      }).stats()
  
  
  hPortScanCollectionFinal
  .filter({case ((myIP,alienPort),(bytesUp,bytesDown,numberPkts,flowSet,numberOfflows,numberOfPairsPort,sampleRate)) =>
          numberOfPairsPort > hPortScanMinFlowsThreshold
    })
  .map({
     case ((myIP,alienPort),(bytesUp,bytesDown,numberPkts,flowSet,numberOfflows,numberOfPairsPort,sampleRate)) =>
    
         val histogram: Map[String,Double] = new HashMap()
         histogram.put(alienPort,numberOfPairsPort)
         
       (myIP,(bytesUp,bytesDown,numberPkts,flowSet,numberOfflows,numberOfPairsPort,histogram,sampleRate))
  })
  .reduceByKey({
    case ((bytesUpA,bytesDownA,numberPktsA,flowSetA,numberOfflowsA,numberOfPairsPortA,histogramA,sampleRateA),(bytesUpB,bytesDownB,numberPktsB,flowSetB,numberOfflowsB,numberOfPairsPortB,histogramB,sampleRateB)) =>
         
      histogramB./:(0){case  (c,(key,qtdH))=> val qtdH2 = {if(histogramA.get(key).isEmpty) 0D else histogramA.get(key).get }
                                                        histogramA.put(key,  qtdH2 + qtdH) 
                                                        0
                                 }
      (bytesUpA+bytesUpB,bytesDownA+bytesDownB, numberPktsA+numberPktsB, flowSetA++flowSetB, numberOfflowsA+numberOfflowsB,numberOfPairsPortA+numberOfPairsPortB, histogramA,(sampleRateA+sampleRateB)/2)
  })
  .filter{case (myIP,(bytesUp,bytesDown,numberPkts,flowSet,numberOfflows,numberOfPairsPort,numPorts,sampleRate)) =>
                   !p2pTalkers.contains(myIP)// Avoid P2P talkers
           }
  .foreach{case  (myIP,(bytesUp,bytesDown,numberPkts,flowSet,numberOfflows,numberOfPairsPort,histogram,sampleRate)) => 
    
                    val savedHistogram=HogHBaseHistogram.getHistogram("HIST07-"+myIP)
                    
                    val hogHistogramOpenPorts=HogHBaseHistogram.getHistogram("HIST01-"+myIP)
                   
                    if(savedHistogram.histSize< 100)
                    {
                      HogHBaseHistogram.saveHistogram(Histograms.mergeMax(savedHistogram, new HogHistogram("",numberOfPairsPort,histogram)))
                    }else
                    {
                          val atypical   = histogram.filter({ case (port,numPairsPort) =>
                            
                                                  if(port.equals("25") && Histograms.isTypicalEvent(hogHistogramOpenPorts.histMap, "25"))
                                                  {
                                                    false // Avoid FP with SMTP servers
                                                  }
                                                    
                                                  if(savedHistogram.histMap.get(port).isEmpty)
                                                  {
                                                    true // This MyIP never accessed so much distinct Aliens in the same port
                                                  }else
                                                  {
                                                    if(savedHistogram.histMap.get(port).get.toLong < numPairsPort)
                                                      true // This MyIP never accessed so much distinct Aliens in the same port
                                                    else
                                                      false // Is typical
                                                  }
                                            })
                            
                           // Histograms.atypical(savedHistogram.histMap, histogram)

                          if(atypical.size>0)
                          {
                            println("MyIP: "+myIP+ "  (N:1,S:"+savedHistogram.histSize+") - Horizontal PortScan: "+numberOfflows+", Ports: "+atypical.toString)
                            
                            val flowMap: Map[String,String] = new HashMap[String,String]
                            flowMap.put("flow:id",System.currentTimeMillis.toString)
                            val event = new HogEvent(new HogFlow(flowMap,myIP,"255.255.255.255"))
                            event.data.put("numberOfFlows",numberOfflows.toString)
                            event.data.put("numberOfFlowsAlienPort",numberOfPairsPort.toString)
                            event.data.put("numberOfFlowsPerPort",atypical.map({case (port,number) => port+"="+number}).mkString("[",", ","]"))
                            event.data.put("myIP", myIP)
                            event.data.put("bytesUp",   (bytesUp*sampleRate).toString)
                            event.data.put("bytesDown", (bytesDown*sampleRate).toString)
                            event.data.put("numberPkts", numberPkts.toString)
                            event.data.put("stringFlows", setFlows2String(flowSet.filter({p => atypical.keySet.contains(p._4)})))
                            event.data.put("flowsMean", hPortScanStats.mean.round.toString)
                            event.data.put("flowsStdev", hPortScanStats.stdev.round.toString)
                            
                            event.data.put("ports",flowSet
                                              .map({case (myIP,myPort,alienIP,alienPort,proto,bytesUp,bytesDown,numberPkts,direction,beginTime,endTime,sampleRate,status) =>
                                                    (proto,alienPort)
                                                   }).toArray
                                                   .distinct
                                                   .map({case (proto,alienPort) => proto+"/"+alienPort})
                                                   .mkString(", ")
                                            )
                                            
                            if(disable_hPortScan==0) {
                              populateHorizontalPortScan(event).alert()
                            }
                          }
                          
                          HogHBaseHistogram.saveHistogram(Histograms.mergeMax(savedHistogram, new HogHistogram("",numberOfflows,histogram)))
                    }
             }
  }
  
  
 /*
  * 
  * Vertical PortScans - tested
  * 
  * 
  */
  //val vPortScanMinPortsThreshold = 3

  logger.info("================= Vertical portscan = always ="+disable_vPortScan)
  System.out.println("======== Vertical portscan=====")

  if(disable_vPortScan<=1)
  {  
  println("")
  println("Vertical portscan")
 
  val vPortScanCollection: PairRDDFunctions[(String,String), (Long,Long,Long,HashSet[(String,String,String,String,String,Long,Long,Long,Int,Long,Long,Long,Int)],Long,Long)] = 
    sflowSummary
    .filter({case ((myIP,myPort,alienIP,alienPort,proto),(bytesUp,bytesDown,numberPkts,direction,beginTime,endTime,sampleRate,status)) 
                  => {
                  val test =
                  alienPort.toLong < vPortScanPortIntervalThreshold  &
                    myPort.toLong > 1023 &
                    numberPkts < 5 & // few pkts per flow
                    !vPortScanExcludedIPs.contains(myIP) &
                    !vPortScanExcludedIPs.contains(alienIP)
                  //println("=============================:"+test+" == "+alienPort.toLong +" --- " +  myPort.toLong +" ++++ " + numberPkts )
                  test
                }
           })
    .map({
      case ((myIP,myPort,alienIP,alienPort,proto),(bytesUp,bytesDown,numberPkts,direction,beginTime,endTime,sampleRate,status)) =>
         val flowSet:HashSet[(String,String,String,String,String,Long,Long,Long,Int,Long,Long,Long,Int)] = new HashSet()
         flowSet.add((myIP,myPort,alienIP,alienPort,proto,bytesUp,bytesDown,numberPkts,direction,beginTime,endTime,sampleRate,status))
        ((myIP,alienIP),(bytesUp,bytesDown,numberPkts,flowSet,1L,sampleRate))
        })
  
  val vPortScanCollectionFinal=
  vPortScanCollection
  .reduceByKey({
    case ((bytesUpA,bytesDownA,numberPktsA,flowSetA,numberOfflowsA,sampleRateA),(bytesUpB,bytesDownB,numberPktsB,flowSetB,numberOfflowsB,sampleRateB)) =>
      (bytesUpA+bytesUpB,bytesDownA+bytesDownB, numberPktsA+numberPktsB, flowSetA++flowSetB, numberOfflowsA+numberOfflowsB, (sampleRateA+sampleRateB)/2)
  })
  .map({case ((myIP,alienIP),(bytesUp,bytesDown,numberPkts,flowSet,numberOfflows,sampleRate)) =>
        
        ((myIP,alienIP),(bytesUp,bytesDown,numberPkts,flowSet,numberOfflows,
            flowSet
            .map({ case (myIP,myPort,alienIP,alienPort,proto,bytesUp,bytesDown,numberPkts,direction,beginTime,endTime,sampleRate,status) => alienPort})
            .toArray
            .distinct
            .size,
          sampleRate))
      })
  .cache
  
  val vPortScanStats = 
  vPortScanCollectionFinal
  .map({case ((myIP,alienIP),(bytesUp,bytesDown,numberPkts,flowSet,numberOfflows,numPorts,sampleRate)) =>
        numPorts
      }).stats()
  
  
  vPortScanCollectionFinal
  .filter({case ((myIP,alienIP),(bytesUp,bytesDown,numberPkts,flowSet,numberOfflows,numPorts,sampleRate)) => {
    //println("=============================:>"+numPorts )
    numPorts > vPortScanMinPortsThreshold
  }
    })
  .filter{case ((myIP,alienIP),(bytesUp,bytesDown,numberPkts,flowSet,numberOfflows,numPorts,sampleRate)) =>
                   !p2pTalkers.contains(myIP)// Avoid P2P talkers
           }
  .foreach{case  ((myIP,alienIP),(bytesUp,bytesDown,numberPkts,flowSet,numberOfflows,numPorts,sampleRate)) => 
    
                    val savedHistogram=HogHBaseHistogram.getHistogram("HIST08-"+myIP)
                    //println("=============================:>"+myIP+ " " + alienIP  )
                    val histogram = new HashMap[String,Double]
                    val key = numPorts.toString
                    histogram.put(key, 1D)
                    
                    if(savedHistogram.histSize< 10)
                    {
                      HogHBaseHistogram.saveHistogram(Histograms.merge(savedHistogram, new HogHistogram("",1L,histogram)))
                    }else
                    {

                          val size = savedHistogram
                            .histMap
                            .filter({case (numberOfPorts,weight) =>
                              numberOfPorts.toLong >= numPorts &
                                weight > Histograms.atypicalThreshold
                            }).size

                          if(size == 0)
                          {
                            println("MyIP: "+myIP+ "  (N:1,S:"+savedHistogram.histSize+") - Vertical PortScan: "+numberOfflows+" numPorts: "+numPorts.toString)
                            
                            val flowMap: Map[String,String] = new HashMap[String,String]
                            flowMap.put("flow:id",System.currentTimeMillis.toString)
                            val event = new HogEvent(new HogFlow(flowMap,myIP,alienIP))
                            event.data.put("numberOfFlows",numberOfflows.toString)
                            event.data.put("numberOfPorts",numPorts.toString)
                            event.data.put("myIP", myIP)
                            event.data.put("alienIP", alienIP)
                            event.data.put("bytesUp",   (bytesUp*sampleRate).toString)
                            event.data.put("bytesDown", (bytesDown*sampleRate).toString)
                            event.data.put("numberPkts", numberPkts.toString)
                            event.data.put("stringFlows", setFlows2String(flowSet))
                            event.data.put("portsMean", vPortScanStats.mean.round.toString)
                            event.data.put("portsStdev", vPortScanStats.stdev.round.toString)
                            
                            if(disable_vPortScan==0) {
                              populateVerticalPortScan(event).alert()
                            }
                          }
                          
                          HogHBaseHistogram.saveHistogram(Histograms.merge(savedHistogram, new HogHistogram("",1L,histogram)))
                    }
             }
  
  }
  
  
  
  
 /*
  * 
  * Server under DDoS attack
  * 
  * 
  */

  logger.info("================= Server under DDoS attack = always ="+disable_DDoS)
  System.out.println("======== Server under DDoS attack=====")

  if(disable_DDoS<=1)
  {   
  println("")
  println("Server under DDoS attack")
 
  val ddosCollection: PairRDDFunctions[(String,String), (Long,Long,Long,HashSet[(String,String,String,String,String,Long,Long,Long,Int,Long,Long,Long,Int)],Long,Long)] = 
    sflowSummary
    .filter({case ((myIP,myPort,alienIP,alienPort,proto),(bytesUp,bytesDown,numberPkts,direction,beginTime,endTime,sampleRate,status)) 
                  => !isMyIP(alienIP, myNets) &
                     !ddosExceptionAlienPorts.contains(alienPort) &
                     direction < 1 &
                    !DDoSExcludedIPs.contains(myIP) &
                    !DDoSExcludedIPs.contains(alienIP)
           })
    .map({
      case ((myIP,myPort,alienIP,alienPort,proto),(bytesUp,bytesDown,numberPkts,direction,beginTime,endTime,sampleRate,status)) =>
         val flowSet:HashSet[(String,String,String,String,String,Long,Long,Long,Int,Long,Long,Long,Int)] = new HashSet()
         flowSet.add((myIP,myPort,alienIP,alienPort,proto,bytesUp,bytesDown,numberPkts,direction,beginTime,endTime,sampleRate,status))
        ((myIP,alienIP),(bytesUp,bytesDown,numberPkts,flowSet,1L,sampleRate))
        })
  
  val ddosCollectionFinal=
  ddosCollection
  .reduceByKey({
    case ((bytesUpA,bytesDownA,numberPktsA,flowSetA,numberOfflowsA,sampleRateA),(bytesUpB,bytesDownB,numberPktsB,flowSetB,numberOfflowsB,sampleRateB)) =>
      (bytesUpA+bytesUpB,bytesDownA+bytesDownB, numberPktsA+numberPktsB, flowSetA++flowSetB, numberOfflowsA+numberOfflowsB, (sampleRateA+sampleRateB)/2)
  })
  .cache
  
  val ddosStats = 
  ddosCollectionFinal
  .map({case ((myIP,alienIP),(bytesUp,bytesDown,numberPkts,flowSet,numberOfflows,sampleRate)) =>
        numberOfflows
      }).stats()
  
  
  ddosCollectionFinal
  .filter({case ((myIP,alienIP),(bytesUp,bytesDown,numberPkts,flowSet,numberOfflows,sampleRate)) =>
        numberOfflows > ddosMinConnectionsThreshold &
        !p2pTalkers.contains(myIP) & // Avoid P2P talkers
        {
          val orderedFlowSet=
          flowSet
          .map({case (myIP,myPort,alienIP,alienPort,proto,bytesUp,bytesDown,numberPkts,direction,beginTime,endTime,sampleRate,status) => beginTime})
          .toArray
          .sortBy { x => x }
          
          val orderedFlowSetSize = orderedFlowSet.size
              
          //TODO: Review. You should count the number of equals beginTime and discover why its generating error.
          if(orderedFlowSetSize>6)
          {  
              val flowSetMean=
              (orderedFlowSet.slice(1, orderedFlowSetSize)
              .zip(orderedFlowSet.slice(0, orderedFlowSetSize-1))
              .map({case (a,b) => a-b})
              .toArray
              .sortBy { x => x }
              .slice(0,orderedFlowSetSize-4)
              .sum)/(orderedFlowSetSize-4)
              
               if(flowSetMean<60)
                true
               else
                 false
          }else
          {false}
        } &bytesDown>bytesUp
        
    })
  .map({case ((myIP,alienIP),(bytesUp,bytesDown,numberPkts,flowSet,numberOfflows,sampleRate)) =>
           (myIP,(bytesUp,bytesDown,numberPkts,flowSet,numberOfflows,1L,sampleRate))
  })
  .reduceByKey({
    case ((bytesUpA,bytesDownA,numberPktsA,flowSetA,numberOfflowsA,pairsA,sampleRateA),(bytesUpB,bytesDownB,numberPktsB,flowSetB,numberOfflowsB,pairsB,sampleRateB)) =>
      (bytesUpA+bytesUpB,bytesDownA+bytesDownB, numberPktsA+numberPktsB, flowSetA++flowSetB, numberOfflowsA+numberOfflowsB,pairsA+pairsB,(sampleRateA+sampleRateB)/2)
  })
  .filter{case  (myIP,(bytesUp,bytesDown,numberPkts,flowSet,numberOfflows,pairs,sampleRate)) => 
                pairs > ddosMinPairsThreshold 
         }
  .foreach{case  (myIP,(bytesUp,bytesDown,numberPkts,flowSet,numberOfflows,pairs,sampleRate)) => 
    
              
                            println("MyIP: "+myIP+ " - DDoS Attack: "+numberOfflows+" Pairs: "+pairs.toString)
                            
                            val flowMap: Map[String,String] = new HashMap[String,String]
                            flowMap.put("flow:id",System.currentTimeMillis.toString)
                            val event = new HogEvent(new HogFlow(flowMap,myIP,"255.255.255.255"))
                            event.data.put("numberOfFlows",numberOfflows.toString)
                            event.data.put("numberOfAttackers",pairs.toString)
                            event.data.put("myIP", myIP)
                            event.data.put("bytesUp",   (bytesUp*sampleRate).toString)
                            event.data.put("bytesDown", (bytesDown*sampleRate).toString)
                            event.data.put("numberPkts", numberPkts.toString)
                            event.data.put("stringFlows", setFlows2String(flowSet))
                            event.data.put("flowsMean", ddosStats.mean.round.toString)
                            event.data.put("flowsStdev", ddosStats.stdev.round.toString)
                        
                           if(disable_DDoS==0)
                           populateDDoSAttack(event).alert()
                  
          }
  
  }
 
   
   /*
   * 
   *  C&C BotNets - tested
   *
   */


  logger.info("================= C&C BotNets = always ="+disable_BotNet)
  System.out.println("======== C&C BotNets=====")

  if(disable_BotNet==0) // <=1
  {

    logger.info("================= C&C BotNets : start 1")

  object ccBotNets  {
    val _set = new scala.collection.mutable.TreeSet[String]
    HogHBaseReputation.getReputationList("CCBotNet", "blacklist")
    .foreach({botnetIP => _set.add(botnetIP)})
    
      def findMatches(prefix: String): Iterable[String] =
          _set from prefix takeWhile(_ startsWith prefix)
    
      def contains(prefix:String):Boolean =
           this.findMatches(prefix).take(1).size>0
  
  }
   
   
  println("")
  println("C&C BotNets")
    logger.info("================= C&C BotNets : start 2")

  val size= sflowSummary.count()
    logger.info("================= Filtered sflowSummary has "+size+" rows!")

  val ccBotNetsCollection: PairRDDFunctions[String, (Set[String],Long,Long,Long,HashSet[(String,String,String,String,String,Long,Long,Long,Int,Long,Long,Long,Int)],Long,Long)] =
  sflowSummary
  .filter({case ((myIP,myPort,alienIP,alienPort,proto),(bytesUp,bytesDown,numberPkts,direction,beginTime,endTime,sampleRate,status)) 
                  => {
    logger.info("================= myIP : "+myIP)
    logger.info("================= alienIP : "+alienIP)

    logger.info("================= myPort : "+myPort)
    logger.info("================= numberPkts : "+numberPkts)
    logger.info("================= ccBotNets : "+ccBotNets)
    logger.info("================= CCminPktsPerFlow : "+CCminPktsPerFlow)

    logger.info("================= ccBotNets.contains(alienIP) : "+ccBotNets.contains(alienIP))

    val result = myPort.toLong > 1023 &
      numberPkts.toLong >= CCminPktsPerFlow &
      (ccBotNets != null && ccBotNets.contains(alienIP)) &
      !BotNetExcludedIPs.contains(myIP) &
      !BotNetExcludedIPs.contains(alienIP)

    logger.info("================= result : "+result)

    result
  }
           })
    .map({
          case ((myIP,myPort,alienIP,alienPort,proto),(bytesUp,bytesDown,numberPkts,direction,beginTime,endTime,sampleRate,status)) =>
               val flowSet:HashSet[(String,String,String,String,String,Long,Long,Long,Int,Long,Long,Long,Int)] = new HashSet()
               flowSet.add((myIP,myPort,alienIP,alienPort,proto,bytesUp,bytesDown,numberPkts,direction,beginTime,endTime,sampleRate,status))
               (myIP,(Set(alienIP),bytesUp,bytesDown,numberPkts,flowSet,1L,sampleRate))
        })

    logger.info("================= C&C BotNets : start 2" + ccBotNetsCollection)

  ccBotNetsCollection
    .reduceByKey({
                   case ((aliensA,bytesUpA,bytesDownA,numberPktsA,flowSetA,connectionsA,sampleRateA),(aliensB,bytesUpB,bytesDownB,numberPktsB,flowSetB,connectionsB,sampleRateB)) =>
                        (aliensA++aliensB,bytesUpA+bytesUpB,bytesDownA+bytesDownB, numberPktsA+numberPktsB, flowSetA++flowSetB, connectionsA+connectionsB,(sampleRateA+sampleRateB)/2)
                })
  .foreach{ case (myIP,(aliens,bytesUp,bytesDown,numberPkts,flowSet,connections,sampleRate)) =>
                    val flowMap: Map[String,String] = new HashMap[String,String]
                    flowMap.put("flow:id",System.currentTimeMillis.toString)
                    val event = new HogEvent(new HogFlow(flowMap,myIP,"255.255.255.255"))
                    
                    event.data.put("hostname", myIP)
                    event.data.put("bytesUp",   (bytesUp*sampleRate).toString)
                    event.data.put("bytesDown", (bytesDown*sampleRate).toString)
                    event.data.put("numberPkts", numberPkts.toString)
                    event.data.put("connections", connections.toString)
                    event.data.put("aliens", aliens.mkString(","))
                    event.data.put("stringFlows", setFlows2String(flowSet))

                    logger.info("================= ALERT BotNet !")

                    if(disable_BotNet==0) {
                      populateCCBotNet(event).alert()
                    }
           }
 
  }

    logger.info("================= Find Operating Systems")

 /*
  * 
  * Find Operating Systems
  * 
  */
   
  println("")
  println("Find Operating Systems")
  
  val osRepos:scala.collection.immutable.Map[String,String] = 
       HogHBaseReputation.getReputationList("OSRepo", "windows" )
                         .map({case ip => (ip -> "Windows")}).toMap++
       HogHBaseReputation.getReputationList("OSRepo", "linux")
                         .map({case ip => (ip -> "Linux")}).toMap++
       HogHBaseReputation.getReputationList("OSRepo", "android")
                         .map({case ip => (ip -> "Android")}).toMap++
       HogHBaseReputation.getReputationList("OSRepo", "apple")
                         .map({case ip => (ip -> "Apple")}).toMap++
       HogHBaseReputation.getReputationList("OSRepo", "freebsd")
                         .map({case ip => (ip -> "FreeBSD")}).toMap
                         
  val osKeySet:scala.collection.immutable.Set[String] = osRepos.keySet
  
  
  val osCollection: PairRDDFunctions[String, Set[String]] = 
    sflowSummary
    .filter({case ((myIP,myPort,alienIP,alienPort,proto),(bytesUp,bytesDown,numberPkts,direction,beginTime,endTime,sampleRate,status)) 
                  => osKeySet.contains(alienIP)
           })
    .map({
      case ((myIP,myPort,alienIP,alienPort,proto),(bytesUp,bytesDown,numberPkts,direction,beginTime,endTime,sampleRate,status)) =>
            (myIP,Set(osRepos.get(alienIP).get))
        })
  
  val osCollectionFinal=
  osCollection
  .reduceByKey({
    case (osA,osB) =>
      (osA++osB)
  })
  .cache
  
  osCollectionFinal
  .foreach{case  (myIP,opSystems) => 
              opSystems.toArray.distinct
              .foreach { opSys =>
                             HogHBaseInventory.saveInventory(myIP, opSys)
                        }                
           }
  
  
  // END
  }
}
