import java.util.Scanner;

public class PiCalculator {

    // Private variable - accessible only within this class
    private double piValue;
    private int terms;

    // Public constant - accessible from anywhere
    public static final String SERIES_NAME = "Leibniz Series (4/1 - 4/3 + 4/5 - 4/7 + 4/9 ...)";

    // Constructor - initializes private variables
    public PiCalculator(int terms) {
        this.terms = terms;
        this.piValue = computePi(terms);
    }

    // Private method - core calculation logic hidden from outside
    private double computePi(int terms) {
        double pi = 0.0;
        int sign = 1;

        for (int i = 0; i < terms; i++) {
            pi += sign * (4.0 / (2 * i + 1));
            sign *= -1;
        }
        return pi;
    }

    // Public method - accessible from anywhere, including main
    public void displayResult() {
        System.out.println("Public Method - Displaying Result:");
        System.out.println("Approximated value of Pi: " + piValue);
        System.out.println();
    }

    // Protected method - accessible within same package/subclasses
    protected void displayPrecisionInfo() {
        System.out.println("Protected Method - Displaying Precision Info:");
        System.out.println("Precision used: " + terms + " terms");
        System.out.println("Series used: " + SERIES_NA
