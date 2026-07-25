import java.io.*;
import java.util.Scanner;

public class StudentFile {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        String[] rollNo = new String[3];
        String[] name = new String[3];
        int[] marks = new int[3];

        String filePath = "/tmp/students.txt";

        System.out.println("Enter Details of 3 Students");

        // Writing student details to file
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(filePath))) {

            for (int i = 0; i < 3; i++) {
                System.out.println("\nStudent " + (i + 1));

                System.out.print("Roll No : ");
                rollNo[i] = sc.nextLine();

                System.out.print("Name : ");
                name[i] = sc.nextLine();

                System.out.print("Marks : ");
                marks[i] = Integer.parseInt(sc.nextLine());

                bw.write(rollNo[i] + "," + name[i] + "," + marks[i]);
                bw.newLine();
            }

            System.out.println("\nStudent records saved successfully.");

        } catch (IOException e) {
            System.out.println("Error writing file: " + e.getMessage());
            return;
        }

        // Reading and displaying all records
        try (BufferedReader br = new BufferedReader(new FileReader(fileP
