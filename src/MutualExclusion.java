import java.io.*;
import java.lang.reflect.Array;
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

public class MutualExclusion {
    private final boolean verbosity = false;
    private int nodeID;
    private String hostName;
    private int port;
    private int numNodes;
    private String projectDir;
    private HashSet<Integer> sentPerms;
    private HashSet<Integer> deferred;
    private int myRequestClock;
    HashMap<Integer, Neighbor> neighbors;
    private boolean inCS;
    public int[] fidgeClock;
    private boolean finished;

    private Thread listener;
    private boolean alive;
    public MutualExclusion(int nodeID, String hostName, int port, String projectDir, int numNodes, ArrayList<Neighbor> neighborArrayList) {
        this.nodeID = nodeID;
        this.hostName = hostName;
        this.port = port;
        this.projectDir = projectDir;
        this.numNodes = numNodes;
        alive = true;
        deferred = new HashSet<>();
        sentPerms = new HashSet<>();
        //fill neighbors
        this.neighbors = new HashMap<>();
        for(Neighbor n : neighborArrayList){
            this.neighbors.put(n.getId(), n);
            if(n.getId() < nodeID)
                sentPerms.add(n.getId());
        }
        myRequestClock = -1;
        inCS = false;

        listener = new Thread(() -> {
            try {
            /*InetAddress address = InetAddress.getByName(hostName);
            InetSocketAddress socketAddress = new InetSocketAddress(address, port);
            ServerSocket socket = new ServerSocket();
            socket.bind(socketAddress);
            System.out.println("Node " + nodeID + " listening on " + InetAddress.getLocalHost().getHostAddress() + ":" + port);*/
                ServerSocket socket = new ServerSocket(port);
                if(verbosity)
                    System.out.println("Node " + nodeID + " listening on " + InetAddress.getLocalHost().getHostAddress() + ":" + port);
                //listen for messages
                while (alive) {
                    Socket clientSocket = socket.accept();
                    if(verbosity)
                        System.out.println("Node " + nodeID + " accepted connection");
                    //System.out.println("Node " + nodeID + " accepted connection from " + clientSocket.getPort());
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
                            if((myRequestClock == -1 || Integer.parseInt(message[2]) < myRequestClock) && !inCS){
                                //reply yes
                                sentPerms.add(Integer.parseInt(message[1]));
                                try {
                                    if(verbosity)
                                        System.out.println("Node " + nodeID + " sending permission to node " + message[1] + " at " + neighbors.get(Integer.parseInt(message[1])).getHostName() + ":" + neighbors.get(Integer.parseInt(message[1])).getPort());
                                    Socket socket2 = new Socket(InetAddress.getLocalHost().getHostAddress(), neighbors.get(Integer.parseInt(message[1])).getPort());
                                    PrintWriter out = new PrintWriter(socket2.getOutputStream(), true);
                                    out.println("PERMISSION " + nodeID + " " + Arrays.toString(fidgeClock));
                                    socket2.close();
                                    if(verbosity)
                                        System.out.println("Node " + nodeID + " sent permission to node " + message[1]);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }else{
                                //add to deferred
                                if(verbosity)
                                    System.out.println("Node " + nodeID + " deferred node " + message[1]);
                                deferred.add(Integer.parseInt(message[1]));
                            }
                            break;
                        case "PERMISSION":
                            //remove from sentPerms
                            sentPerms.remove(Integer.parseInt(message[1]));
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
                                sentPerms.remove(Integer.parseInt(message[1]));
                            }
                            else{
                                finished = true;
                            }
                            //if everyone is finished, unalive
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
                    }

                    /*//if all neighbors are dead, finish
                    boolean allDead = true;
                    for(Neighbor n : neighbors.values()){
                        if(n.isAlive()){
                            allDead = false;
                            break;
                        }
                    }
                    if(allDead)
                        alive = false;*/
                }
                if(verbosity)
                    System.out.println("Node " + nodeID + " socket closed");
                socket.close();
            }catch (FileNotFoundException | UnknownHostException e) {
                e.printStackTrace();
            } catch (IOException e) {
                //socket timed out or failed to bind (probably failed to bind)
                e.printStackTrace();
                System.out.println("IOException in MutualExclusion.run() - socket timed out or failed to bind (probably failed to bind)");

            }
            if(verbosity)
                System.out.println("Node " + nodeID + " finished listening");
        });
        listener.start();
    }

    /**
     * Enter the critical section. Request permission from all nodes for which we do not have permission.
     */
    public void csEnter() {
        if(verbosity)
            System.out.println("Node " + nodeID + " sent perms = " + sentPerms);
        myRequestClock = fidgeClock[nodeID];
        HashSet<Integer> requested = new HashSet<>();
        //make a copy of sentPerms to iterate over
        HashSet<Integer> tempSentPerms = new HashSet<>(sentPerms);
        for(int i : tempSentPerms){
            try {
                Neighbor n = neighbors.get(i);
                if(verbosity)
                    System.out.println("Node " + nodeID + " requesting permission from node " + n.getId() + " at " + n.getHostName() + ":" + n.getPort());
                Socket socket = new Socket(InetAddress.getLocalHost().getHostAddress(), n.getPort());
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                out.println("REQUEST " + nodeID + " " + myRequestClock + " " + Arrays.toString(fidgeClock));
                socket.close();
                if(verbosity)
                    System.out.println("Node " + nodeID + " sent request to node " + i);
                requested.add(i);
            } catch (IOException e) {
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
            temp.removeAll(requested);
            if(!temp.isEmpty()){
                //request them
                for(int i : temp){
                    try {
                        Neighbor n = neighbors.get(i);
                        if(verbosity)
                            System.out.println("Node " + nodeID + " requesting permission from node " + n.getId() + " at " + n.getHostName() + ":" + n.getPort());
                        Socket socket = new Socket(InetAddress.getLocalHost().getHostAddress(), n.getPort());
                        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                        out.println("REQUEST " + nodeID + " " + myRequestClock);
                        socket.close();
                        if(verbosity)
                            System.out.println("Node " + nodeID + " sent request to node " + i);
                        requested.add(i);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        //message self to that we are entering cs
        /*try {
            Socket socket = new Socket(hostName, port);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            out.println("ENTER");
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }*/
    }

    public void csLeave() {
        myRequestClock = -1;
        inCS = false;
        if(verbosity)
            System.out.println("Node " + nodeID + " deferred = " + deferred);
        //message self that we have exited
        /*try {
            Socket socket = new Socket(hostName, port);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            out.println("EXIT");
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }*/
        //send all deferred messages
        for(int i : deferred){
            try {
                Socket socket = new Socket(InetAddress.getLocalHost().getHostAddress(), neighbors.get(i).getPort());
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                out.println("PERMISSION " + nodeID + " " + Arrays.toString(fidgeClock));
                socket.close();
                if(verbosity)
                    System.out.println("Node " + nodeID + " sent permission to node " + i);
                sentPerms.add(i);
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("Node " + nodeID + " failed to send permission to node " + i);
            }
        }
        deferred.clear();
    }


    public void finish() {
        if(verbosity)
            System.out.println("Node " + nodeID + " finish() called");
        //send a message to all neighbors to tell them we are finished
        for(Neighbor n : neighbors.values()){
            try {
                Socket socket = new Socket(n.getHostName(), n.getPort());
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                out.println("FINISHED " + nodeID);
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
