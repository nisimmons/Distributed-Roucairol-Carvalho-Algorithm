import java.io.File;
import java.io.FileNotFoundException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Scanner;

public class Project2Node {
    private int numNodes;
    private int interRequestDelay;
    private int csExecTime;
    private int requestsPerNode;
    private int nodeID;
    private String ip;
    private int port;
    private String projectDir;
    private ArrayList<String> messages = new ArrayList<>();
    private ArrayList<Neighbor> neighbors = new ArrayList<>();


    public static void main(String[] args){
        Project2Node node = new Project2Node();
        try {
            if(args.length == 2) {
                node.readFile(args[0], Integer.parseInt(args[1]));
            }
            else {
                node.readFile(args[0]);
            }
            node.run();
        } catch (FileNotFoundException | UnknownHostException e) {
            e.printStackTrace();
        }
    }

    public void readFile(String dir) throws FileNotFoundException, UnknownHostException {
        projectDir = dir;
        //read the config file
        Scanner scanner = new Scanner(new File(projectDir + "/config.txt"));
        String s;
        ArrayList<String> validLines = new ArrayList<>();
        Scanner line;

        while(scanner.hasNext()) {
            s = scanner.nextLine();
            s = s.replaceAll("#.*", "");    //remove comments
            s = s.trim();                                   //remove leading and trailing whitespace
            if (s.matches("^\\d.*"))              //check if the line starts with an unsigned integer
                validLines.add(s);
        }


        //line 1 is nodes, inter-request delay, cs-exec time, requests per node
        line = new Scanner(validLines.get(0));
        numNodes = line.nextInt();
        interRequestDelay = line.nextInt();
        csExecTime = line.nextInt();
        requestsPerNode = line.nextInt();

        //for each line after that, call Project2 with arg[0] as the first int
        for(int i = 1; i < validLines.size(); i++){
            line = new Scanner(validLines.get(i));
            int node = line.nextInt();
            String ip = line.next();
            int port = line.nextInt();
            if(InetAddress.getByName(ip).getHostAddress().equals(InetAddress.getLocalHost().getHostAddress())){
                //this is the node
                this.nodeID = node;
                this.ip = ip;
                this.port = port;
            }
            else{
                Neighbor neighbor = new Neighbor();
                neighbor.setId(node);
                neighbor.setHostName(ip);
                neighbor.setPort(port);
                neighbors.add(neighbor);
            }

        }
    }

    public void readFile(String dir, int nodeID) throws FileNotFoundException {
        projectDir = dir;
        //read the config file
        Scanner scanner = new Scanner(new File(dir + "/config.txt"));
        String s;
        ArrayList<String> validLines = new ArrayList<>();
        Scanner line;

        while(scanner.hasNext()) {
            s = scanner.nextLine();
            s = s.replaceAll("#.*", "");    //remove comments
            s = s.trim();                                   //remove leading and trailing whitespace
            if (s.matches("^\\d.*"))              //check if the line starts with an unsigned integer
                validLines.add(s);
        }



        //line 1 is nodes, inter-request delay, cs-exec time, requests per node
        line = new Scanner(validLines.get(0));

        numNodes = line.nextInt();
        interRequestDelay = line.nextInt();
        csExecTime = line.nextInt();
        requestsPerNode = line.nextInt();

        //for each line after that, call Project2 with arg[0] as the first int
        for(int i = 1; i < validLines.size(); i++){
            line = new Scanner(validLines.get(i));
            int node = line.nextInt();
            String ip = line.next();
            int port = line.nextInt();
            if(node == nodeID) {
                //this is the node
                this.nodeID = node;
                this.ip = ip;
                this.port = port;

            }
            else{
                Neighbor neighbor = new Neighbor();
                neighbor.setId(node);
                neighbor.setHostName(ip);
                neighbor.setPort(port);
                neighbor.setAlive(true);
                neighbors.add(neighbor);
            }
        }
        if(nodeID == 1){
            System.out.println("Node " + nodeID + " neighbors:");
            for(Neighbor n: neighbors){
                System.out.println(n.getId() + " " + n.getHostName() + " " + n.getPort());
            }
        }
    }

    public void run(){
        //initialize the mutex and application and run
        MutualExclusion mt = new MutualExclusion(nodeID, ip, port, projectDir, numNodes, neighbors);
        new Application(mt, nodeID, projectDir, interRequestDelay, requestsPerNode, csExecTime, numNodes , neighbors);
    }
}
