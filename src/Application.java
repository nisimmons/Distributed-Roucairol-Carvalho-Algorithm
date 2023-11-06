import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.net.Socket;

public class Application  {
    private final boolean verbosity = true;
    MutualExclusion mutex;
    private int numNodes;
    private final int interRequestDelay;
    private final int csExecTime;
    private final int requestsPerNode;
    private final int nodeID;
    private final String projectDir;
    Random rand = new Random();
    public Application(MutualExclusion mutualExclusion, int nodeID, String projectDir, int interRequestDelay, int requestsPerNode, int csExecTime, int numNodes, ArrayList<Neighbor> neighbors) {
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

        for(Neighbor n: neighbors){
            //try to connect to n, and catch an exception if it fails. Repeat until it succeeds
            while(true){
                try {
                    //open a socket to n
                    Socket s = new Socket(n.getHostName(), n.getPort());
                    //if it succeeds, close the socket and break out of the loop
                    //send hello
                    s.getOutputStream().write("HELLO\n".getBytes());
                    s.close();
                    break;
                } catch (IOException e) {
                    //e.printStackTrace();
                    //System.out.println("Failed to connect to neighbor " + n.getId() + " at " + n.getHostName() + ":" + n.getPort());
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException interruptedException) {
                        //interruptedException.printStackTrace();
                    }
                }
            }
            System.out.println("Connected to neighbor " + n.getId() + " at " + n.getHostName() + ":" + n.getPort());
        }
        System.out.println("All neighbors connected");


        app = new Thread(() -> {
            try {
                Thread.sleep(100);
                mutex.messages = 0;
                FileWriter fileWriter = new FileWriter(projectDir.replace("config.txt", "output" + nodeID + ".txt"));
                //if(verbosity)
                    System.out.println("Node " + nodeID + " app started");
                long startTime = System.currentTimeMillis();
                //loop for requestsPerNode
                for (int i = 0; i < requestsPerNode; i++) {
                    //call mutex csEnter
                    long responseTime = mutex.csEnter();
                    fileWriter.write("RESPONSETIME " + responseTime + "\n");
                    if(verbosity)
                        System.out.println("Node " + nodeID + " entered CS");
                    mutex.fidgeClock[nodeID]++;
                    if(verbosity)
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
                    //if(verbosity)
                        //System.out.println("Node " + nodeID + " leaving CS");
                    mutex.csLeave();

                    //wait for interRequestDelay and loop
                    Thread.sleep((long) exponentialProbabilityDistribution(interRequestDelay));
                }
                long endTime = System.currentTimeMillis();
                fileWriter.write("TOTALTIME " + (endTime - startTime) + "\n");
                fileWriter.write("MESSAGES " + mutex.messages + "\n");

                fileWriter.close();

                if(verbosity)
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
        //return x*(rand.nextInt(4)+1);
        //return lambda e^-lambda*x
        return -x*Math.log(1-rand.nextDouble());


    }
}
