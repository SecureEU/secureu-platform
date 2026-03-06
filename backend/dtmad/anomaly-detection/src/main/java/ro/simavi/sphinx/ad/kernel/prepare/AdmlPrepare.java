package ro.simavi.sphinx.ad.kernel.prepare;

import org.apache.hadoop.hbase.client.Delete;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.filter.BinaryComparator;
import org.apache.hadoop.hbase.filter.CompareFilter;
import org.apache.hadoop.hbase.filter.SingleColumnValueFilter;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.spark.api.java.JavaPairRDD;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ro.simavi.sphinx.ad.kernel.hbase.AdmlHBaseRDD;

import java.io.IOException;
import java.util.Iterator;
import java.util.stream.IntStream;

public class AdmlPrepare {

    private static final Logger logger = LoggerFactory.getLogger(AdmlPrepare.class);

    public static void prepare(JavaPairRDD admlRDD) {
        logger.info("Cleaning HBase...");
        try {
            cleanFlows(admlRDD);
            cleanSFlows(admlRDD);
            cleanAuthRecords(admlRDD);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static void cleanFlows(JavaPairRDD admlRDD) throws IOException {
        /**
         * This is an illustration of the purge process in a fancy time-line.
         *
         *
         *        Sup1-denseTime       tSup1                         tSup2                          now
         *  old flows   |  dense period  |    training dirty period    |       don't touch           |      future
         * ------------------------------------------------------------------------------------------------------------->
         *  remove all     remove all        Remove flows w/o events
         *                   in par           priority_id=1 in par
         *
         *  You can change this, but the time below are reasonable
         *
         *  tSup2     = now - timeUnit
         *  tSup1     = now - 100*timeUnit
         *  denseTime = 2*timeUnit
         *
         *  24h = 86400000
         *  12h = 43200000
         *  06h = 21600000
         */

        // Delete old data from HBase 86400 is one day. You should need even more, depends on your available resources.

        logger.info("Cleaning adlm_flows...");

        Long now = System.currentTimeMillis();

        Long timeUnit = 21600000L; /* maybe one day (86400000) or half (43200000) */
        Long timeSuperior1 = now - (timeUnit * 100);
        Long timeSuperior2 = now - timeUnit;
        int nSplits = 4; /* number of parallel tasks */
        Long denseTime = timeUnit * 4;
        Long deltaT1 = denseTime / nSplits;
        Long deltaT2 = (timeSuperior2 - timeSuperior1) / nSplits;

        logger.info("Removing all older than " + timeSuperior1);

        int totalOld = (IntStream.range(1, nSplits)).map(k -> {
            try {
                Scan scan = new Scan();
                if (k == 0) {
                    scan.setTimeRange(0, timeSuperior1 - denseTime);
                } else {
                    scan.setTimeRange(timeSuperior1 - denseTime + deltaT1 * (k - 1), timeSuperior1 - denseTime + deltaT1 * k);
                }

                logger.info("TimeRange: " + scan.getTimeRange().toString());

                Table flowsTable = AdmlHBaseRDD.getInstance().getFlows();
                Iterator<Result> scanner = flowsTable.getScanner(scan).iterator();

                int counter = 0;
                while (scanner.hasNext()) {
                    flowsTable.delete(new Delete(scanner.next().getRow()));
                    counter += 1;
                }

                return counter;
            } catch (IOException e) {
                e.printStackTrace();
            }
            return 0;
        }).reduce((a, b) -> a + b).getAsInt();

        logger.info("Old rows dropped: " + totalOld);

        logger.info("Removing flows w/o events priority 1, which are between " + timeSuperior1 + " and " + timeSuperior2);

        int totalWOEvent = (IntStream.range(1, nSplits)).map(k -> {
            try {
                Scan scan = new Scan();
                SingleColumnValueFilter filter = new SingleColumnValueFilter(Bytes.toBytes("event"),
                        Bytes.toBytes("priority_id"),
                        CompareFilter.CompareOp.valueOf("NOT_EQUAL"),
                        new BinaryComparator(Bytes.toBytes("1")));

                filter.setFilterIfMissing(false);

                scan.setTimeRange(timeSuperior1 + deltaT2 * (k - 1), timeSuperior1 + deltaT2 * k);

                scan.setFilter(filter);

                logger.info("TimeRange: " + scan.getTimeRange().toString());
                Table flowsTable = AdmlHBaseRDD.getInstance().getFlows();

                Iterator<Result> scanner = flowsTable.getScanner(scan).iterator();

                int counter = 0;
                while (scanner.hasNext()) {
                    flowsTable.delete(new Delete(scanner.next().getRow()));
                    counter += 1;
                }
                return counter;
            } catch (IOException e) {
                e.printStackTrace();
            }
            return 0;
        }).reduce((a, b) -> a + b).getAsInt();

        logger.info("Flows without event priority 1 dropped: " + totalWOEvent);
    }

    private static void cleanAuthRecords(JavaPairRDD admlRDD) {



        // Delete old data from HBase 86400 is one day. You should need even more, depends on your available resources.

        logger.info("Cleaning adml_authrecords...");
        Long now = System.currentTimeMillis();

        Long timeUnit = 21600000l; /* maybe one day (86400000) or half (43200000) or quarter (21600000) */
        Long timeSuperior1 = now - timeUnit;
        int nSplits = 5; /* number of parallel tasks */
        Long denseTime = timeUnit*1;
        Long deltaT1 = denseTime/nSplits;
        //val deltaT2 = (timeSuperior2-timeSuperior1)/nSplits

        logger.info("Removing all older than "+timeSuperior1);
        int totalOld = (IntStream.range(1, nSplits)).map(k -> {
            try{
                Scan scan = new Scan();

                if(k==0) {
                    scan.setTimeRange(0, timeSuperior1 - denseTime);
                }else {
                    scan.setTimeRange(timeSuperior1 - denseTime + deltaT1 * (k - 1), timeSuperior1 - denseTime + deltaT1 * k);
                }

                logger.info("TimeRange: "+scan.getTimeRange().toString());

                Table authrecordsTable = AdmlHBaseRDD.getInstance().getAuthrecords();
                Iterator<Result> scanner = authrecordsTable.getScanner(scan).iterator();
                int counter=0;
                while(scanner.hasNext()) {
                    authrecordsTable.delete(new Delete(scanner.next().getRow()));
                    counter+=1;
                }

                return counter;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return 0;
        }).reduce((a, b) -> a + b).getAsInt();

        logger.info("Old rows dropped: "+totalOld);

    }

    private static void cleanSFlows(JavaPairRDD admlRDD) {
        /**
         * This is an illustration of the purge process in a fancy time-line.
         *
         *
         *        Sup1-denseTime       tSup1                          now
         *  old flows   |  dense period  |       don't touch           |      future
         * ------------------------------------------------------------------------------------------------->
         *  remove all     remove all
         *                   in par
         *
         *  You can change this, but the time below are reasonable
         *
         *  tSup2     = now - timeUnit
         *  tSup1     = now - 100*timeUnit
         *  denseTime = 2*timeUnit
         *
         *  24h = 86400000
         *  12h = 43200000
         *  06h = 21600000
         */

        // Delete old data from HBase 86400 is one day. You should need even more, depends on your available resources.

        logger.info("Cleaning adml_sflows...");
        Long now = System.currentTimeMillis();

        Long timeUnit = 21600000L; /* maybe one day (86400000) or half (43200000) or quarter (21600000) */
        Long timeSuperior1 = now - timeUnit;
        int nSplits = 5; /* number of parallel tasks */
        Long denseTime = timeUnit*1;
        Long deltaT1 = denseTime/nSplits;
        //val deltaT2 = (timeSuperior2-timeSuperior1)/nSplits

        logger.info("Removing all older than "+timeSuperior1);
        int totalOld = (IntStream.range(1, nSplits)).map(k -> {
            try {
                Scan scan = new Scan();

                if(k==0) {
                    scan.setTimeRange(0, timeSuperior1 - denseTime);
                }else {
                    scan.setTimeRange(timeSuperior1 - denseTime + deltaT1 * (k - 1), timeSuperior1 - denseTime + deltaT1 * k);
                }

                logger.info("TimeRange: "+scan.getTimeRange().toString());


                Table sflowsTable = AdmlHBaseRDD.getInstance().getSflowsTable();
                Iterator<Result> scanner = sflowsTable.getScanner(scan).iterator();

                int counter=0;
                while(scanner.hasNext()) {
                    sflowsTable.delete(new Delete(scanner.next().getRow()));
                    counter+=1;
                }

                return counter;
            } catch (IOException e) {
                e.printStackTrace();
            }
            return 0;
    }).reduce((a, b) -> a + b).getAsInt();

        logger.info("Old rows dropped: "+totalOld);
    }
}
