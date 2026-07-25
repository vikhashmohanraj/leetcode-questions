
import java.util.*;
import java.util.stream.*;

public class EmployeeAnalytics {

    static class Employee {
        int id;
        String name;
        String department;
        double salary;

        public Employee(int id, String name, String department, double salary) {
            this.id = id;
            this.name = name;
            this.department = department;
            this.salary = salary;
        }

        public int getId() { return id; }
        public String getName() { return name; }
        public String getDepartment() { return department; }
        public double getSalary() { return salary; }

        @Override
        public String toString() {
            return id + "\t" + name + "\t" + department + "\t" + salary;
        }
    }

    public static void main(String[] args) {
        List<Employee> employees = Arrays.asList(
                new Employee(101, "Rahul", "CSE", 55000.0),
                new Employee(102, "Sneha", "ECE", 62000.0),
                new Employee(103, "Kiran", "CSE", 48000.0),
                new Employee(104, "Divya", "MECH", 51000.0),
                new Employee(105, "Arjun", "ECE", 70000.0)
        );

        System.out.println("---- All Employees ----");
        employees.forEach(System.out::println);

        System.out.println("\n---- Salary Above 50000 (High to Low) ----");
