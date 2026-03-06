package ro.simavi.sphinx.util;

import java.text.CharacterIterator;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.text.StringCharacterIterator;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.TimeZone;

public class FormatHelper {

    public static final String DATE = "MMM d, yyyy HH:mm:ss";

    private static SimpleDateFormat sdf = new SimpleDateFormat(FormatHelper.DATE);

    public static String humanReadableByteCountSI(long bytes) {
        if (-1000 < bytes && bytes < 1000) {
            return bytes + " B";
        }
        CharacterIterator ci = new StringCharacterIterator("kMGTPE");
        while (bytes <= -999_950 || bytes >= 999_950) {
            bytes /= 1000;
            ci.next();
        }
        return String.format("%.1f %cB", bytes / 1000.0, ci.current());
    }

    public static Date toDate(String timeString){
        try {
            return sdf.parse(timeString.split("\\.")[0]);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static synchronized Long getTimestamp(String timeString){
        if (timeString==null){
            return null;
        }
      //  System.out.println("----------------------------------");
        String[] timeSplit = timeString.split("\\.");
        Long timestamp = null;
        try {
            if (timeSplit.length>0){
                String time = timeSplit[0];
               // System.out.println("===> time:"+time);
                Date date = sdf.parse(time);
              //  System.out.println("===> date:"+date);
                timestamp = date.getTime();
              //  System.out.println("===> timestamp:"+timestamp);
                return timestamp;
            }
        } catch (Exception e) {
            System.out.println("error:"+timeString + " " + timestamp);
        }
     //   System.out.println("----------------------------------");
        return timestamp;
    }

    public static void main(String[] arg){
        String timeString = "Nov 29, 2020 17:11:24.163843000 GTB Standard Time";
        System.out.println(getTimestamp(timeString));
    }
    public static LocalDateTime toLocalDateTime(String timeString){
        try {

            String[] time = timeString.split("\\.");
            if (time.length>0){

                Date date = sdf.parse(time[0]);
                return Instant.ofEpochMilli(date.getTime())
                        .atZone(ZoneId.systemDefault())
                        .toLocalDateTime();

            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static LocalDateTime toLocalDateTime(Long timestamp){

        if (timestamp==null){
            return null;
        }
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp),
                        TimeZone.getDefault().toZoneId());

    }

    public static LocalDate toLocalDate(String timeString) {
        try {
            Date dateToConvert = toDate(timeString);
            return Instant.ofEpochMilli(dateToConvert.getTime())
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate();
        }catch (Exception e){

        }
        return null;
    }

}
