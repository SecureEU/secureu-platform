package ro.simavi.sphinx.ad.dpi.test;

import java.util.Date;

public class MergeDataSetMain {

    public static void main(String arg[]){
        long start = new Date().getTime();

        DataSetUtil.merge(DataSet.CTU_Normal_4_only_DNS, DataSet.CTU_Normal_4_only_DNS, DataSet.CTU_Normal_4_only_DNS);

        long end = new Date().getTime();
        System.out.println("Time:"+(end-start));
    }

}

