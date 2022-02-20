package part1;

import io.swagger.client.ApiException;
import io.swagger.client.ApiResponse;
import io.swagger.client.api.SkiersApi;
import io.swagger.client.model.LiftRide;

import java.util.Random;
import java.sql.Timestamp;

public class SingleThreadTest {
    public static void main(String[] args) throws InterruptedException {

        Timestamp before = new Timestamp(System.currentTimeMillis());
        Thread t = new Thread(new MyRunnableForTest());
        t.start();
        t.join();
        Timestamp after = new Timestamp(System.currentTimeMillis());
        long wallTime = (after.getTime() - before.getTime()) / 1000;
        System.out.println("the total run time is " + wallTime + " seconds.");
        System.out.println("the throughput is " + 10000 / wallTime);
    }
}

class MyRunnableForTest implements Runnable {
    static final int requests = 10000;
    static final String basePath = "http://35.88.244.109:8080/cs6500_lab_war/";
    static final int numOfTrialsLimit = 5;

    @Override
    public void run() {
        Random random = new Random();

        for (int i = 0; i < this.requests; i++) {
            SkiersApi apiInstance = new SkiersApi();
            apiInstance.getApiClient().setBasePath(this.basePath);
            LiftRide liftRideBody = new LiftRide();
            liftRideBody.setLiftID(random.nextInt(40));
            liftRideBody.setTime(random.nextInt(420));
            liftRideBody.setWaitTime(random.nextInt(10));
            int numOfTrials = 0;
            try {
                // handle failure response
                while (numOfTrials <= numOfTrialsLimit) {
                    ApiResponse response = apiInstance.writeNewLiftRideWithHttpInfo(liftRideBody, 1, "1", "1", random.nextInt(1000));
                    numOfTrials++;
                    if (response.getStatusCode() == 201) {
                        MultiThreadClient.numOfSuccess.getAndIncrement();
                        break;
                    }
                }
                if (numOfTrials > numOfTrialsLimit){
                    MultiThreadClient.numOfFailures.getAndIncrement();
                }
            } catch (ApiException e) {
                e.printStackTrace();
            }
        }
    }
}