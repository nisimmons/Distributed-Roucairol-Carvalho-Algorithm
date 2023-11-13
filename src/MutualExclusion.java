import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Mutual exclusion class. Handles all mutual exclusion logic.
 */
public class MutualExclusion {
    private final boolean verbosity = false;
    private int nodeID;
    private String hostName;
    private int port;
    private int numNodes;
    private String projectDir;
    private CopyOnWriteArrayList<Integer> sentPerms;
    private CopyOnWriteArrayList<Integer> deferred;
    private HashSet<Integer> requested;
    private HashSet<Integer> resend;
    private int myRequestClock;
    HashMap<Integer, Neighbor> neighbors;
    private boolean inCS;
    public int[] fidgeClock;
    private boolean finished;
    public int messages;

    private Thread listener;
    private boolean alive;

    /**
     * Constructor for the mutual exclusion class. Creates a thread that listens for messages.
     * @param nodeID node ID
     * @param hostName host name
     * @param port port
     * @param projectDir project directory
     * @param numNodes number of nodes
     * @param neighborArrayList list of neighbors
     */
    public MutualExclusion(int nodeID, String hostName, int port, String projectDir, int numNodes, ArrayList<Neighbor> neighborArrayList) {
        this.nodeID = nodeID;
        this.hostName = hostName;
        this.port = port;
        this.projectDir = projectDir;
        this.numNodes = numNodes;
        alive = true;
        deferred = new CopyOnWriteArrayList<>();
        sentPerms = new CopyOnWriteArrayList<>();
        requested = new HashSet<>();
        resend = new HashSet<>();
        //fill neighbors
        this.neighbors = new HashMap<>();
        for(Neighbor n : neighborArrayList){
            this.neighbors.put(n.getId(), n);
            if(n.getId() < nodeID)
                sentPerms.add(n.getId());
            n.setAlive(true);
        }
        myRequestClock = -1;
        inCS = false;


        //start the listener thread
        listener = new Thread(() -> {
            try {
                ServerSocket socket = new ServerSocket(port);
                //if(verbosity)
                    System.out.println("Node " + nodeID + " listening on " + InetAddress.getLocalHost().getHostAddress() + ":" + port);
                //listen for messages
                while (alive) {
                    Socket clientSocket = socket.accept();
                    BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                    String clientMessage;
                    clientMessage = in.readLine();
                    if(verbosity)
                        System.out.println("Node " + nodeID + " received message \"" + clientMessage + "\"");
                    //reply yes if we have no request open or my timestamp is higher than incoming, else add to deferred
                    String[] message = clientMessage.split(" ");
                    // format is REQUEST <nodeID> <timestamp> <fidgeClock>
                    switch(message[0]){
                        case "REQUEST":
                            int clientNodeID = Integer.parseInt(message[1]);
                            int clientClock = Integer.parseInt(message[2]);
                            if((myRequestClock == -1 || clientClock < myRequestClock || (clientClock == myRequestClock && clientNodeID < nodeID)) && !inCS){
                            //if I have no request or incoming clock is lower than mine, or incoming clock is equal to mine and incoming node is lower than mine, and we are not in CS, reply yes
                                if(!sentPerms.contains(clientNodeID))
                                    sentPerms.add(clientNodeID);
                                if(deferred.contains(clientNodeID))
                                    deferred.remove((Integer) clientNodeID);

                                //send permission
                                while(true) {
                                    try {
                                        if(!neighbors.get(clientNodeID).isAlive()){
                                            break;
                                        }
                                        Socket socket2 = new Socket(neighbors.get(clientNodeID).getHostName(), neighbors.get(clientNodeID).getPort());
                                        PrintWriter out = new PrintWriter(socket2.getOutputStream(), true);
                                        out.println("PERMISSION " + nodeID + " " + Arrays.toString(fidgeClock));
                                        socket2.close();
                                        if (verbosity) {
                                            String msg = ("Node " + nodeID + " sent permission to node " + message[1] + " at " + neighbors.get(clientNodeID).getHostName() + ":" + neighbors.get(clientNodeID).getPort() + "\n");
                                            msg += "Deferred = " + deferred + "\n";
                                            msg += "Sent perms = " + sentPerms + "\n";
                                            msg += "=====================================";
                                            System.out.println(msg);
                                        }

                                        //if in requested, request again
                                        if(requested.contains(clientNodeID)){
                                            //request again
                                            Socket socket3 = new Socket(neighbors.get(clientNodeID).getHostName(), neighbors.get(clientNodeID).getPort());
                                            PrintWriter out2 = new PrintWriter(socket3.getOutputStream(), true);
                                            out2.println("REQUEST " + nodeID + " " + myRequestClock + " " + Arrays.toString(fidgeClock));
                                            socket3.close();
                                            if(verbosity) {
                                                String msg = ("Node " + nodeID + " sent request to node " + message[1] + " at " + neighbors.get(clientNodeID).getHostName() + ":" + neighbors.get(clientNodeID).getPort() + "\n");
                                                msg += "Deferred = " + deferred + "\n";
                                                msg += "Sent perms = " + sentPerms + "\n";
                                                msg += "=====================================";
                                                System.out.println(msg);
                                            }
                                        }

                                        break;
                                    } catch (IOException e) {
                                        e.printStackTrace();

                                    }
                                }
                            }else{
                                if(!deferred.contains(clientNodeID)) {
                                    //add to deferred
                                    deferred.add(clientNodeID);
                                    if (verbosity) {
                                        String msg = ("Node " + nodeID + " deferring node " + message[1] + " at " + neighbors.get(clientNodeID).getHostName() + ":" + neighbors.get(clientNodeID).getPort() + " because my clock is " + myRequestClock + " and theirs is " + clientClock + "\n");
                                        msg += "Deferred = " + deferred + "\n";
                                        msg += "Sent perms = " + sentPerms + "\n";
                                        msg += "=====================================";
                                        System.out.println(msg);
                                    }
                                }
                            }
                            break;
                        case "PERMISSION":
                            //remove from sentPerms
                            sentPerms.remove((Integer) Integer.parseInt(message[1]));
                            //fidge clock = max of fidge clock and incoming fidge clock
                            int[] incomingFidgeClock = new int[numNodes];
                            String[] fidgeClockString = clientMessage.substring(clientMessage.indexOf("[") + 1, clientMessage.indexOf("]")).split(", ");
                            for(int i = 0; i < numNodes; i++){
                                incomingFidgeClock[i] = Integer.parseInt(fidgeClockString[i]);
                            }
                            for(int i = 0; i < numNodes; i++){
                                fidgeClock[i] = Math.max(fidgeClock[i], incomingFidgeClock[i]);
                            }
                            break;
                        case "FINISHED":
                            //remove from sentPerms
                            if(Integer.parseInt(message[1]) != nodeID) {
                                neighbors.get(Integer.parseInt(message[1])).setAlive(false);
                                sentPerms.remove((Integer) Integer.parseInt(message[1]));
                                //adjust clock
                                int[] incomingFidgeClock2 = new int[numNodes];
                                String[] fidgeClockString2 = clientMessage.substring(clientMessage.indexOf("[") + 1, clientMessage.indexOf("]")).split(", ");
                                for (int i = 0; i < numNodes; i++) {
                                    incomingFidgeClock2[i] = Integer.parseInt(fidgeClockString2[i]);
                                }
                                for (int i = 0; i < numNodes; i++) {
                                    fidgeClock[i] = Math.max(fidgeClock[i], incomingFidgeClock2[i]);
                                }
                            }
                            else{
                                finished = true;
                            }
                            //if everyone is finished, finish
                            boolean allFinished = true;
                            for(Neighbor n : neighbors.values()){
                                if(n.isAlive()){
                                    allFinished = false;
                                    break;
                                }
                            }
                            if(!finished)
                                allFinished = false;
                            if(allFinished)
                                alive = false;
                            break;
                        case "ENTER":
                            inCS = true;
                            break;
                        case "EXIT":
                            inCS = false;
                            break;
                        default:
                            break;
                    }
                }
                if(verbosity)
                    System.out.println("Node " + nodeID + " socket closed");
                socket.close();
            }catch (FileNotFoundException | UnknownHostException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("IOException in MutualExclusion.run() - socket timed out or failed to bind (probably failed to bind)");

            }
            //if(verbosity)
                System.out.println("Node " + nodeID + " finished listening");
        });
        listener.start();
    }

    /**
     * Enter the critical section. Request permission from all nodes for which we do not have permission.
     */
    public long csEnter() {
        if(verbosity)
            System.out.println("Node " + nodeID + " sent perms = " + sentPerms);
        myRequestClock = fidgeClock[nodeID];
        requested = new HashSet<>();
        //make a copy of sentPerms to iterate over
        HashSet<Integer> tempSentPerms = new HashSet<>(sentPerms);
        long startTime = System.currentTimeMillis();
        for(int i : tempSentPerms){
            try {
                Neighbor n = neighbors.get(i);
                if(verbosity) {
                    String msg = ("Node " + nodeID + " requesting permission from node " + n.getId() + " at " + n.getHostName() + ":" + n.getPort() + "\n");
                    msg += "Deferred = " + deferred + "\n";
                    msg += "Sent perms = " + sentPerms + "\n";
                    msg += "Requested = " + requested + "\n";
                    msg += "=====================================";
                    System.out.println(msg);
                }
                Socket socket = new Socket(n.getHostName(), n.getPort());
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                out.println("REQUEST " + nodeID + " " + myRequestClock + " " + Arrays.toString(fidgeClock));
                messages++;
                socket.close();
                requested.add(i);
            } catch (IOException e) {
                System.out.println("Node " + nodeID + " failed to send request to node " + i);
                e.printStackTrace();
            }
        }
        //wait until we have all perms
        while(!sentPerms.isEmpty()){
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            //check if there is any in sentPerms that are not in requested
            HashSet<Integer> temp = new HashSet<>(sentPerms);
            //Iterator<Integer> iterator = sentPerms.iterator();
            temp.removeAll(requested);
            if(!temp.isEmpty()){
                //request them
                for(int i : temp){
                    try {
                        Neighbor n = neighbors.get(i);
                        if (verbosity){
                            String msg = ("Node " + nodeID + " requesting permission(2) from node " + n.getId() + " at " + n.getHostName() + ":" + n.getPort() + "\n");
                            msg += "Deferred = " + deferred + "\n";
                            msg += "Sent perms = " + sentPerms + "\n";
                            msg += "Requested = " + requested + "\n";
                            msg += "=====================================";
                            System.out.println(msg);
                        }
                        Socket socket = new Socket(n.getHostName(), n.getPort());
                        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                        out.println("REQUEST " + nodeID + " " + myRequestClock + " " + Arrays.toString(fidgeClock));
                        messages++;
                        socket.close();
                        requested.add(i);
                    } catch (IOException e) {
                        System.out.println("Node " + nodeID + " failed to send request(2) to node " + i);
                        e.printStackTrace();
                    }
                }
            }
        }
        requested = new HashSet<>();
        long endTime = System.currentTimeMillis();

        return endTime - startTime;
    }

    /**
     * Leave the critical section. Send permission to all nodes in deferred.
     */
    public void csLeave() {
        myRequestClock = -1;
        inCS = false;
        if(verbosity)
            System.out.println("Node " + nodeID + " deferred = " + deferred);
        //send all deferred messages
        Iterator<Integer> iterator = deferred.iterator();
        deferred = new CopyOnWriteArrayList<>();
        int i = -1;
        while(iterator.hasNext()){
            try {
                i = iterator.next();
                Socket socket = new Socket(neighbors.get(i).getHostName(), neighbors.get(i).getPort());
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                out.println("PERMISSION " + nodeID + " " + Arrays.toString(fidgeClock));
                messages++;
                socket.close();
                if(verbosity)
                    System.out.println("Node " + nodeID + " sent permission to node " + i);
                if(!sentPerms.contains(i))
                    sentPerms.add(i);
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("Node " + nodeID + " failed to send permission to node " + i);
            }
        }
    }


    /**
     * Finish the mutual exclusion class. Send a message to all neighbors to tell them we are finished.
     */
    public void finish() {
        if(verbosity)
            System.out.println("Node " + nodeID + " finish() called");
        //send a message to all neighbors to tell them we are finished
        for(Neighbor n : neighbors.values()){
            try {
                Socket socket = new Socket(n.getHostName(), n.getPort());
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                out.println("FINISHED " + nodeID + " " + Arrays.toString(fidgeClock));
                socket.close();
            } catch (IOException e) {
                System.out.println("Node " + nodeID + " failed to send FINISHED to node " + n.getId());
                e.printStackTrace();
            }
        }
        //send to myself
        try {
            Socket socket = new Socket(InetAddress.getLocalHost().getHostAddress(), port);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            out.println("FINISHED " + nodeID);
            socket.close();
        } catch (IOException e) {
            System.out.println("Node " + nodeID + " failed to send FINISHED to self");
            e.printStackTrace();
        }
    }
}
