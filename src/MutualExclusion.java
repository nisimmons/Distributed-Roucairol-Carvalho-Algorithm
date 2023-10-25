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
        //for every node in sentPerms, request permission
        for(int i : sentPerms){
            try {
                Socket socket = new Socket(hostName, port);
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out.println("REQUEST " + nodeID + Arrays.toString(fidgeClock));
                String serverMessage;
                while ((serverMessage = in.readLine()) != null) {
                    if(serverMessage.equals("PERMISSION " + nodeID)){
                        //add the node to the sentPerms set
                        sentPerms.add(i);
                        break;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void csLeave() {

    }


    @Override
    public void run() {
        try {
            InetAddress address = InetAddress.getByName(hostName);
            InetSocketAddress socketAddress = new InetSocketAddress(address, port);
            ServerSocket socket = new ServerSocket();
            System.out.print("Attempting to bind socket to " + hostName + ":" + port + "...");
            socket.bind(socketAddress);
            System.out.println("Success!");


            //listen for messages
            while (alive) {
                Socket clientSocket = socket.accept();
                PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                String clientMessage;
                while ((clientMessage = in.readLine()) != null && alive) {
                    System.out.println("Recieved message \"" + clientMessage + "\" from client " + clientSocket.getInetAddress().getHostName());


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
