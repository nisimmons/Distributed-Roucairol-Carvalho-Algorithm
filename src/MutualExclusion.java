import java.util.ArrayList;

public class MutualExclusion {
    private int nodeID;
    private String ip;
    private int port;
    private String projectDir;
    private ArrayList<Integer> keys;
    public MutualExclusion(int nodeID, String ip, int port, String projectDir){
        this.nodeID = nodeID;
        this.ip = ip;
        this.port = port;
    }
    public void csEnter() {

    }

    public void csLeave() {

    }
}
