package ro.simavi.sphinx.ad.kernel.util;

import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.mllib.linalg.Vector;
import org.apache.spark.mllib.linalg.Vectors;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class AdmlStatistics implements Serializable {

    public List<Double> getSums(JavaRDD<ArrayList<Double>> matrix){
        // Scala: //  val sums = HttpRDDcount.reduce((a,b) => a.zip(b).map(t => t._1 + t._2))

        // my translate
        ArrayList<Double> sums = matrix.reduce((a,b)->{
            Stream<List<Double>> zip = IntStream
                    .range(0, Math.min(a.size(), b.size()))
                    .mapToObj(i -> Arrays.asList(a.get(i),b.get(i)));

            List<Double> list = zip.map(t->{
                Double value = t.get(0) + t.get(1);
//                if (value>0){
//                    System.out.println(value);
//                }
                return value;
            }).collect(Collectors.toList());

            return new ArrayList<>(list);
        });

        return sums;
    }

    public List<Double> getSquares(JavaRDD<ArrayList<Double>> matrix) {

//        val sumSquares = HttpRDDcount.fold(
//                new Array[Double](numCols)
//        )(
//                (a,b) => a.zip(b).map(t => t._1 + t._2*t._2)
//        )

        JavaRDD<ArrayList<Double>> squares = matrix.map( t ->{
            ArrayList<Double> list =  new ArrayList<>();
            for(Double value: t){
                list.add(value*value);
            }
            return list;
        });

        return getSums(squares);

    }

    public List<Double> getStdev(JavaRDD<ArrayList<Double>> matrix, List<Double> sums, List<Double> sumSquares, long n){

        // scala
        // val stdevs = sumSquares.zip(sums).map{
        //      case(sumSq,sum) => math.sqrt(n*sumSq - sum*sum)/n
        // }

        List<Double> stdevs = new ArrayList<>();
        long numCols = matrix.first().size();
        for(int i=0;i<numCols;i++){
            double stdev = Math.sqrt(n*sumSquares.get(i)-sums.get(i)*sums.get(i))/n;
            stdevs.add(stdev);
        }

        return stdevs;
    }

    private List<List<Double>> zip(List<Double> list1, List<Double> list2){
        List<List<Double>> list = new ArrayList<>();
        for(int i=0;i<list1.size();i++){
            List<Double> l = new ArrayList<>();
            l.add(list1.get(i));
            l.add(list2.get(i));
            list.add(l);
        }
        return list;
    }

    private ArrayList<Double> sum(List<Double> list1, List<Double> list2){
        ArrayList<Double> list = new ArrayList<>();
        for(int i=0;i<list1.size();i++){
            list.add(list1.get(i)+list2.get(i));
        }
        return list;
    }

    private ArrayList<Double> sumSquares(List<Double> list1, List<Double> list2){
        ArrayList<Double> list = new ArrayList<>();
        for(int i=0;i<list1.size();i++){
            list.add(list1.get(i)*list1.get(i)+list2.get(i)*list2.get(i));
        }
        return list;
    }

    public Vector normalize(double[] values, List<Double> sums, List<Double> stdevs, long n){
        // se caluleaza mediile pe fiecare coloana
        // standard score sau z-score
        //    = (x-m)/std, std>0
        //    = (x-m), std<=0
        // std = standard deviation
        // m = means

        /*
        def normalize(vector: Vector):Vector = {
                val normArray = (vector.toArray,means,stdevs).zipped.map(
                (value,mean,std) =>
        if(std<=0) (value-mean) else (value-mean)/std)
        return Vectors.dense(normArray)
        */

        ArrayList<Double> normArray = new ArrayList<>();
        for(int i = 0; i<sums.size(); i++){
            double std = stdevs.get(i);
            double mean = sums.get(i)/n;
            double value = values[i];
            if (std<=0){
                normArray.add(value-mean);
            }else{
                normArray.add((value-mean)/std);
            }
        }

        double [] normList =normArray.stream()
                .mapToDouble(Double::doubleValue)
                .toArray();
        return Vectors.dense(normList);

    }
}
