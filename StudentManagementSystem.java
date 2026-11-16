import java.io.*;
import java.nio.file.*;
import java.util.*;

public class StudentManagementSystem {
    private static final String FILE_NAME = "students.txt";
    private final List<Student> students = new ArrayList<>();
    private final Map<Integer, Student> studentsById = new HashMap<>();
    private final Set<String> emails = new HashSet<>();
    private final Scanner sc = new Scanner(System.in);

    public static void main(String[] args) {
        StudentManagementSystem app = new StudentManagementSystem();
        app.loadStudents();
        app.run();
    }

    private void run() {
        boolean running = true;
        System.out.println("=== STUDENT MANAGEMENT SYSTEM ===");
        while (running) {
            System.out.println("\n1. Add Student\n2. View Students\n3. Search Student");
            System.out.println("4. Update Student\n5. Delete Student\n6. Save and Exit");
            int choice = readInt("Enter choice: ");
            try {
                switch (choice) {
                    case 1 -> addStudent();
                    case 2 -> viewStudents();
                    case 3 -> searchStudent();
                    case 4 -> updateStudent();
                    case 5 -> deleteStudent();
                    case 6 -> { saveStudents(); running = false; System.out.println("Saved. Goodbye!"); }
                    default -> System.out.println("Choose 1 to 6.");
                }
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }

    // Method overloading
    private void showMessage(String message) { System.out.println(message); }
    private void showMessage(String heading, String message) {
        System.out.println(heading + ": " + message);
    }

    private void addStudent() throws IOException {
        int id = readInt("Student ID: ");
        String name = readText("Name: ");
        String email = readText("Email: ").toLowerCase();
        double marks = readDouble("Marks (0-100): ");
        validate(id, name, email, marks);

        if (studentsById.containsKey(id)) throw new IllegalArgumentException("ID already exists.");
        if (emails.contains(email)) throw new IllegalArgumentException("Email already exists.");

        Student s = new Student(id, name, email, marks);
        students.add(s);
        studentsById.put(id, s);
        emails.add(email);
        saveStudents();
        showMessage("Success", "Student added.");
    }

    private void viewStudents() {
        if (students.isEmpty()) { showMessage("No student records."); return; }
        System.out.println("\nID\tName\tEmail\t\tMarks");
        for (Student s : students) System.out.println(s);
    }

    private void searchStudent() throws StudentNotFoundException {
        int id = readInt("Enter ID: ");
        showMessage("Student Found", findStudent(id).toString());
    }

    private void updateStudent() throws IOException, StudentNotFoundException {
        int id = readInt("Enter ID to update: ");
        Student s = findStudent(id);
        String name = readText("New name (blank to keep): ");
        String email = readText("New email (blank to keep): ").toLowerCase();
        String marksText = readText("New marks (blank to keep): ");

        String updatedName = name.isBlank() ? s.name : name;
        String updatedEmail = email.isBlank() ? s.email : email;
        double updatedMarks = marksText.isBlank() ? s.marks : parseDouble(marksText);
        validate(id, updatedName, updatedEmail, updatedMarks);

        Student owner = findByEmail(updatedEmail);
        if (owner != null && owner.id != id)
            throw new IllegalArgumentException("Email belongs to another student.");

        emails.remove(s.email);
        s.name = updatedName;
        s.email = updatedEmail;
        s.marks = updatedMarks;
        emails.add(updatedEmail);
        saveStudents();
        showMessage("Success", "Student updated.");
    }

    private void deleteStudent() throws IOException, StudentNotFoundException {
        int id = readInt("Enter ID to delete: ");
        Student s = findStudent(id);
        students.remove(s);
        studentsById.remove(id);
        emails.remove(s.email);
        saveStudents();
        showMessage("Success", "Student deleted.");
    }

    private Student findStudent(int id) throws StudentNotFoundException {
        Student s = studentsById.get(id);
        if (s == null) throw new StudentNotFoundException("Student ID " + id + " not found.");
        return s;
    }

    private Student findByEmail(String email) {
        for (Student s : students)
            if (s.email.equalsIgnoreCase(email)) return s;
        return null;
    }

    // throw raises an exception; this method validates input.
    private void validate(int id, String name, String email, double marks) {
        if (id <= 0) throw new IllegalArgumentException("ID must be positive.");
        if (name.isBlank()) throw new IllegalArgumentException("Name cannot be empty.");
        if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$"))
            throw new IllegalArgumentException("Invalid email.");
        if (marks < 0 || marks > 100)
            throw new IllegalArgumentException("Marks must be between 0 and 100.");
    }

    private int readInt(String prompt) {
        while (true) {
            try { System.out.print(prompt); return Integer.parseInt(sc.nextLine().trim()); }
            catch (NumberFormatException e) { System.out.println("Enter a valid integer."); }
        }
    }

    private double readDouble(String prompt) {
        while (true) {
            try { System.out.print(prompt); return Double.parseDouble(sc.nextLine().trim()); }
            catch (NumberFormatException e) { System.out.println("Enter a valid number."); }
        }
    }

    private double parseDouble(String value) {
        try { return Double.parseDouble(value); }
        catch (NumberFormatException e) { throw new IllegalArgumentException("Invalid marks."); }
    }

    private String readText(String prompt) {
        System.out.print(prompt);
        return sc.nextLine().trim();
    }

    // File reading with try-with-resources
    private void loadStudents() {
        Path path = Paths.get(FILE_NAME);
        if (!Files.exists(path)) return;
        try (BufferedReader reader = Files.newBufferedReader(path)) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] p = line.split("\\|", -1);
                if (p.length != 4) continue;
                try {
                    int id = Integer.parseInt(p[0]);
                    String name = p[1], email = p[2];
                    double marks = Double.parseDouble(p[3]);
                    validate(id, name, email, marks);
                    if (!studentsById.containsKey(id) && !emails.contains(email)) {
                        Student s = new Student(id, name, email, marks);
                        students.add(s); studentsById.put(id, s); emails.add(email);
                    }
                } catch (RuntimeException ignored) {
                    System.out.println("Skipped an invalid saved record.");
                }
            }
        } catch (IOException e) {
            System.out.println("Could not read file: " + e.getMessage());
        }
    }

    // throws declares that this method may pass IOException to its caller.
    private void saveStudents() throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(FILE_NAME))) {
            for (Student s : students) {
                writer.write(s.id + "|" + s.name + "|" + s.email + "|" + s.marks);
                writer.newLine();
            }
        }
    }

    private static class Student {
        int id; String name, email; double marks;
        Student(int id, String name, String email, double marks) {
            this.id = id; this.name = name; this.email = email; this.marks = marks;
        }
        public String toString() {
            return id + "\t" + name + "\t" + email + "\t" + marks;
        }
    }

    private static class StudentNotFoundException extends Exception {
        StudentNotFoundException(String message) { super(message); }
    }
}
