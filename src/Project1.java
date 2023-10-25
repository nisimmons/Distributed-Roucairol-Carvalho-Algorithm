import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;

/**
 * Created by Nate Simmons on 10/10/2023
 * This class is the main class for a system of client/servers that communicate with each other using sockets.
 * It reads in a config file and creates the nodes and their neighbors. It then starts the threads for this node.
 * The nodes will communicate with each other until all nodes are passive. Then the program will exit.
 * There is also support for a snapshot thread that sends messages to neighbor nodes to get their state.
 */
public class Project1 {
    private int id;
    private String hostName;
    private int port;
    private int minPerActive;
    private int maxPerActive;
    private int minSendDelay;
    private int maxNumberMessages;
    private int numMessagesSent;
    private int snapshotDelay;
    private Neighbor[] neighbors;
    private boolean active;
    private boolean alive;
    private Thread listenThread;
    private Thread sendThread;
    private Thread snapShotThread;
    ArrayList<String> messages;
    public static String projectDir = "/home/013/n/ni/nis190000/cs6378/Project";
    private boolean red;
    private String configFilename;

    private int[] fidgeClock;


    /**
     * This is the main method for the program. It creates a new Project1 object and passes the args to it.
     * @param args directory for config file
     */
    public static void main(String[]args)  {
        Project1 p = new Project1(args);
    }

    /**
     * It reads in the config file and creates the nodes and their neighbors.
     * It then starts the threads for each node. The nodes will communicate with each other until all nodes are passive.
     * Then the program will join all threads and exit.
     * @param args directory for config file
     */
    public Project1(String[]args){
        //open config file specified in args[0]
        configFilename = args[0];
        try {
            readFile(configFilename);
        } catch (FileNotFoundException | UnknownHostException e) {
            e.printStackTrace();
        }
        //initialize some variables
        alive = true;
        red = false;
        messages = new ArrayList<>();
        Arrays.fill(fidgeClock, 0);

        //create listen thread
        listenThread = new Thread(() -> {

            //create socket
            try {
                InetAddress address = InetAddress.getByName(hostName);
                InetSocketAddress socketAddress = new InetSocketAddress(address, port);
                ServerSocket socket = new ServerSocket();
                System.out.print("Attempting to bind socket to " + hostName + ":" + port + "...");
                socket.bind(socketAddress);
                System.out.println("Success!");
                System.out.println("My neighbors are:");
                for(Neighbor n : neighbors){
                    System.out.println("\t" + n.getId() + "\t" + n.getHostName() + "\t" + n.getPort());
                    n.setRed(false);
                }

                //listen for messages
                while (alive) {
                    Socket clientSocket = socket.accept();
                    PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                    BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                    String clientMessage;
                    while ((clientMessage = in.readLine()) != null && alive) {
                        //System.out.println("Recieved message \"" + clientMessage + "\" from client " + clientSocket.getInetAddress().getHostName());
                        int clientID;
                        switch(clientMessage.substring(0,3)){
                            case "CLI":
                                if(numMessagesSent < maxNumberMessages)
                                    active = true;
                                //get the value between # in clientmessage
                                fidgeClock[Integer.parseInt(clientMessage.split("#")[1])]++;
                                updateFidgeClock(fidgeClock, clientMessage);
                                socket.setSoTimeout(minSendDelay * maxPerActive * neighbors.length+8000);
                                break;
                            case "SNP":
                                //save state
                                if(!red) {
                                    red = true;
                                    messages.add(Arrays.toString(fidgeClock) + "\n");
                                    System.out.println("Snapshot taken");
                                    //send snap to neighbors
                                    for (Neighbor n : neighbors) {
                                        Socket neighborSocket = new Socket(n.getHostName(), n.getPort());
                                        PrintWriter neighborOut = new PrintWriter(neighborSocket.getOutputStream(), true);
                                        neighborOut.println("SNP #" + id + "#");
                                        neighborSocket.close();
                                    }
                                }
                                clientID = Integer.parseInt(clientMessage.split("#")[1]);
                                //send ack to parent
                                if(clientID != id) {
                                    //find parent in neighbors by id
                                    for (Neighbor n : neighbors) {
                                        if (n.getId() == clientID) {
                                            Socket parentSocket = new Socket(n.getHostName(), n.getPort());
                                            PrintWriter parentOut = new PrintWriter(parentSocket.getOutputStream(), true);
                                            parentOut.println("ACK #" + id + "#");
                                            parentSocket.close();
                                        }
                                    }
                                }
                                break;
                            case "ACK":
                                clientID = Integer.parseInt(clientMessage.split("#")[1]);
                                //set neighbor to red
                                for(Neighbor n: neighbors){
                                    if(n.getId() == clientID){
                                        n.setRed(true);
                                        break;
                                    }
                                }
                                //if all neighbors are red, set self to blue
                                boolean allRed = true;
                                for(Neighbor n : neighbors){
                                    if(!n.isRed()){
                                        allRed = false;
                                        break;
                                    }
                                }
                                if(allRed) {
                                    red = false;
                                    //set all neighbors to blue
                                    for (Neighbor n : neighbors) {
                                        n.setRed(false);
                                    }
                                }
                                break;
                            case "INQ":
                                //respond with passive/active and vector clock

                                out.println("#" + id + "# " + (active ? "active" : "passive") + " " + messages.get(Integer.parseInt(clientMessage.split("#")[1])));
                                break;
                            case "KIL":
                                System.out.println("Received kill message");
                                //kill node
                                alive = false;
                                writeOutput();
                                //attempt to kill neighbors
                                for(Neighbor n : neighbors){
                                    try {
                                        Socket neighborSocket = new Socket(n.getHostName(), n.getPort());
                                        PrintWriter neighborOut = new PrintWriter(neighborSocket.getOutputStream(), true);
                                        neighborOut.println("KIL");
                                        neighborSocket.close();
                                    }catch (IOException ignored){} //Do not do anything since that neighbor is already dead
                                }
                                break;
                        }

                    }
                }
                socket.close();
            }catch (FileNotFoundException | UnknownHostException e) {
                e.printStackTrace();
            } catch (IOException e) {
                //socket timed out or failed to bind (probably failed to bind)
                System.out.println("Failure. Exiting...");
                alive = false;
                writeOutput();
            }
            //listen thread ends
        });
        listenThread.start();
        //Give some space so we don't try to start sending messages before the listen thread is ready
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        //create send thread
        sendThread = new Thread(()-> {
            while(numMessagesSent < maxNumberMessages && alive) {
                try {
                    Thread.sleep(minSendDelay);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if(active && !red) {
                    //generate a random number of messages to send
                    int numMessagesToSend = (int) (Math.random() * (maxPerActive - minPerActive) + minPerActive);
                    //choose random neighbors to send the message to
                    int trycount = 0;
                    for (int i = 0; i < numMessagesToSend; i++) {
                        try {
                            Thread.sleep(minSendDelay);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        try {
                            //randomly pick one neighbor to send the message to
                            int neighbor = (int) (Math.random() * neighbors.length);
                            //uncomment for verbose output
                            //System.out.println("Node " + id + " sending message to node " + nodes[id].getNeighbors()[neighbor] + " Hostname: " + nodes[nodes[id].getNeighbors()[neighbor]].getHostName() + " Port: " + nodes[nodes[id].getNeighbors()[neighbor]].getPort() + "");

                            //make the socket
                            Socket socket = new Socket(neighbors[neighbor].getHostName(), neighbors[neighbor].getPort());
                            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                            out.println("CLIENT #" + id + "# CLOCK: " + Arrays.toString(fidgeClock));
                            //increment the number of messages sent
                            numMessagesSent += 1;
                            fidgeClock[id] += 1;
                            socket.close();
                        } catch (IOException e) {
                            if (trycount <= 10) {
                                i--;
                                trycount++;
                                try {
                                    Thread.sleep(1000);
                                } catch (InterruptedException e1) {
                                    e1.printStackTrace();
                                }
                            }
                        }
                    }
                    active =false;
                }
            }
            //send thread ends
        });
        sendThread.start();

        //if this is the first node, start the snapshot thread as well
        if(id == 0) {
            //create snapshot thread
            snapShotThread = new Thread(() -> {
                Neighbor[] nodes = null;
                int snapshotCount = 0;
                while (alive) {
                    //wait for snapshotDelay
                    try {
                        Thread.sleep(snapshotDelay);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    //send snapshot to self if we are not red to get the ball rolling
                    if (alive && !red) {
                        try {
                            Socket socket = new Socket(hostName, port);
                            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                            out.println("SNP #" + id + "#");
                            socket.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    boolean allPassive = true;
                    if (nodes == null) {
                        //open config file and read in all neighbor info
                        try {
                            nodes = readNeighbors(configFilename);
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        }
                    }
                    //create an arraylist to hold all the local snapshots
                    ArrayList<int[]> localSnapshots = new ArrayList<>();
                    //send INQ to all nodes to check passivity
                    for (Neighbor n : nodes) {
                        try {
                            Socket neighborSocket = new Socket(n.getHostName(), n.getPort());
                            PrintWriter neighborOut = new PrintWriter(neighborSocket.getOutputStream(), true);
                            neighborOut.println("INQ #" + snapshotCount + "#");
                            //and receive response
                            Scanner neighborIn = new Scanner(neighborSocket.getInputStream());
                            String response = neighborIn.nextLine();
                            if (response.contains("active"))
                                allPassive = false;
                            //get the local snapshot from the response
                            String[] splitResponse = response.split(" ");
                            String[] snapShotString = splitResponse[2].substring(1, splitResponse[2].length() - 1).split(",");
                            int[] snapShotNumbers = new int[snapShotString.length];
                            for (int i = 0; i < snapShotString.length; i++) {
                                snapShotNumbers[i] = Integer.parseInt(snapShotString[i]);
                            }
                            localSnapshots.add(snapShotNumbers);
                            neighborSocket.close();
                        } catch (IOException ignored) {}
                    }
                    //check if snapshots are consistent
                    boolean consistent = true;
                    for (int i = 0; i < localSnapshots.size(); i++) {
                        for (int j = i + 1; j < localSnapshots.size(); j++) {
                            boolean strictlyLess = false;
                            boolean strictlyGreater = false;
                            boolean equal = true;
                            for (int k = 0; k < localSnapshots.get(i).length; k++) {
                                if (localSnapshots.get(i)[k] < localSnapshots.get(j)[k])
                                    strictlyLess = true;
                                if (localSnapshots.get(i)[k] > localSnapshots.get(j)[k])
                                    strictlyGreater = true;
                                if (localSnapshots.get(i)[k] != localSnapshots.get(j)[k])
                                    equal = false;
                            }
                            if (strictlyLess && strictlyGreater && !equal) {
                                consistent = false;
                                break;
                            }
                        }
                        if (!consistent)
                            break;
                    }
                    if(consistent){
                        System.out.println("Snapshot Daemon: Snapshots are consistent");
                    }
                    else{
                        System.out.println("Snapshot Daemon: Snapshots are not consistent");
                    }
                    if (allPassive) {
                        //send KIL to self
                        try {
                            Socket socket = new Socket(hostName, port);
                            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                            System.out.println("Snapshot Daemon: All nodes passive, sending Kill message");
                            out.println("KIL");
                            socket.close();
                            break;
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
                //snapshot thread ends
            });
            snapShotThread.start();
        }
    }

    /**
     * Reads in the config file and returns an array of neighbors
     * @param filename the name of the config file
     * @return an array of neighbors
     * @throws FileNotFoundException if the config file is not found
     */
    private Neighbor[] readNeighbors(String filename) throws FileNotFoundException {
        Neighbor[] neighbors;
        //read the config file
        Scanner scanner = new Scanner(new File(filename));
        String s;
        Scanner line;
        //read until we get a valid line
        do {
            s = scanner.nextLine();
            s = s.replaceAll("#.*", "");    //remove comments
            s = s.trim();                                   //remove leading and trailing whitespace
        } while (!s.matches("^\\d .*"));              //check if the line starts with an unsigned integer
        line = new Scanner(s);
        int nodeCount = line.nextInt();
        line.nextInt();
        line.nextInt();
        line.nextInt();
        line.nextInt();
        line.nextInt();
        neighbors = new Neighbor[nodeCount];

        //fill the neighbor list with all node info
        for (int i = 0; i < nodeCount; i++) {
            neighbors[i] = new Neighbor();
            //read until we get a valid line
            do {
                s = scanner.nextLine();
                s = s.replaceAll("#.*", "");
                s = s.trim();
            } while (!s.matches("^\\d+ .*"));

            line = new Scanner(s);
            neighbors[i].setId(line.nextInt());
            neighbors[i].setHostName(line.next());
            neighbors[i].setPort(line.nextInt());
        }
        return neighbors;
    }

    /**
     * Reconciles the local clock with the clock received from a client
     * @param fidgeClock the local clock
     * @param clientMessage the message received from the client
     */
    private void updateFidgeClock(int[] fidgeClock, String clientMessage) {
        //parse the client clock from the message
        int[] clientClock = new int[fidgeClock.length];
        String[] clock = clientMessage.substring(clientMessage.indexOf('[')+1, clientMessage.indexOf(']')).split(", ");
        //convert the string array to an int array and update the local clock
        for(int i = 0; i < clock.length; i++){
            clientClock[i] = Integer.parseInt(clock[i]);
        }
        for(int i = 0; i < fidgeClock.length; i++){
            fidgeClock[i] = Math.max(fidgeClock[i], clientClock[i]);
        }
    }

    /**
     * Writes the output to a file
     */
    private void writeOutput() {
        //write output to config<id>.out
        try {
            FileWriter writer = new FileWriter(projectDir+"/config-" + id + ".out");
            //write all messages
            for(String s : messages){
                //just write the numbers separated by a space and a newline between messages
                String[] printArr = s.substring(s.indexOf('[')+1, s.indexOf(']')).split(", ");
                for(int i = 0; i < printArr.length; i++){
                    writer.write(printArr[i]);
                    if(i != printArr.length - 1)
                        writer.write(" ");
                }
                writer.write("\n");
            }

            writer.close();
            System.out.println("Output written to config" + id + ".out");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Reads the config file and sets the node information
     * @param filename the name of the config file
     * @throws FileNotFoundException if the file is not found
     * @throws UnknownHostException if the host is not found
     */
    public void readFile(String filename) throws FileNotFoundException, UnknownHostException {
        //read the config file
        Scanner scanner = new Scanner(new File(filename));
        String s;
        Scanner line;
        //read until we get a valid line
        do {
            s = scanner.nextLine();
            s = s.replaceAll("#.*", "");    //remove comments
            s = s.trim();                                   //remove leading and trailing whitespace
        } while (!s.matches("^\\d .*"));              //check if the line starts with an unsigned integer
        line = new Scanner(s);
        int nodeCount = line.nextInt();
        minPerActive = line.nextInt();
        maxPerActive = line.nextInt();
        minSendDelay = line.nextInt();
        snapshotDelay = line.nextInt();
        maxNumberMessages = line.nextInt();
        neighbors = new Neighbor[nodeCount];

        //fill the neighbor list with all node info
        for (int i = 0; i < nodeCount; i++) {
            neighbors[i] = new Neighbor();
            //read until we get a valid line
            do {
                s = scanner.nextLine();
                s = s.replaceAll("#.*", "");
                s = s.trim();
            } while (!s.matches("^\\d+ .*"));

            line = new Scanner(s);
            neighbors[i].setId(line.nextInt());
            neighbors[i].setHostName(line.next());
            neighbors[i].setPort(line.nextInt());

            //if this is the current node, set the id, hostname, and port
            if(InetAddress.getByName(neighbors[i].getHostName()).getHostAddress().equals(InetAddress.getLocalHost().getHostAddress())) {
                id = neighbors[i].getId();
                hostName = neighbors[i].getHostName();
                port = neighbors[i].getPort();
                System.out.println("ID: " + id + "\tHostname: " + hostName + "\tPort: " + port);
                if(id == 0){
                    active = true;
                    System.out.println("I'm active!");
                }
            }
        }

        fidgeClock = new int[neighbors.length];

        //reduce the neighbor list to only the neighbors of this list
        for (int i = 0; i < neighbors.length; i++) {

            //read until we get a valid line
            do {
                s = scanner.nextLine();
                s = s.replaceAll("#.*", "");
                s = s.trim();
            } while (!s.matches("^\\d+ .*"));

            String[] neighborList = s.split(" ");
            if(i == id){
                Neighbor[] neighborArray = new Neighbor[neighborList.length];
                for (int j = 0; j < neighborArray.length; j++) {
                    neighborArray[j] = new Neighbor();
                    neighborArray[j].setId(neighbors[Integer.parseInt(neighborList[j])].getId());
                    neighborArray[j].setHostName(neighbors[Integer.parseInt(neighborList[j])].getHostName());
                    neighborArray[j].setPort(neighbors[Integer.parseInt(neighborList[j])].getPort());
                }
                neighbors = neighborArray;
            }
        }
    }
}