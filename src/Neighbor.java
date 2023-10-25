/**
 * Nathaniel Simmons 10/10/2023
 * Neighbor class to store the information of each node
 */
public class Neighbor {
    private int id;
    private String hostName;
    private int port;
    private boolean red;

    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }
    public String getHostName() {
        return hostName;
    }
    public void setHostName(String hostName) {
        this.hostName = hostName;
    }
    public int getPort() {
        return port;
    }
    public void setPort(int port) {
        this.port = port;
    }
    public boolean isRed() {return red;}
    public void setRed(boolean red) {this.red = red;}
}
