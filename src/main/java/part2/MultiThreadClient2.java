package part2;

import io.swagger.client.*;
import io.swagger.client.api.SkiersApi;
import io.swagger.client.model.*;

import java.io.File;
import java.io.InputStreamReader;
import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class MultiThreadClient2 {
    static int numThread;
    static int numSkier;
    static int numRun = 10;
    static int liftNum = 40;
    static String basePath = "http://35.88.244.109:8080/cs6500_lab_war/";

    static DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;
    static Thread[] threadPool1;
    static Thread[] threadPool2;
    static Thread[] threadPool3;

    // these variables will be accessed by different threads
    static AtomicInteger phase1FinishedThreadNum = new AtomicInteger(0);
    static AtomicInteger phase2FinishedThreadNum = new AtomicInteger(0);
    static AtomicInteger phase3FinishedThreadNum = new AtomicInteger(0);
    static AtomicInteger numOfFailures = new AtomicInteger(0);
    static AtomicInteger numOfSuccess = new AtomicInteger(0);
    static List<String[]> records = Collections.synchronizedList(new ArrayList<>());
    static List<Long> latencies = Collections.synchronizedList(new ArrayList<>());

    private static void getParams() {
        Scanner scanner = new Scanner(new InputStreamReader(System.in));
        System.out.println("Please input the maximum number of threads, with a limit of 1024.");
        numThread = scanner.nextInt();
        System.out.println("Please input the number of skiers, with a limit of 100000.");
        numSkier = scanner.nextInt();
        System.out.println("Please input the number of lifts, with a range of 5 - 60.");
        liftNum = scanner.nextInt();
        System.out.println("Please input the mean numbers of ski lifts each skier rides each day, with a limit of 20.");
        numRun = scanner.nextInt();
    }

    private static void executePhase1() {
        int phase1NumThread = numThread / 4;
        threadPool1 = new Thread[phase1NumThread];
        int skierNumGroup = numSkier / phase1NumThread;
        for (int i = 0; i < phase1NumThread; i++) {
            int skierIdBegin = i * skierNumGroup;
            int skierIdEnd = (i + 1) * skierNumGroup - 1;
            threadPool1[i] = new Thread(new MyRunnable(basePath, skierIdBegin, skierIdEnd, liftNum, (int) Math.ceil((numRun * 0.2) * skierNumGroup), 0, 90, 1));
            threadPool1[i].start();
        }
    }

    private static void executePhase2() {
        int skierNumGroup = numSkier / numThread;
        threadPool2 = new Thread[numThread];
        for (int i = 0; i < numThread; i++) {
            int skierIdBegin = i * skierNumGroup;
            int skierIdEnd = (i + 1) * skierNumGroup - 1;
            threadPool2[i] = new Thread(new MyRunnable(basePath, skierIdBegin, skierIdEnd, liftNum, (int) Math.ceil((numRun * 0.6) * skierNumGroup), 91, 360, 2));
            threadPool2[i].start();
        }
    }

    private static void executePhase3() {
        int phase3NumThread = numThread / 10;
        threadPool3 = new Thread[phase3NumThread];
        int skierNumGroup = numSkier / numThread;
        for (int i = 0; i < phase3NumThread; i++) {
            int skierIdBegin = i * skierNumGroup;
            int skierIdEnd = (i + 1) * skierNumGroup - 1;
            threadPool3[i] = new Thread(new MyRunnable(basePath, skierIdBegin, skierIdEnd, liftNum, (int) Math.ceil((numRun * 0.1) * skierNumGroup), 361, 420, 3));
            threadPool3[i].start();
        }
    }

    public static void main(String[] args) {
        // read configs from command line
        getParams();

        Timestamp before = new Timestamp(System.currentTimeMillis());

        // execute phase 1
        executePhase1();

        // execute phase 2
        while (phase1FinishedThreadNum.get() < 0.2 * (numThread / 4)) {
            continue;
        }
        executePhase2();

        // execute phase 3
        while (phase2FinishedThreadNum.get() < 0.2 * numThread) {
            continue;
        }
        executePhase3();

//        // wait until all threads to finish
//        for (int i = 0; i < threadPool1.length; i++) {
//            threadPool1[i].join();
//        }
//        for (int i = 0; i < threadPool2.length; i++) {
//            threadPool2[i].join();
//        }
//        for (int i = 0; i < threadPool3.length; i++) {
//            threadPool3[i].join();
//        }
        while (phase1FinishedThreadNum.get() < numThread / 4 || phase2FinishedThreadNum.get() < numThread || phase3FinishedThreadNum.get() < (numThread / 10)) {
            continue;
        }

        Timestamp after = new Timestamp(System.currentTimeMillis());

        // post-processing
        long wallTime = (after.getTime() - before.getTime()) / 1000;
        int totalReq = numOfFailures.get() + numOfSuccess.get();
        long throughput = wallTime == 0 ? totalReq : totalReq / wallTime;

        System.out.println("the total run time is " + wallTime + " seconds.");
        System.out.println("the throughput is " + throughput);

        List<Long> latencyList = new ArrayList<>();
        for (int i = 0; i < latencies.size(); i++) {
            latencyList.add(latencies.get(i));
        }
        long minLatency = PostProcessing.getMin(latencyList);
        long maxLatency = PostProcessing.getMax(latencyList);
        long meanLatency = PostProcessing.getMean(latencyList);
        long medianLatency = PostProcessing.getMedian(latencyList);
        File csvFile = PostProcessing.getCSVFile(records);

        System.out.println("the min latency is " + minLatency);
        System.out.println("the max latency is " + maxLatency);
        System.out.println("the mean latency is " + meanLatency);
        System.out.println("the median latency is " + medianLatency);
    }
}

class MyRunnable implements Runnable {
    private final int skierIdBegin;
    private final int skierIdEnd;
    private final int liftNum;
    private final int iterateNum;
    private final String basePath;
    private final int numOfTrialsLimit = 5;
    private final int startTime;
    private final int endTime;
    private final int phase;

    MyRunnable(String basePath, int skierIdBegin, int skierIdEnd, int liftNum, int iterateNum, int startTime, int endTime, int phase) {
        this.skierIdBegin = skierIdBegin;
        this.skierIdEnd = skierIdEnd;
        this.iterateNum = iterateNum;
        this.liftNum = liftNum;
        this.basePath = basePath;
        this.startTime = startTime;
        this.endTime = endTime;
        this.phase = phase;
    }

    @Override
    public void run() {
        Random random = new Random();

        for (int i = 0; i < this.iterateNum; i++) {
            SkiersApi apiInstance = new SkiersApi();
            apiInstance.getApiClient().setBasePath(basePath);
            LiftRide liftRideBody = new LiftRide();
            liftRideBody.setLiftID(random.nextInt(liftNum));
            liftRideBody.setTime(random.nextInt(endTime - startTime) + startTime);
            liftRideBody.setWaitTime(random.nextInt(10));
            int numOfTrials = 0;
            try {
                // handle failure response
                while (numOfTrials <= numOfTrialsLimit) {
                    Timestamp beforeReq = new Timestamp(System.currentTimeMillis());
                    ApiResponse response = apiInstance.writeNewLiftRideWithHttpInfo(liftRideBody, 1, "1", "1", random.nextInt(skierIdEnd - skierIdBegin) + skierIdBegin);
                    numOfTrials++;
                    Timestamp afterReq = new Timestamp(System.currentTimeMillis());

                    // process each record and save it to the record list
                    long latency = afterReq.getTime() - beforeReq.getTime();
                    MultiThreadClient2.latencies.add(latency);
                    String latencyStr = Long.toString(latency);
                    String startTime = beforeReq.toLocalDateTime().format(MultiThreadClient2.formatter);
                    int resCode = response.getStatusCode();
                    String resCodeStr = Integer.toString(resCode);
                    String[] curr = new String[] {startTime, latencyStr, "POST", resCodeStr};
                    MultiThreadClient2.records.add(curr);

                    if (resCode == 201) {
                        MultiThreadClient2.numOfSuccess.getAndIncrement();
                        break;
                    }
                }
                if (numOfTrials > numOfTrialsLimit) {
                    MultiThreadClient2.numOfFailures.getAndIncrement();
                }
            } catch (ApiException e) {
                e.printStackTrace();
            }
        }

        if (phase == 1) {
            MultiThreadClient2.phase1FinishedThreadNum.getAndIncrement();
        }
        if (phase == 2) {
            MultiThreadClient2.phase2FinishedThreadNum.getAndIncrement();
        } else {
            MultiThreadClient2.phase3FinishedThreadNum.getAndIncrement();
        }
    }
}
