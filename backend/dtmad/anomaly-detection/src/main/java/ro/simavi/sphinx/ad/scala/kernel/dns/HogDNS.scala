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
/**
 *  REFERENCES:
 *   - http://www.zytrax.com/books/dns/ch15/
 *   - http://ids-hogzilla.org/xxx/826000001
 */


package ro.simavi.sphinx.ad.scala.kernel.dns

import org.apache.hadoop.hbase.util.Bytes
import org.apache.spark._
import org.apache.spark.mllib.clustering.KMeans
import org.apache.spark.mllib.linalg.{Vector, Vectors}
import org.apache.spark.rdd.RDD
import ro.simavi.sphinx.ad.scala.kernel.event.{HogEvent, HogSignature}
import ro.simavi.sphinx.ad.scala.kernel.hbase.HogHBaseRDD
import ro.simavi.sphinx.ad.scala.kernel.util.HogFlow

import scala.collection.mutable.{HashMap, Map}

/**
 * 
 */
object HogDNS {

//  val signature = (HogSignature(3,"AD: Suspicious DNS flow identified by K-Means clustering",2,1,826000001,826).saveHBase(),
//                   HogSignature(3,"AD: Suspicious DNS flow identified by SuperBag",2,1,826000002,826).saveHBase())
                   
  val numberOfClusters=9
  val maxAnomalousClusterProportion=0.05
  val minDirtyProportion=0.001
  
  /**
   * 
   * 
   * 
   */
  def run(HogRDD: RDD[(org.apache.hadoop.hbase.io.ImmutableBytesWritable,org.apache.hadoop.hbase.client.Result)],spark:SparkContext)
  {
    
    // DNS K-means clustering on flow bytes
    //kmeansBytes(HogRDD)
    
    // DNS Super Bag
    //superbag(HogRDD,spark)
    
    // DNS K-means clustering
   kmeans(HogRDD)
 
  }
  
  
  /**
   * 
   * 
   * 
   */
  def kmeansPopulate(event:HogEvent):HogEvent =
  {
    val centroids:String = event.data.get("centroids")
    val vector:String = event.data.get("vector")
    val clusterLabel:String = event.data.get("clusterLabel")
    val hostname:String = event.data.get("hostname")
    
    val signature_id = 826000001;

    event.text = "This flow was detected by Hogzilla as an abnormal activity. In what follows you can see more information.\n"+
                 "Hostname mentioned in DNS flow: "+hostname+"\n"+
                 "Hogzilla module: HogDNS, Method: k-means clustering with k="+numberOfClusters+"\n"+
                 "URL for more information: http://ids-hogzilla.org/signature-db/"+"%.0f".format(signature_id)+"\n"+""
                // "Centroids:\n"+centroids+"\n"+
                // "Vector: "+vector+"\n"+
                // "(cluster,label nDPI): "+clusterLabel+"\n"
    
    event.signature_id = signature_id
                 
    event
  }
  
  
  /**
   * 
   * 
   * 
   */
  def kmeans(HogRDD: RDD[(org.apache.hadoop.hbase.io.ImmutableBytesWritable,org.apache.hadoop.hbase.client.Result)])
  {
    
	  val features = Array("flow:avg_packet_size",
			                   "flow:packets_without_payload",
                         "flow:avg_inter_time",
			                   "flow:flow_duration",
			                   "flow:max_packet_size",
			                   "flow:bytes",
			                   "flow:packets",
			                   "flow:min_packet_size",
			                   "flow:packet_size-0",
			                   "flow:inter_time-0",
                         "flow:packet_size-1",
		                     "flow:dns_num_queries",
			                   "flow:dns_num_answers",
			                   "flow:dns_ret_code",
			                   "flow:dns_bad_packet",
		                 	   "flow:dns_query_type",
		                 	   "flow:dns_rsp_type")
 
    println("Filtering HogRDD...")
    val DnsRDD = HogRDD.
        map { case (id,result) => {
          val map: Map[String,String] = new HashMap[String,String]
              map.put("flow:id",Bytes.toString(id.get).toString())
                HogHBaseRDD.columns.foreach { column =>
                
                val ret = result.getValue(Bytes.toBytes(column.split(":")(0).toString()),Bytes.toBytes(column.split(":")(1).toString()))
                map.put(column, Bytes.toString(ret)) 
        }
          if(map.get("flow:dns_num_queries")==null) map.put("flow:dns_num_queries","0")
          if(map.get("flow:dns_num_answers")==null) map.put("flow:dns_num_answers","0")
          if(map.get("flow:dns_ret_code")==null) map.put("flow:dns_ret_code","0")
          if(map.get("flow:dns_bad_packet")==null) map.put("flow:dns_bad_packet","0")
          if(map.get("flow:dns_query_type")==null) map.put("flow:dns_query_type","0")
          if(map.get("flow:dns_rsp_type")==null) map.put("flow:dns_rsp_type","0")
          if(map.get("flow:packet_size-1")==null) map.put("flow:packet_size-1","0")
          
          val lower_ip = result.getValue(Bytes.toBytes("flow"),Bytes.toBytes("lower_ip"))
          val upper_ip = result.getValue(Bytes.toBytes("flow"),Bytes.toBytes("upper_ip"))
          new HogFlow(map,Bytes.toString(lower_ip),Bytes.toString(upper_ip))
        }
    }.filter(x =>  ( x.get("flow:lower_port").equals("53") ||
                     x.get("flow:upper_port").equals("53")
                   ) && x.get("flow:packets").toDouble.>(1)
                     && x.get("flow:id").split('.')(0).toLong.<(System.currentTimeMillis()-6000000)
             ).cache

  println("Counting HogRDD...")
  val RDDtotalSize= DnsRDD.count()
  println("Filtered HogRDD has "+RDDtotalSize+" rows!")
  
  if(RDDtotalSize == 0)
    return
  
  println("Calculating some variables to normalize data...")
  val DnsRDDcount = DnsRDD.map(flow => features.map { feature => flow.get(feature).toDouble }).cache()
  val n = RDDtotalSize
  val numCols = DnsRDDcount.first.length
  val sums = DnsRDDcount.reduce((a,b) => a.zip(b).map(t => t._1 + t._2))
  val sumSquares = DnsRDDcount.fold(
      new Array[Double](numCols)
  )(
      (a,b) => a.zip(b).map(t => t._1 + t._2*t._2)
   )
      
  val stdevs = sumSquares.zip(sums).map{
      case(sumSq,sum) => math.sqrt(n*sumSq - sum*sum)/n
    }
    
  val means = sums.map(_/n)
      
  def normalize(vector: Vector):Vector = {
    val normArray = (vector.toArray,means,stdevs).zipped.map(
        (value,mean,std) =>
          if(std<=0) (value-mean) else (value-mean)/std)
    return Vectors.dense(normArray)
  }
    
  println("Normalizing data...")
    val labelAndData = DnsRDD.map { flow => 
     val vector = Vectors.dense(features.map { feature => flow.get(feature).toDouble })
       // ( (DNS,1,lele.com,flow) , vector )
       ((flow.get("flow:detected_protocol"), 
          if (flow.get("event:priority_id")!=null && flow.get("event:priority_id").equals("1")) 1 else 0 , 
          flow.get("flow:host_server_name"),flow),normalize(vector)
       )
    }
  
    println("Estimating model...")
    val data = labelAndData.values.cache()
    val kmeans = new KMeans()
    kmeans.setK(numberOfClusters)
    val vectorCount = data.count()
    println("Number of vectors: "+vectorCount)
    val model = kmeans.run(data)
    
    println("Predicting points (ie, find cluster for each point)...")
     val clusterLabel = labelAndData.map({
      case (label,datum) =>
        val cluster = model.predict(datum)
        (cluster,label,datum)
    })
    
    println("Generating histogram...")
    val clusterLabelCount = clusterLabel.map({
      case (cluster,label,datum) =>
        val map: Map[(Int,String),(Double,Int)] = new HashMap[(Int,String),(Double,Int)]
        map.put((cluster,label._1),  (label._2.toDouble,1))
        map
    }).reduce((a,b) => { 
      
        b./:(0){
         case (c,((key:(Int,String)),(avg2,count2))) =>
           
                val avg = (a.get(key).get._1*a.get(key).get._2 + b.get(key).get._1*b.get(key).get._2)/
                          (a.get(key).get._2+b.get(key).get._2)
                          
                a.put(key, (avg,a.get(key).get._2+b.get(key).get._2))
           
               0
              }

      a
    })
    
    println("######################################################################################")
    println("######################################################################################")
    println("######################################################################################")
    println("######################################################################################")
    println("DNS K-Means Clustering")
    println("Centroids")
    val centroids = ""+model.clusterCenters.mkString(",\n")
    //model.clusterCenters.foreach { center => centroids.concat("\n"+center.toString) }
    
    clusterLabelCount./:(0) 
     { case (z,(key:(Int,String),(avg,count))) =>    
      val cluster = key._1
      val label = key._2

      println(f"Cluster: $cluster%1s\t\tLabel: $label%20s\t\tCount: $count%10s\t\tAvg: $avg%10s")
      0
      }

      val thr=maxAnomalousClusterProportion*RDDtotalSize
      
      
      println("Selecting cluster to be tainted...")
       val taintedArray = clusterLabelCount.filter({ case (key:(Int,String),(avg,count)) => 
                                                          (count.toDouble < thr 
                                                              && avg.toDouble >= minDirtyProportion )
                          }).map(_._1)
                //.
                //  sortBy ({ case (cluster:Int,label:String) => clusterLabelCount.get((cluster,label))._1.toDouble }).reverse
      
      taintedArray.par.map 
      {
        tainted =>
        
        //val tainted = taintedArray.apply(0)
                  
        println("######################################################################################")
        println("Tainted flows of: "+tainted.toString())
      
        println("Generating events into HBase...")
        clusterLabel.filter({ case (cluster,(group,tagged,hostname,flow),datum) => (cluster,group).equals(tainted) && tagged.equals(0) }).
        foreach{ case (cluster,(group,tagged,hostname,flow),datum) => 
          val event = new HogEvent(flow)
          event.data.put("centroids", centroids)
          event.data.put("vector", datum.toString)
          event.data.put("clusterLabel", "("+cluster.toString()+","+group+")")
          event.data.put("hostname", flow.get("flow:host_server_name"))
          kmeansPopulate(event).alert()
        }

      println("######################################################################################")
      println("######################################################################################")
      println("######################################################################################")
      println("######################################################################################")           

   }
      
   if(taintedArray.isEmpty)
   {
        println("No flow matched!")
   }

  }

}