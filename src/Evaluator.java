import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Scanner;

public class Evaluator {
    public static void main(String[] args) throws IOException {
        String projectDir = "X:\\Code\\Intellij\\CS6378Project2Part1\\src";
        //read the config file to find num nodes, inter-request delay, cs-exec time, requests per node
        Scanner scanner = new Scanner(new File(projectDir+"\\config.txt"));
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
        int numNodes = line.nextInt();
        int interRequestDelay = line.nextInt();
        int csExecTime = line.nextInt();
        int requestsPerNode = line.nextInt();

        //create a list of objects with one int and one array
        class specialObject {
            int nodeID;
            int[] fidgeClock;
        }


        //Make a comparator to compare the arrays
        class specialComparator {
            public boolean compare(int[] a, int[] b) {
                //check if each value in a is <= b and one value is < b or vice versa
                boolean lessThan = false;
                boolean greaterThan = false;
                for(int i = 0; i < a.length; i++) {
                    if(a[i] <= b[i]) {
                        lessThan = true;
                    }
                    if(a[i] < b[i]) {
                        greaterThan = true;
                    }
                }
                return lessThan && greaterThan;
            }
        }
        specialComparator comparator = new specialComparator();
        //fill list with entries from every output file
        ArrayList<specialObject> temp = new ArrayList<>();
        for(int i = 0; i < numNodes; i++) {
            Scanner fileScanner = new Scanner(new File(projectDir+"\\output"+i+".txt"));
            while(fileScanner.hasNext()) {
                specialObject obj = new specialObject();
                String l = fileScanner.nextLine();
                obj.nodeID = Integer.parseInt(l.substring(0, l.indexOf(" ")));
                obj.fidgeClock = new int[numNodes];
                String[] clock = l.substring(l.indexOf("[")+1, l.indexOf("]")).split(", ");
                for(int j = 0; j < numNodes; j++) {
                    obj.fidgeClock[j] = Integer.parseInt(clock[j]);
                }
                temp.add(obj);
            }
        }

        //sort the list by the array

        temp.sort((o1, o2) -> {
            for(int i = 0; i < o1.fidgeClock.length; i++) {
                if(o1.fidgeClock[i] < o2.fidgeClock[i]) {
                    return -1;
                }
                else if(o1.fidgeClock[i] > o2.fidgeClock[i]) {
                    return 1;
                }
            }
            return 0;
        });
        FileWriter fileWriter = new FileWriter(projectDir + "\\outputFull.txt", true);
        for(int i = 0; i < temp.size(); i++) {
            fileWriter.write("Node " + temp.get(i).nodeID + " " + Arrays.toString(temp.get(i).fidgeClock) + "\n");
        }
        fileWriter.close();

        //check that every entry is followed by the same node exiting
        for(int i = 0; i < temp.size(); i++) {
            if(i == temp.size()-1) {
                if(temp.get(i).nodeID != temp.get(0).nodeID) {
                    System.out.println("Node " + temp.get(i).nodeID + " did not exit");
                }
            }
            else {
                if(temp.get(i).nodeID != temp.get(i+1).nodeID) {
                    System.out.println("Node " + temp.get(i).nodeID + " did not exit");
                }
            }
        }

    }
}
