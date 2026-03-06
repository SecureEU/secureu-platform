package ro.simavi.sphinx.ad;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@SpringBootTest
public class SparkTest {

    @Autowired
    JavaSparkContext sparkContext;

    @Test
    void test() {
        String SPACE_DELIMITER = " ";

        //SparkConf conf = new SparkConf().setMaster("local[*]").setAppName("SparkFileSumApp");
        //SparkConf conf = new SparkConf().setMaster("spark://localhost:7077").setAppName("SparkFileSumApp");
        //JavaSparkContext sparkContext = new JavaSparkContext(conf);

       // JavaRDD<String> input = sparkContext.textFile("test\\numbers.txt");

        List<String> list = new ArrayList<>();
        list.add("1 2 3");
        JavaRDD<String> input = sparkContext.parallelize(list);

        JavaRDD<String> numberStrings = input.flatMap(s -> Arrays.asList(s.split(SPACE_DELIMITER)).iterator());
        JavaRDD<String> validNumberString = numberStrings.filter(string -> !string.isEmpty());
        JavaRDD<Integer> numbers = validNumberString.map(numberString -> Integer.valueOf(numberString));
        int finalSum = numbers.reduce((x,y) -> x+y);

        System.out.println("Final sum is: " + finalSum);

        //sparkContext.close();
    }
}
