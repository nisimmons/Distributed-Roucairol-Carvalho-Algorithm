import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws FileNotFoundException {
        readFile("C:\\Users\\nsimm\\Downloads\\CS6378Project2Part1\\src");
    }
    public static void readFile(String filename) throws FileNotFoundException {
        //read the config file
        Scanner scanner = new Scanner(new File(filename + "\\config.txt"));
        String s;
        ArrayList<String> validLines = new ArrayList<>();
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
