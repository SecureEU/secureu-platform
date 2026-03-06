/*
* Copyright (C) 2015-2015 Paulo Angelo Alves Resende <pa@pauloangelo.com>
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

package ro.simavi.sphinx.ad.scala.kernel.hbase

/**
 * @author pa
 */

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.hbase.client._
import org.apache.hadoop.hbase.mapreduce.TableInputFormat
import org.apache.hadoop.hbase.{HBaseConfiguration, TableName}
import org.apache.spark._
import org.apache.spark.rdd.RDD
import ro.simavi.sphinx.ad.scala.kernel.event.HogSignature

object HogHBaseRDD {

  val columns = List("flow:lower_ip","flow:lower_ip","flow:lower_ip","flow:upper_ip","flow:lower_name","flow:upper_name","flow:lower_port","flow:upper_port","flow:protocol","flow:vlan_id","flow:last_seen","flow:bytes","flow:packets","flow:flow_duration","flow:first_seen","flow:max_packet_size","flow:min_packet_size","flow:avg_packet_size","flow:payload_bytes","flow:payload_first_size","flow:payload_avg_size","flow:payload_min_size","flow:payload_max_size","flow:packets_without_payload","flow:detection_completed","flow:detected_protocol","flow:host_server_name","flow:inter_time_stddev","flow:packet_size_stddev","flow:avg_inter_time",
                     "flow:packet_size-0","flow:inter_time-0","flow:packet_size-1","flow:inter_time-1","flow:packet_size-2","flow:inter_time-2","flow:packet_size-3","flow:inter_time-3","flow:packet_size-4","flow:inter_time-4",
                     "flow:detected_os",
                     "flow:dns_num_queries","flow:dns_num_answers","flow:dns_ret_code","flow:dns_bad_packet","flow:dns_query_type","flow:dns_query_class","flow:dns_rsp_type",
                     "event:sensor_id","event:event_id","event:event_second","event:event_microsecond","event:signature_id","event:generator_id","event:classification_id","event:priority_id",
                     "flow:http_method","flow:http_url","flow:http_content_type") 
 
//  val columnsSFlow = List("flow:IPprotocol","flow:IPsize","flow:agentID","flow:dstIP","flow:dstMAC","flow:dstPort","flow:ethernetType","flow:inVlan","flow:inputPort","flow:ipTos",
//                          "flow:ipTtl","flow:outVlan","flow:outputPort","flow:packetSize","flow:samplingRate","flow:srcIP","flow:srcMAC","flow:srcPort","flow:tcpFlags",
//                          "flow:timestamp")
                     
  // "flow:inter_time-%d","flow:packet_size-%d"

  var hbaseZookeeperQuorum: String = null

  def setHbaseZookeeperQuorum(hbaseZookeeperQuorum_1:String)={
    hbaseZookeeperQuorum = hbaseZookeeperQuorum_1;
  }

  var hbaseZookeeperClientPort: Int = 2181

  def setHbaseZookeeperClientPort(hbaseZookeeperClientPort_1:Int)={
    hbaseZookeeperClientPort = hbaseZookeeperClientPort_1;
  }

  var admin: Admin = null

  var hogzilla_flows: Table = null
  var hogzilla_sflows: Table = null
  var hogzilla_events: Table = null
  var hogzilla_sensor: Table = null
  var hogzilla_signatures : Table = null
  var hogzilla_mynets: Table = null
  var hogzilla_reputation: Table = null
  var hogzilla_histograms: Table = null
  var hogzilla_clusters: Table = null
  var hogzilla_cluster_members: Table = null
  var hogzilla_inventory: Table = null
  var hogzilla_authrecords: Table = null

  def getHogzillaSensor(): Table ={
    return hogzilla_sensor;
  }

  def getConf(table: String, timeout: Int): Configuration = {
    val conf = HBaseConfiguration.create();

    conf.set(TableInputFormat.INPUT_TABLE, table)
    conf.set("zookeeper.session.timeout", timeout + "")
    conf.setInt("hbase.client.scanner.timeout.period", timeout)
    // You can limit the SCANNED COLUMNS here
    conf.set("hbase.rpc.timeout", "1800000")
    if (hbaseZookeeperClientPort!=null) {
      conf.setInt("hbase.zookeeper.property.clientPort", hbaseZookeeperClientPort)
    }else{
      conf.setInt("hbase.zookeeper.property.clientPort", 2181)
    }

    if (hbaseZookeeperQuorum!=null){
      conf.set("hbase.zookeeper.quorum", hbaseZookeeperQuorum)
    }

    return conf;
  }

  def getConnection( conf: Configuration): Connection ={

    val connection = ConnectionFactory.createConnection(conf)

    hogzilla_flows            = connection.getTable(TableName.valueOf("adml_flows"))
    hogzilla_sflows           = connection.getTable(TableName.valueOf("adml_sflows"))
    hogzilla_events           = connection.getTable(TableName.valueOf("adml_events"))
    hogzilla_sensor           = connection.getTable(TableName.valueOf("adml_sensor"))
    hogzilla_signatures       = connection.getTable(TableName.valueOf("adml_signatures"))
    hogzilla_mynets           = connection.getTable(TableName.valueOf("adml_mynets"))
    hogzilla_reputation       = connection.getTable(TableName.valueOf("adml_reputation"))
    hogzilla_histograms       = connection.getTable(TableName.valueOf("adml_histograms"))
    hogzilla_clusters         = connection.getTable(TableName.valueOf("adml_clusters"))
    hogzilla_cluster_members  = connection.getTable(TableName.valueOf("adml_cluster_members"))
    hogzilla_inventory        = connection.getTable(TableName.valueOf("adml_inventory"))
    hogzilla_authrecords      = connection.getTable(TableName.valueOf("adml_authrecords"))

    return connection;
  }

  def saveSignature(): Unit ={

    // HogSFlowHistograms
    var signature_HogSFlowHistograms = HogSignature(3,"AD: Top talker identified" ,                2,1,826001101,826).saveHBase() //1

    // DNS
    var signature_DNS = (HogSignature(3,"AD: Suspicious DNS flow identified by K-Means clustering",2,1,826000001,826).saveHBase(),
      HogSignature(3,"AD: Suspicious DNS flow identified by SuperBag",2,1,826000002,826).saveHBase())

    // HTTP
    var signature_HTTP = (HogSignature(3,"AD: Suspicious HTTP flow identified by K-Means clustering",2,1,826000101,826).saveHBase(),
     HogSignature(3,"AD: Suspicious HTTP flow identified by SuperBag",2,1,826000102,826).saveHBase())

    // HogAuth
    val signature_HogAuth = (HogSignature(3,"AD/Auth: Atypical access location" ,                2,1,826001201,826).saveHBase(), //1
                     HogSignature(3,"AD/Auth: Atypical access user-agent" ,              2,1,826001202,826).saveHBase(), //2
                     HogSignature(3,"AD/Auth: Atypical access service or system" ,       2,1,826001203,826).saveHBase(), //3
                     HogSignature(3,"AD/Auth: Atypical user access" ,                    2,1,826001204,826).saveHBase()) //4
  }

  def genericConnect(spark: SparkContext, table: String, timeout: Int):RDD[(org.apache.hadoop.hbase.io.ImmutableBytesWritable,org.apache.hadoop.hbase.client.Result)]=
  {
    val conf = getConf(table, timeout)
    val connection = getConnection(conf);

    val admin = connection.getAdmin()
    if (!admin.isTableAvailable(TableName.valueOf(table))) {
      println("Table "+table+" does not exist.")
    }

    val hBaseRDD = spark.newAPIHadoopRDD(conf, classOf[TableInputFormat],
      classOf[org.apache.hadoop.hbase.io.ImmutableBytesWritable],
      classOf[org.apache.hadoop.hbase.client.Result])

    return hBaseRDD
  }

  def connect(spark: SparkContext):RDD[(org.apache.hadoop.hbase.io.ImmutableBytesWritable,org.apache.hadoop.hbase.client.Result)]=
  {
    return genericConnect(spark, "adml_flows", 1800000)
  }

  def connectSFlow(spark: SparkContext):RDD[(org.apache.hadoop.hbase.io.ImmutableBytesWritable,org.apache.hadoop.hbase.client.Result)]=
  {
    return genericConnect(spark, "adml_sflows", 600000)
  }

  def connectHistograms(spark: SparkContext):RDD[(org.apache.hadoop.hbase.io.ImmutableBytesWritable,org.apache.hadoop.hbase.client.Result)]=
  {
    return genericConnect(spark, "adml_histograms", 600000)
  }

  def connectAuth(spark: SparkContext):RDD[(org.apache.hadoop.hbase.io.ImmutableBytesWritable,org.apache.hadoop.hbase.client.Result)]=
  {
    return genericConnect(spark, "adml_authrecords", 600000)
  }
  
  def close()
  {
     if (admin!=null){
       admin.close()
     }

  }
    
}