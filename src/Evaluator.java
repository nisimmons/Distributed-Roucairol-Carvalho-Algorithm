import java.io.File;
import java.io.FileNotFoundException;
import java.time.LocalTime;
import java.util.Scanner;

public class Evaluator {
    public static void main(String[] args) throws FileNotFoundException {
        //read the output file
        Scanner scanner = new Scanner(new File("C:\\Users\\nsimm\\Downloads\\CS6378Project2Part1\\src\\output.txt"));
        String s;
        s = scanner.nextLine();
        LocalTime previous = LocalTime.parse(s.split(" ")[1]);
        int counter = 1;
        LocalTime current = LocalTime.parse(s.split(" ")[2]);
        if (current.isAfter(previous)) {
            counter++;
        } else {
            System.out.println("Error: " + s);
        }
        while (scanner.hasNext()) {
            //check if the start time is after the previous end time
            s = scanner.nextLine();
            current = LocalTime.parse(s.split(" ")[1]);
            if (current.isAfter(previous)) {
                counter++;
            } else {
                System.out.println("Error: " + s);
            }
            previous = LocalTime.parse(s.split(" ")[2]);
        }
        System.out.println("Number of successful critical sections: " + counter);
    }
}
