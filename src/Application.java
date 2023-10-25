import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalTime;

public class Application implements Runnable {
    MutualExclusion mutex;
    int numNodes;
    private final int interRequestDelay;
    private final int csExecTime;
    private final int requestsPerNode;
    private final int nodeID;
    private final String projectDir;
    public Application(MutualExclusion mutualExclusion, int nodeID, String projectDir, int interRequestDelay, int requestsPerNode, int csExecTime){
        this.mutex = mutualExclusion;
        this.nodeID = nodeID;
        this.projectDir = projectDir;
        this.interRequestDelay = interRequestDelay;
        this.requestsPerNode = requestsPerNode;
        this.csExecTime = csExecTime;
    }
    @Override
    public void run() {
        //loop for requestsPerNode
        for (int i = 0; i < requestsPerNode; i++) {
            //call mutex csEnter
            mutex.csEnter();
            //open file in append mode
            try {
                FileWriter fileWriter = new FileWriter(projectDir + "\\output.txt", true);
                //Note the start time
                LocalTime startTime = LocalTime.now();
                //wait for csExecTime
                Thread.sleep(csExecTime);
                //Note the end time
                LocalTime endTime = LocalTime.now();
                //write start, end, and node info
                fileWriter.write(nodeID + " " + startTime + " " + endTime + "\n");
                //close the file
                fileWriter.close();
                //call mutex csLeave
                mutex.csLeave();
                //wait for interRequestDelay and loop
                Thread.sleep(interRequestDelay);
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }

        }

    }
}
