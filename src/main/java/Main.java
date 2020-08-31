import java.util.Scanner;

/**
 * Demo class for the usage of this project.
 */
public class Main {

    /**
     * Scanner for reading input data from console.
     */
    private static Scanner scanner = new Scanner(System.in);

    /**
     * Main method for complete program. Run to have a simulation demo.
     * @param args Arguments as String array, that should contain the processCount and the duration. The arguments are requested by using the scanner.
     */
    public static void main(String[] args) {

        // read processCount
        System.out.print("Set number of process: ");
        int processCount = scanner.nextInt();
        // alternative: use count of available processors
        // processCount = Runtime.getRuntime().availableProcessors();

        // read duration
        System.out.print("Set duration for network in milliseconds: ");
        int duration = scanner.nextInt();

        // create network and start run method
        System.out.println("Simulate " + processCount + " processes for " + duration + " milliseconds.");
        new Network(processCount, duration).run();
    }
}