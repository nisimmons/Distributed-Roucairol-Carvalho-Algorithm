import java.io.File;
import java.io.FileNotFoundException;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Scanner;

public class Evaluator {
    public static void main(String[] args) throws FileNotFoundException {
        String projectDir = "X:\\School items\\Fall 20\\CS2336\\Code\\CS6378Project2Part1\\src";
        //read the output file
        Scanner scanner = new Scanner(new File(projectDir + "\\config.txt"));
        String s;
        int counter = 1;
        int numNodes = 0;
        while (scanner.hasNext()) {
            //scan to the first valid line
            do {
                s = scanner.nextLine();
                s = s.replaceAll("#.*", "");    //remove comments
                s = s.trim();                                   //remove leading and trailing whitespace
            } while (!s.matches("^\\d .*"));              //check if the line starts with an unsigned integer
            //just get the first number
            Scanner line = new Scanner(s);
            numNodes = line.nextInt();
        }
        for(int i = 0; i < numNodes; i++){
            scanner = new Scanner(new File(projectDir + "\\output" + i + ".txt"));
            //check here

        }
        System.out.println("Number of successful critical sections: " + counter);
    }
}
