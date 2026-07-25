 
import java.util.*;

// ---------- Student Model ----------
class Student {
    private int id;
    private String name;
    private int age;
    private String course;
    private double marks;

    public Student(int id, String name, int age, String course, double marks) {
        this.id = id;
        this.name = name;
        this.age = age;
        this.course = course;
        this.marks = marks;
    }

    // Getters and Setters
    public int getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }
    public String getCourse() { return course; }
    public void setCourse(String course) { this.course = course; }
    public double getMarks() { return marks; }
    public void setMarks(double marks) { this.marks = marks; }

    @Override
    public String toString() {
        return String.format("ID: %-5d Name: %-15s Age: %-4d Course: %-10s Marks: %.2f",
                id, name, age, course, marks);
    }
}

// ---------- Management System ----------
class StudentRecordManagementSystem {
    private ArrayList<Student> studentList;        // ordered storage
    private HashMap<Integer, Integer> idIndexMap;  // id -> index in ArrayList (fast lookup)
