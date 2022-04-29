package part1;

import io.swagger.client.*;
import io.swagger.client.api.SkiersApi;
import io.swagger.client.model.*;

import java.io.InputStreamReader;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class MultiThreadClient {
    static int numThread;
    static int numSkier;
    static int numRun = 10;
    static int liftNum = 40;
    static String basePath = "http://18.236.230.247:8080/cs6500-lab_war/";

    // these variables will be accessed by different threads
    static AtomicInteger numOfFailures = new AtomicInteger(0);
    static AtomicInteger numOfSuccess = new AtomicInteger(0);


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

    private static void executePhase1(CountDownLatch completed1, CountDownLatch completed) {
        int phase1NumThread = numThread / 4;
        Thread[] threadPool1 = new Thread[phase1NumThread];
        int skierNumGroup = numSkier / phase1NumThread;
        for (int i = 0; i < phase1NumThread; i++) {
            int skierIdBegin = i * skierNumGroup;
            int skierIdEnd = (i + 1) * skierNumGroup - 1;
            threadPool1[i] = new Thread(new MyRunnable(basePath, skierIdBegin, skierIdEnd, liftNum, (int) Math.ceil((numRun * 0.2) * skierNumGroup), 0, 90, completed1, completed));
            threadPool1[i].start();
        }
    }

    private static void executePhase2(CountDownLatch completed2, CountDownLatch completed) {
        int skierNumGroup = numSkier / numThread;
        Thread[] threadPool2 = new Thread[numThread];
        for (int i = 0; i < numThread; i++) {
            int skierIdBegin = i * skierNumGroup;
            int skierIdEnd = (i + 1) * skierNumGroup - 1;
            threadPool2[i] = new Thread(new MyRunnable(basePath, skierIdBegin, skierIdEnd, liftNum, (int) Math.ceil((numRun * 0.6) * skierNumGroup), 91, 360, completed2, completed));
            threadPool2[i].start();
        }
    }

    private static void executePhase3(CountDownLatch completed3, CountDownLatch completed) {
        int phase3NumThread = numThread / 10;
        Thread[] threadPool3 = new Thread[phase3NumThread];
        int skierNumGroup = numSkier / numThread;
        for (int i = 0; i < phase3NumThread; i++) {
            int skierIdBegin = i * skierNumGroup;
            int skierIdEnd = (i + 1) * skierNumGroup - 1;
            threadPool3[i] = new Thread(new MyRunnable(basePath, skierIdBegin, skierIdEnd, liftNum, (int) Math.ceil((numRun * 0.1) * skierNumGroup), 361, 420, completed3, completed));
            threadPool3[i].start();
        }
    }

    public static void main(String[] args) throws InterruptedException {
        // read configs from command line
        getParams();

        Timestamp before = new Timestamp(System.currentTimeMillis());
        CountDownLatch completed = new CountDownLatch(numThread / 4 + numThread + numThread / 10);

        // execute phase 1
        System.out.println("executing phase 1");
        CountDownLatch completed1 = new CountDownLatch((int)(0.2 * (numThread / 4)));
        executePhase1(completed1, completed);
        completed1.await();

        // execute phase 2
        System.out.println("executing phase 2");
        CountDownLatch completed2 = new CountDownLatch((int)(0.2 * (numThread)));
        executePhase2(completed2, completed);
        completed2.await();

        // execute phase 3
        System.out.println("executing phase 3");
        CountDownLatch completed3 = new CountDownLatch(numThread / 4 + numThread + numThread / 10);
        executePhase3(completed3, completed);
        completed.await();

        Timestamp after = new Timestamp(System.currentTimeMillis());
        long wallTime = (after.getTime() - before.getTime()) / 1000;
        int totalReq = numOfFailures.get() + numOfSuccess.get();
        long throughput = wallTime == 0 ? totalReq : totalReq / wallTime;

        System.out.println("The result of running " + numThread + " threads of " + numSkier + " skiers are:");
        System.out.println("The number of successful requests are: " + numOfSuccess);
        System.out.println("The number of failed requests are: " + numOfFailures);
        System.out.println("the total run time is " + wallTime + " seconds.");
        System.out.println("the throughput is " + throughput);
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
    private CountDownLatch completed;
    private CountDownLatch completedAll;

    MyRunnable(String basePath, int skierIdBegin, int skierIdEnd, int liftNum, int iterateNum, int startTime, int endTime, CountDownLatch completed, CountDownLatch completedAll) {
        this.skierIdBegin = skierIdBegin;
        this.skierIdEnd = skierIdEnd;
        this.iterateNum = iterateNum;
        this.liftNum = liftNum;
        this.basePath = basePath;
        this.startTime = startTime;
        this.endTime = endTime;
        this.completed = completed;
        this.completedAll = completedAll;
    }

    @Override
    public void run() {
        Random random = new Random();

        for (int i = 0; i < this.iterateNum; i++) {
            SkiersApi apiInstance = new SkiersApi();
            apiInstance.getApiClient().setBasePath(basePath);
            apiInstance.getApiClient().getHttpClient().setConnectTimeout(600000, TimeUnit.MILLISECONDS);
            apiInstance.getApiClient().getHttpClient().setReadTimeout(600000, TimeUnit.MILLISECONDS);
            LiftRide liftRideBody = new LiftRide();
            liftRideBody.setLiftID(random.nextInt(liftNum));
            liftRideBody.setTime(random.nextInt(endTime - startTime) + startTime);
            liftRideBody.setWaitTime(random.nextInt(10));
            int numOfTrials = 0;
            int waitTime = 100;
            try {
                // handle failure response
                // adding in exponential backoff
                while (numOfTrials <= numOfTrialsLimit) {
                    if (numOfTrials > 0) {
                        waitTime = waitTime * 2;
                        wait(waitTime + (int)(100 * Math.random()));
                    }
                    ApiResponse response = apiInstance.writeNewLiftRideWithHttpInfo(liftRideBody, 1, "1", "3", random.nextInt(skierIdEnd - skierIdBegin) + skierIdBegin);
                    numOfTrials++;
                    if (response.getStatusCode() == 201) {
                        MultiThreadClient.numOfSuccess.getAndIncrement();
                        break;
                    }
                }
                if (numOfTrials > numOfTrialsLimit){
                    MultiThreadClient.numOfFailures.getAndIncrement();
                }
            } catch (ApiException | InterruptedException e) {
                e.printStackTrace();
                MultiThreadClient.numOfFailures.getAndIncrement();
            }
        }

        this.completedAll.countDown();
        this.completed.countDown();
    }
}