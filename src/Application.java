import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Random;

public class Application  {
    MutualExclusion mutex;
    private int numNodes;
    private final int interRequestDelay;
    private final int csExecTime;
    private final int requestsPerNode;
    private final int nodeID;
    private final String projectDir;
    Random rand = new Random();
    public Application(MutualExclusion mutualExclusion, int nodeID, String projectDir, int interRequestDelay, int requestsPerNode, int csExecTime, int numNodes) {
        this.mutex = mutualExclusion;
        this.nodeID = nodeID;
        this.projectDir = projectDir;
        this.interRequestDelay = interRequestDelay;
        this.requestsPerNode = requestsPerNode;
        this.csExecTime = csExecTime;
        mutex.fidgeClock = new int[numNodes];
        for(int i = 0; i < numNodes; i++) {
            mutex.fidgeClock[i] = 0;
        }

        app = new Thread(() -> {
            try {
                FileWriter fileWriter = new FileWriter(projectDir + "\\output"+nodeID+".txt");
                //loop for requestsPerNode
                for (int i = 0; i < requestsPerNode; i++) {
                    //call mutex csEnter
                    mutex.csEnter();
                    System.out.println("Node " + nodeID + " entered CS");
                    mutex.fidgeClock[nodeID]++;
                    System.out.println("Fidge clock: " + Arrays.toString(mutex.fidgeClock));

                    //write to output file
                    fileWriter.write(nodeID + " enters " + Arrays.toString(mutex.fidgeClock) + "\n");
                    fileWriter.flush();
                    //wait for csExecTime
                    mutex.fidgeClock[nodeID]++;
                    Thread.sleep((long) exponentialProbabilityDistribution(csExecTime));
                    //write to output file
                    fileWriter.write(nodeID + " leaves " + Arrays.toString(mutex.fidgeClock) + "\n");
                    fileWriter.flush();

                    //call mutex csLeave
                    mutex.fidgeClock[nodeID]++;
                    System.out.println("Node " + nodeID + " left CS");
                    mutex.csLeave();

                    //wait for interRequestDelay and loop
                    Thread.sleep((long) exponentialProbabilityDistribution(interRequestDelay));
                }
                fileWriter.close();
                System.out.println("Node " + nodeID + " app finished");
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }
            mutex.finish();
        });
        app.start();
    }

    Thread app;
    /**
     * Main method for the application thread. Calls mutex.csEnter, writes to output file, sleeps for csExecTime, writes to output file, and calls mutex.csLeave
     */


    private double exponentialProbabilityDistribution(int x){
        /*Random random = new Random();
        return -x * Math.log(1 - random.nextDouble());*/
        //return x*random number between 1 and 4
        return x*(rand.nextInt(4)+1);
    }
}
