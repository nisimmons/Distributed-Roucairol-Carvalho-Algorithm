import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
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


    public static void main(String[] args){
        Project2Node node = new Project2Node();
        try {
            if(args.length == 2) {
                node.readFile(args[0], node.nodeID);
            }
            else {
                node.readFile(args[0]);
            }
            node.run();
            node.writeOutput();
        } catch (FileNotFoundException | UnknownHostException e) {
            e.printStackTrace();
        }
    }

    public void readFile(String filename) throws FileNotFoundException, UnknownHostException {
        projectDir = filename;
        //read the config file
        Scanner scanner = new Scanner(new File(filename+"\\config.txt"));
        String s;
        ArrayList<String> validLines = new ArrayList<>();
        Scanner line;

        while(scanner.hasNext()) {
            do {
                s = scanner.nextLine();
                s = s.replaceAll("#.*", "");    //remove comments
                s = s.trim();                                   //remove leading and trailing whitespace
            } while (!s.matches("^\\d .*"));              //check if the line starts with an unsigned integer
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
                break;
            }

        }

    }

    public void readFile(String filename, int nodeID) throws FileNotFoundException {
        projectDir = filename;
        //read the config file
        Scanner scanner = new Scanner(new File(filename + "\\config.txt"));
        String s;
        ArrayList<String> validLines = new ArrayList<>();
        Scanner line;

        while(scanner.hasNext()) {
            do {
                s = scanner.nextLine();
                s = s.replaceAll("#.*", "");    //remove comments
                s = s.trim();                                   //remove leading and trailing whitespace
            } while (!s.matches("^\\d .*"));              //check if the line starts with an unsigned integer
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
                break;
            }
        }
    }

    public void run(){
        messages.add("Node " + nodeID + " is starting on " + ip + ":" + port);
    }

    public void writeOutput(){
        //write output to config<id>.out
        try {
            FileWriter writer = new FileWriter(projectDir+"/config-" + nodeID + ".out");
            //write all messages
            for(String s : messages){
                writer.write(s);
                writer.write("\n");
            }

            writer.close();
            System.out.println("Output written to config" + nodeID + ".out");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
