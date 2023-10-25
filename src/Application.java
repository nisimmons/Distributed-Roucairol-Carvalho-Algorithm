import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Random;

public class Application implements Runnable {
    MutualExclusion mutex;
    private int numNodes;
    private final int interRequestDelay;
    private final int csExecTime;
    private final int requestsPerNode;
    private final int nodeID;
    private final String projectDir;
    private int[] fidgeClock;
    Random rand = new Random();
    public Application(MutualExclusion mutualExclusion, int nodeID, String projectDir, int interRequestDelay, int requestsPerNode, int csExecTime, int numNodes) {
        this.mutex = mutualExclusion;
        this.nodeID = nodeID;
        this.projectDir = projectDir;
        this.interRequestDelay = interRequestDelay;
        this.requestsPerNode = requestsPerNode;
        this.csExecTime = csExecTime;
        fidgeClock = new int[numNodes];
        for(int i = 0; i < numNodes; i++) {
            fidgeClock[i] = 0;
        }
    }
    @Override
    public void run() {
        try {
            FileWriter fileWriter = new FileWriter(projectDir + "\\output"+nodeID+".txt", true);
            //loop for requestsPerNode
            for (int i = 0; i < requestsPerNode; i++) {
                fidgeClock[nodeID]++;
                //call mutex csEnter
                mutex.csEnter(fidgeClock);
                fidgeClock[nodeID]++;

                //write to output file
                fileWriter.write(nodeID + " enters " + Arrays.toString(fidgeClock) + "\n");
                fileWriter.flush();
                //wait for csExecTime
                fidgeClock[nodeID]++;
                Thread.sleep((long) exponentialProbabilityDistribution(csExecTime));
                //write to output file
                fileWriter.write(nodeID + " leaves " + Arrays.toString(fidgeClock) + "\n");
                fileWriter.flush();

                //call mutex csLeave
                fidgeClock[nodeID]++;
                mutex.csLeave();

                //wait for interRequestDelay and loop
                Thread.sleep((long) exponentialProbabilityDistribution(interRequestDelay));
            }
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private double exponentialProbabilityDistribution(int x){
        return  Math.log(1-rand.nextDouble())/(-x);
    }
}
