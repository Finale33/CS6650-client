package part2;

import com.opencsv.CSVWriter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PostProcessing {

    public static File getCSVFile(List<String[]> records)
    {
        File file = new File("records.CSV");
        try {
            // create FileWriter object with file as parameter
            FileWriter outputfile = new FileWriter(file);

            // create CSVWriter object filewriter object as parameter
            CSVWriter writer = new CSVWriter(outputfile);

            // adding header to csv
            String[] header = { "startTime       ", "latency", "reqType", "resCode" };
            writer.writeNext(header);

            // add data to csv
            for (int i = 0; i < records.size(); i++) {
                writer.writeNext(records.get(i));
            }

            // closing writer connection
            writer.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return file;
    }

    public static long getMin(List<Long> list) {
        long min = Long.MAX_VALUE;
        for (Long cur: list) {
            min = Math.min(min, cur);
        }
        return min;
    }

    public static long getMax(List<Long> list) {
        long max = Long.MIN_VALUE;
        for (int i = 0; i < list.size(); i++) {
            max = Math.max(max, list.get(i));
        }
        return max;
    }

    public static long getMean(List<Long> list) {
        return getSum(list) / list.size();
    }

    public static long getSum(List<Long> list) {
        long sum = 0;
        for (int i = 0; i < list.size(); i++) {
            sum += list.get(i);
        }
        return sum;
    }

    public static long getMedian(List<Long> list) {
        List<Long> curr = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            curr.add(list.get(i));
        }
        Collections.sort(curr);
        return curr.get(curr.size() / 2);
    }

    public static long percentile(List<Long> latencies, double percentile) {
        int index = (int) Math.ceil(percentile / 100.0 * latencies.size());
        return latencies.get(index-1);
    }



//    mean response time (millisecs), doing so when done
//    median response time (millisecs), doing so when done
//    p99 (99th percentile) response time. Here’s a nice article about why percentiles are important and why calculating them is not always easy. (millisecs)
}
