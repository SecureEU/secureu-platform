package ro.simavi.sphinx.ad.kernel.test;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class CountNumber implements Serializable {

    /*
    cd c:\tools\spark\bin
    spark-submit --class ro.simavi.sphinx.ad.kernel.test.CountNumber --master spark://172.18.252.171:7077 C:\work/SPHINX/anomaly-detection/target/anomaly-detection-0.0.1-SNAPSHOT.jar
    spark-submit --class ro.simavi.sphinx.ad.kernel.test.CountNumber --jars "C:\work/SPHINX/anomaly-detection/target/anomaly-detection-0.0.1-SNAPSHOT.jar" "C:\work/SPHINX/anomaly-detection/target/anomaly-detection-0.0.1-SNAPSHOT.jar"
    spark-submit C:\work\SPHINX\anomaly-detection\target\anomaly-detection-0.0.1-SNAPSHOT.jar --class ro.simavi.sphinx.ad.kernel.test.CountNumber
    spark-submit --class ro.simavi.sphinx.ad.kernel.test.CountNumber C:\work\SPHINX\anomaly-detection\target\anomaly-detection-0.0.1-SNAPSHOT.jar

    spark-submit --class ro.simavi.sphinx.ad.ADApplication C:\work\SPHINX\anomaly-detection\target\anomaly-detection-0.0.1-SNAPSHOT.jar
    spark-submit C:\work\SPHINX\anomaly-detection\target\anomaly-detection-0.0.1-SNAPSHOT.jar  --class ro.simavi.sphinx.ad.ADApplication
    Error: Failed to load class ro.simavi.sphinx.ad.kernel.test.CountNumber.

     */
    public static void main2(String arg[]){

        SparkConf sparkConf = new SparkConf()
                .setMaster("spark://172.18.252.171:7077")
                //.setMaster("local[*]")
                .setAppName("CountNumber");
        JavaSparkContext sparkContext = new JavaSparkContext(sparkConf);

        String SPACE_DELIMITER = " ";

        //JavaRDD<String> input = sparkContext.textFile("test\\numbers.txt");

        List<String> list = new ArrayList<>();
        list.add("1");
        list.add("1");
        list.add("2");
        JavaRDD<String> input = sparkContext.parallelize(list);

        Map<String, Long> wordCounts = input.countByValue();

        System.out.println("-------word Counts----------"+wordCounts);

        JavaRDD<String> numberStrings = input.flatMap(s -> Arrays.asList(s.split(SPACE_DELIMITER)).iterator());
        JavaRDD<String> validNumberString = numberStrings.filter(string -> !string.isEmpty());
        JavaRDD<Integer> numbers = validNumberString.map(numberString -> Integer.valueOf(numberString));
        int finalSum = numbers.reduce((x,y) -> x+y);

        System.out.println("-------sum----------"+finalSum);

        sparkContext.stop();

    }
}
