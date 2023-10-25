import java.io.File;
import java.io.FileNotFoundException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws FileNotFoundException {
        readFile("X:\\School items\\Fall 20\\CS2336\\Code\\CS6378Project2Part1\\src");
    }
    public static void readFile(String filename) throws FileNotFoundException {
        //read the config file
        Scanner scanner = new Scanner(new File(filename + "\\config.txt"));
        String s;
        ArrayList<String> validLines = new ArrayList<String>();
        Scanner line;
        //read until we get a valid line
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
        int numNodes = line.nextInt();
        int interRequestDelay = line.nextInt();
        int csExecTime = line.nextInt();
        int requestsPerNode = line.nextInt();

        //for each line after that, call Project2 with arg[0] as the first int
        for(int i = 1; i < validLines.size(); i++){
            line = new Scanner(validLines.get(i));
            String nodeID = line.next();
            //make a new thread to run this
            Thread t = new Thread(() -> Project2Node.main(new String[]{filename, String.valueOf(nodeID)}));
            t.start();
        }

    }
}
