
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
 
/**
 * Task 1: Student Grade Tracker
 * Features: Add/Remove students, calculate avg/highest/lowest,
 *           letter grades, color-coded table, summary report
 */
public class Studentgradetracker extends JFrame {
    static class Student {
        String name;
        ArrayList<Double> grades;

        Student(String name) {
            this.name = name;
            this.grades = new ArrayList<>();
        }

        void addGrade(double grade) { grades.add(grade); }

        double getAverage() {
            if (grades.isEmpty()) return 0;
            double sum = 0;
            for (double g : grades) sum += g;
            return sum / grades.size();
        }

        double getHighest() {
            return grades.stream().mapToDouble(Double::doubleValue).max().orElse(0);
        }

        double getLowest() {
            return grades.stream().mapToDouble(Double::doubleValue).min().orElse(0);
        }

        String getLetterGrade() {
            double avg = getAverage();
            if (avg >= 90) return "A";
            if (avg >= 80) return "B";
            if (avg >= 70) return "C";
            if (avg >= 60) return "D";
            return "F";
        }

        String getStatus() {
            return getAverage() >= 60 ? "Pass" : "Fail";
        }
    }

    
    private final ArrayList<Student> students = new ArrayList<>();
    private DefaultTableModel tableModel;
    private JLabel lblSummary;

    
    public Studentgradetracker() {
        setTitle("Student Grade Tracker");
        setSize(900, 580);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));
        getContentPane().setBackground(new Color(30, 30, 45));

        add(buildHeader(),  BorderLayout.NORTH);
        add(buildTable(),   BorderLayout.CENTER);
        add(buildBottom(),  BorderLayout.SOUTH);

        loadSampleData();
        refreshTable();
        setVisible(true);
    }

    
    private JPanel buildHeader() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 10));
        panel.setBackground(new Color(20, 20, 35));
        panel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        JLabel title = new JLabel("📊 Student Grade Tracker");
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        title.setForeground(new Color(100, 200, 255));
        panel.add(title);

        panel.add(styledButton("➕ Add Student",    new Color(40, 167, 80),  e -> showAddStudentDialog()));
        panel.add(styledButton("📝 Add Grade",      new Color(0, 123, 200),  e -> showAddGradeDialog()));
        panel.add(styledButton("🗑 Remove Student", new Color(200, 50, 50),   e -> removeSelectedStudent()));
        panel.add(styledButton("📄 Full Report",    new Color(150, 80, 200),  e -> showFullReport()));

        return panel;
    }

    private JScrollPane buildTable() {
        String[] cols = {"Name", "Grades", "Average", "Highest", "Lowest", "Letter", "Status"};
        tableModel = new DefaultTableModel(cols, 0) {
            
            public boolean isCellEditable(int r, int c) { return false; }
        };

        JTable table = new JTable(tableModel);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        table.setRowHeight(32);
        table.setBackground(new Color(40, 40, 60));
        table.setForeground(Color.WHITE);
        table.setGridColor(new Color(70, 70, 90));
        table.setSelectionBackground(new Color(0, 123, 200));

        JTableHeader header = table.getTableHeader();
        header.setFont(new Font("Segoe UI", Font.BOLD, 14));
        header.setBackground(new Color(20, 20, 40));
        header.setForeground(new Color(100, 200, 255));

        table.getColumnModel().getColumn(6).setCellRenderer(new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(JTable t, Object val,
                    boolean sel, boolean foc, int row, int col) {
                super.getTableCellRendererComponent(t, val, sel, foc, row, col);
                setHorizontalAlignment(CENTER);
                setForeground("Pass".equals(val) ? new Color(80, 220, 120) : new Color(255, 90, 90));
                return this;
            }
        });
        table.getColumnModel().getColumn(5).setCellRenderer(new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(JTable t, Object val,
                    boolean sel, boolean foc, int row, int col) {
                super.getTableCellRendererComponent(t, val, sel, foc, row, col);
                setHorizontalAlignment(CENTER);
                String grade = (String) val;
                setForeground(switch (grade) {
                    case "A" -> new Color(80, 220, 120);
                    case "B" -> new Color(100, 200, 255);
                    case "C" -> new Color(255, 200, 50);
                    case "D" -> new Color(255, 140, 0);
                    default  -> new Color(255, 90, 90);
                });
                return this;
            }
        });

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
        scroll.getViewport().setBackground(new Color(40, 40, 60));
        return scroll;
    }

    private JPanel buildBottom() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 8));
        panel.setBackground(new Color(20, 20, 35));
        lblSummary = new JLabel("No students yet.");
        lblSummary.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblSummary.setForeground(new Color(180, 180, 200));
        panel.add(lblSummary);
        return panel;
    }

    private void showAddStudentDialog() {
        String name = JOptionPane.showInputDialog(this, "Enter student name:", "Add Student", JOptionPane.PLAIN_MESSAGE);
        if (name != null && !name.trim().isEmpty()) {
            for (Student s : students)
                if (s.name.equalsIgnoreCase(name.trim())) {
                    JOptionPane.showMessageDialog(this, "Student already exists!"); return;
                }
            students.add(new Student(name.trim()));
            refreshTable();
        }
    }

    private void showAddGradeDialog() {
        if (students.isEmpty()) { JOptionPane.showMessageDialog(this, "Add a student first!"); return; }
        String[] names = students.stream().map(s -> s.name).toArray(String[]::new);
        String chosen = (String) JOptionPane.showInputDialog(this, "Select student:", "Add Grade",
                JOptionPane.PLAIN_MESSAGE, null, names, names[0]);
        if (chosen == null) return;
        String input = JOptionPane.showInputDialog(this, "Enter grade (0–100):", "Add Grade", JOptionPane.PLAIN_MESSAGE);
        if (input == null) return;
        try {
            double grade = Double.parseDouble(input.trim());
            if (grade < 0 || grade > 100) { JOptionPane.showMessageDialog(this, "Grade must be 0–100."); return; }
            for (Student s : students) if (s.name.equals(chosen)) { s.addGrade(grade); break; }
            refreshTable();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Invalid number.");
        }
    }

    private void removeSelectedStudent() {
        if (students.isEmpty()) return;
        String[] names = students.stream().map(s -> s.name).toArray(String[]::new);
        String chosen = (String) JOptionPane.showInputDialog(this, "Select student to remove:",
                "Remove Student", JOptionPane.PLAIN_MESSAGE, null, names, names[0]);
        if (chosen != null) {
            students.removeIf(s -> s.name.equals(chosen));
            refreshTable();
        }
    }

    private void showFullReport() {
        if (students.isEmpty()) { JOptionPane.showMessageDialog(this, "No students to report."); return; }
        StringBuilder sb = new StringBuilder();
        sb.append("═══════════════════════════════════════\n");
        sb.append("        STUDENT GRADE REPORT\n");
        sb.append("═══════════════════════════════════════\n\n");
        for (Student s : students) {
            sb.append(String.format("Name    : %s%n", s.name));
            sb.append(String.format("Grades  : %s%n", s.grades));
            sb.append(String.format("Average : %.2f  (%s)%n", s.getAverage(), s.getLetterGrade()));
            sb.append(String.format("Highest : %.2f%n", s.getHighest()));
            sb.append(String.format("Lowest  : %.2f%n", s.getLowest()));
            sb.append(String.format("Status  : %s%n", s.getStatus()));
            sb.append("───────────────────────────────────────\n");
        }
        // Class stats
        double classAvg = students.stream().mapToDouble(Student::getAverage).average().orElse(0);
        Student top = students.stream().max((a, b) -> Double.compare(a.getAverage(), b.getAverage())).orElse(null);
        Student low = students.stream().min((a, b) -> Double.compare(a.getAverage(), b.getAverage())).orElse(null);
        sb.append(String.format("%nClass Average : %.2f%n", classAvg));
        if (top != null) sb.append(String.format("Top Student   : %s (%.2f)%n", top.name, top.getAverage()));
        if (low != null) sb.append(String.format("Needs Help    : %s (%.2f)%n", low.name, low.getAverage()));

        JTextArea area = new JTextArea(sb.toString());
        area.setFont(new Font("Monospaced", Font.PLAIN, 13));
        area.setEditable(false);
        JOptionPane.showMessageDialog(this, new JScrollPane(area), "Full Report", JOptionPane.INFORMATION_MESSAGE);
    }

    private void refreshTable() {
        tableModel.setRowCount(0);
        for (Student s : students) {
            tableModel.addRow(new Object[]{
                s.name,
                s.grades.isEmpty() ? "—" : s.grades.toString(),
                s.grades.isEmpty() ? "—" : String.format("%.2f", s.getAverage()),
                s.grades.isEmpty() ? "—" : String.format("%.2f", s.getHighest()),
                s.grades.isEmpty() ? "—" : String.format("%.2f", s.getLowest()),
                s.grades.isEmpty() ? "—" : s.getLetterGrade(),
                s.grades.isEmpty() ? "—" : s.getStatus()
            });
        }
        updateSummary();
    }

    private void updateSummary() {
        int total = students.size();
        long passed = students.stream().filter(s -> !s.grades.isEmpty() && s.getAverage() >= 60).count();
        double classAvg = students.stream().filter(s -> !s.grades.isEmpty())
                .mapToDouble(Student::getAverage).average().orElse(0);
        lblSummary.setText(String.format(
            "Total Students: %d  |  Passed: %d  |  Failed: %d  |  Class Average: %.2f",
            total, passed, total - passed, classAvg));
    }

    private void loadSampleData() {
        String[][] data = {
            {"Alice",  "92,88,95,91"},
            {"Bob",    "74,68,72,80"},
            {"Charlie","55,60,58,62"},
            {"Diana",  "85,90,88,92"}
        };
        for (String[] d : data) {
            Student s = new Student(d[0]);
            for (String g : d[1].split(",")) s.addGrade(Double.parseDouble(g));
            students.add(s);
        }
    }

    private JButton styledButton(String text, Color bg, ActionListener al) {
        JButton btn = new JButton(text);
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addActionListener(al);
        return btn;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Studentgradetracker::new);
    }
}