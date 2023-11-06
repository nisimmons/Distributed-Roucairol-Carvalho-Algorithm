import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class Evaluator {
    public static void main(String[] args) throws IOException {
        //read the config file to find num nodes, inter-request delay, cs-exec time, requests per node
        Scanner scanner = new Scanner(new File("src/config.txt"));
        String s;
        ArrayList<String> validLines = new ArrayList<>();
        Scanner line;

        while(scanner.hasNext()) {
            s = scanner.nextLine();
            s = s.replaceAll("#.*", "");    //remove comments
            s = s.trim();                                   //remove leading and trailing whitespace
            if (s.matches("^\\d.*"))              //check if the line starts with an unsigned integer
                validLines.add(s);
        }

        //line 1 is nodes, inter-request delay, cs-exec time, requests per node
        line = new Scanner(validLines.get(0));
        int numNodes = line.nextInt();
        int interRequestDelay = line.nextInt();
        int csExecTime = line.nextInt();
        int requestsPerNode = line.nextInt();

        for(int i = 0; i < numNodes; i++) {
            if(!verifyFile("src/output"+i+".txt", numNodes)) {
                throw new RuntimeException("Invalid file for node " + i);
            }
        }

        //create a list of objects with one int and one array



        long totalResponseTime = 0;
        long totalMessages = 0;
        long totalTime = 0;

        //fill list with entries from every output file
        ArrayList<specialObject> temp = new ArrayList<>();
        for(int i = 0; i < numNodes; i++) {
            Scanner fileScanner = new Scanner(new File("src/output"+i+".txt"));
            while(fileScanner.hasNext()) {
                String l = fileScanner.nextLine();
                if(l.contains("RESPONSETIME")) {
                    totalResponseTime += Long.parseLong(l.substring(13));
                    continue;
                }
                else if(l.contains("TOTALTIME")) {
                    totalTime = Math.max(totalTime, Long.parseLong(l.substring(10)));
                    continue;
                }
                else if(l.contains("MESSAGES")) {
                    totalMessages += Long.parseLong(l.substring(9));
                    continue;
                }
                specialObject obj = new specialObject();
                obj.nodeID = Integer.parseInt(l.substring(0, l.indexOf(" ")));
                obj.fidgeClock = new int[numNodes];
                String[] clock = l.substring(l.indexOf("[")+1, l.indexOf("]")).split(", ");
                for(int j = 0; j < numNodes; j++) {
                    obj.fidgeClock[j] = Integer.parseInt(clock[j]);
                }
                temp.add(obj);
            }
        }
        long averageResponseTime = totalResponseTime/((long) numNodes *requestsPerNode);
        double throughput = ((double)(requestsPerNode)*numNodes)/(totalTime/1000.0); //requests fulfilled per millisecond
        System.out.println("Total messages: " + totalMessages);
        System.out.println("Average response time: " + averageResponseTime + " milliseconds");
        System.out.println("Throughput: " + throughput + " requests per second");

        System.out.println();
        System.out.println("Other info");
        System.out.println("Total time: " + totalTime/1000 + " seconds");
        System.out.println("Requests satisfied: " + requestsPerNode*numNodes);
        System.out.println("Average messages per node: " + totalMessages/numNodes);


        //sort temp using comparator
        temp.sort(new specialComparator());

        FileWriter fileWriter = new FileWriter("outputFull.txt");
        for(int i = 0; i < temp.size(); i++) {
            fileWriter.write("Node " + temp.get(i).nodeID + " " + Arrays.toString(temp.get(i).fidgeClock) + "\n");
        }
        fileWriter.close();


        //check that every entry is followed by the same node exiting
        for(int i = 0; i < temp.size()/2; i+=2) {
            if(temp.get(i).nodeID != temp.get(i+1).nodeID) {
                System.out.println("Node " + temp.get(i).nodeID + " entered with clock " + Arrays.toString(temp.get(i).fidgeClock));
                System.out.println("Node " + temp.get(i+1).nodeID + " exited with clock " + Arrays.toString(temp.get(i+1).fidgeClock));
                throw new RuntimeException("Invalid entry/exit");
            }
        }
        System.out.println("All entries and exits are valid");
    }


    public static boolean verifyFile(String fileName, int numNodes) throws FileNotFoundException {
        Scanner scanner = new Scanner(new File(fileName));
        String s;
        s = scanner.nextLine();
        int[] clock1 = null;
        int[] clock2 = null;
        String[] clockString;
        while(scanner.hasNext()){
            s = scanner.nextLine();

            if(s.contains("RESPONSETIME") || s.contains("TOTALTIME") || s.contains("MESSAGES")) {
                continue;
            }
            if(clock1 == null) {
                clockString = s.substring(s.indexOf("[") + 1, s.indexOf("]")).split(", ");
                clock1 = new int[numNodes];
                for (int i = 0; i < numNodes; i++) {
                    clock1[i] = Integer.parseInt(clockString[i]);
                }
                continue;
            }
            clockString = s.substring(s.indexOf("[")+1, s.indexOf("]")).split(", ");
            clock2 = new int[numNodes];
            for(int i = 0; i < numNodes; i++) {
                clock2[i] = Integer.parseInt(clockString[i]);
            }

            if(!compareClocks(clock1, clock2)) {
                System.out.println("Clock 1: " + Arrays.toString(clock1));
                System.out.println("Clock 2: " + Arrays.toString(clock2));
                return false;
            }
            clock1 = clock2;
        }
        return true;
    }

    private static boolean compareClocks(int[] clock1, int[] clock2) {
        //check if clock 1 is never greater than, and not equal to clock 2
        boolean equals = true;
        for(int i = 0; i < clock1.length; i++) {
            if(clock1[i] > clock2[i]) {
                return false;
            }
            if(clock1[i] < clock2[i]) {
                equals = false;
            }
        }
        return !equals;
    }


    static class specialObject {
        int nodeID;
        int[] fidgeClock;
    }
    //Make a comparator to compare the arrays
    static class specialComparator implements Comparator<specialObject> {
        public int compare(specialObject aa, specialObject bb) {
            int[] a = aa.fidgeClock;
            int[] b = bb.fidgeClock;
            //check if each value in a is <= b and one value is < b or vice versa
            boolean lessThan = false;
            boolean greaterThan = false;
            for(int i = 0; i < a.length; i++) {
                if(a[i] < b[i]) {
                    lessThan = true;
                }
                if(a[i] > b[i]) {
                    greaterThan = true;
                }
            }
            if(lessThan && greaterThan) {
                //throw exception
                System.out.println("Invalid clock");
                System.out.println("a: " + Arrays.toString(a));
                System.out.println("b: " + Arrays.toString(b));
                throw new RuntimeException("Invalid clock");
            }
            else if(lessThan) {
                return -1;
            }
            else {
                return 1;
            }
        }
    }
}