import java.io.*;
import java.net.*;
import java.util.Arrays;
import java.util.HashSet;

public class MutualExclusion implements Runnable {
    private int nodeID;
    private String hostName;
    private int port;
    private int numNodes;
    private String projectDir;
    private HashSet<Integer> sentPerms;
    private HashSet<Integer> deferred;
    private int[] myRequestClock;

    private boolean alive;
    public MutualExclusion(int nodeID, String hostName, int port, String projectDir, int numNodes){
        this.nodeID = nodeID;
        this.hostName = hostName;
        this.port = port;
        this.projectDir = projectDir;
        this.numNodes = numNodes;
        alive = true;
        deferred = new HashSet<>();
        sentPerms = new HashSet<>();
        //add every node with an ID less than this one to the sentPerms set
        for(int i = 0; i < nodeID; i++){
            sentPerms.add(i);
        }
    }
    public void csEnter(int[] fidgeClock) {
        myRequestClock = fidgeClock;
        //for every node in sentPerms, request permission
        for(int i : sentPerms){
            try {
                Socket socket = new Socket(hostName, port);
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out.println("REQUEST " + nodeID + Arrays.toString(fidgeClock));
                String response;
                while((response = in.readLine()) != null){
                    if(response.equals("PERMISSION " + nodeID)){
                        //add to sentPerms
                        sentPerms.remove(i);
                        break;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        //wait until we have all perms
        while(sentPerms.isEmpty()){
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void csLeave() {
        //send all deferred messages
        for(int i : deferred){
            try {
                Socket socket = new Socket(hostName, port);
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                out.println("PERMISSION " + nodeID);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    @Override
    public void run() {
        try {
            InetAddress address = InetAddress.getByName(hostName);
            InetSocketAddress socketAddress = new InetSocketAddress(address, port);
            ServerSocket socket = new ServerSocket();
            socket.bind(socketAddress);

            //listen for messages
            while (alive) {
                Socket clientSocket = socket.accept();
                PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                String clientMessage;
                while ((clientMessage = in.readLine()) != null && alive) {
                    System.out.println("Recieved message \"" + clientMessage + "\" from client " + clientSocket.getInetAddress().getHostName());
                    //reply yes if we have no request open or my timestamp is higher than incoming, else add to deferred
                    String[] message = clientMessage.split(" ");
                    switch(message[0]){
                        case "REQUEST":
                            int[] incomingClock = new int[numNodes];
                            for(int i = 0; i < numNodes; i++){
                                incomingClock[i] = Integer.parseInt(message[1].substring(i+1, i+2));
                            }
                            if(myRequestClock == null || Arrays.compare(incomingClock, myRequestClock) < 0){
                                //reply yes
                                out.println("PERMISSION " + nodeID);
                            }else{
                                //add to deferred
                                deferred.add(Integer.parseInt(message[1].substring(0, 1)));
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
        }
    }
}
