import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Locale;

/**
 * Level 2 (Intermediate) - Task 1: Employee Management System
 * Codveda Technology - Java Development Internship
 *
 * A basic Employee Management System with CRUD functionality
 * (Create, Read, Update, Delete) built using Object-Oriented
 * Programming. Employee data is stored in an in-memory ArrayList
 * and displayed in a JTable-based Swing GUI. Includes a CSV
 * export feature (the file can be opened directly in Excel).
 */
public class Task1_EmployeeManagementSystem extends JFrame {

    // In-memory storage for employee records
    private final ArrayList<Employee> employees = new ArrayList<>();

    private JTable table;
    private DefaultTableModel tableModel;
    private JTextField nameField;
    private JTextField positionField;
    private JTextField salaryField;

    // Color palette
    private static final Color COLOR_BG = new Color(28, 28, 30);
    private static final Color COLOR_PANEL = new Color(44, 44, 48);
    private static final Color COLOR_ACCENT = new Color(90, 200, 250);
    private static final Color COLOR_SUCCESS = new Color(76, 217, 100);
    private static final Color COLOR_WARNING = new Color(255, 149, 0);
    private static final Color COLOR_DANGER = new Color(255, 69, 58);
    private static final Color COLOR_EXPORT = new Color(150, 120, 220);
    private static final Color COLOR_TEXT = Color.WHITE;
    private static final Color COLOR_SUBTEXT = new Color(180, 180, 185);

    public Task1_EmployeeManagementSystem() {
        setTitle("Employee Management System - Codveda Internship");
        setSize(820, 520);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(COLOR_BG);

        buildUI();
    }

    private void buildUI() {
        setLayout(new BorderLayout(15, 15));
        ((JPanel) getContentPane()).setBorder(new EmptyBorder(15, 20, 15, 20));

        // ===== Header =====
        JLabel titleLabel = new JLabel("Employee Management System", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        titleLabel.setForeground(COLOR_TEXT);
        add(titleLabel, BorderLayout.NORTH);

        // ===== Table (Read) =====
        String[] columns = {"No", "ID", "Name", "Position", "Salary"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // editing only through the Update button
            }
        };

        table = new JTable(tableModel);
        table.setBackground(COLOR_PANEL);
        table.setForeground(COLOR_TEXT);
        table.setGridColor(new Color(70, 70, 75));
        table.setRowHeight(30);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        table.setSelectionBackground(COLOR_ACCENT);
        table.setSelectionForeground(Color.BLACK);
        table.getSelectionModel().addListSelectionListener(e -> fillFormFromSelection());

        JTableHeader header = table.getTableHeader();
        header.setBackground(new Color(60, 60, 65));
        header.setForeground(COLOR_TEXT);
        header.setFont(new Font("Segoe UI", Font.BOLD, 14));

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        table.getColumnModel().getColumn(0).setCellRenderer(centerRenderer); // No
        table.getColumnModel().getColumn(0).setMaxWidth(50);
        table.getColumnModel().getColumn(1).setCellRenderer(centerRenderer); // ID
        table.getColumnModel().getColumn(1).setMaxWidth(70);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.getViewport().setBackground(COLOR_PANEL);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(70, 70, 75)));
        add(scrollPane, BorderLayout.CENTER);

        // ===== Bottom: form + buttons =====
        JPanel bottomPanel = new JPanel(new BorderLayout(0, 12));
        bottomPanel.setBackground(COLOR_BG);

        // --- Input form ---
        JPanel formPanel = new JPanel(new GridLayout(2, 3, 12, 6));
        formPanel.setBackground(COLOR_BG);

        formPanel.add(makeFormLabel("Name"));
        formPanel.add(makeFormLabel("Position"));
        formPanel.add(makeFormLabel("Salary"));

        nameField = makeTextField();
        positionField = makeTextField();
        salaryField = makeTextField();

        formPanel.add(nameField);
        formPanel.add(positionField);
        formPanel.add(salaryField);

        // --- Action buttons (CRUD + Export) ---
        JPanel buttonPanel = new JPanel(new GridLayout(1, 5, 12, 0));
        buttonPanel.setBackground(COLOR_BG);

        RoundedButton addButton = makeButton("Add", COLOR_SUCCESS, Color.BLACK);
        addButton.addActionListener(e -> addEmployee());

        RoundedButton updateButton = makeButton("Update", COLOR_ACCENT, Color.BLACK);
        updateButton.addActionListener(e -> updateEmployee());

        RoundedButton deleteButton = makeButton("Delete", COLOR_DANGER, Color.WHITE);
        deleteButton.addActionListener(e -> deleteEmployee());

        RoundedButton clearButton = makeButton("Clear Form", COLOR_WARNING, Color.WHITE);
        clearButton.addActionListener(e -> clearForm());

        RoundedButton exportButton = makeButton("Export CSV", COLOR_EXPORT, Color.WHITE);
        exportButton.addActionListener(e -> exportToCsv());

        buttonPanel.add(addButton);
        buttonPanel.add(updateButton);
        buttonPanel.add(deleteButton);
        buttonPanel.add(clearButton);
        buttonPanel.add(exportButton);

        bottomPanel.add(formPanel, BorderLayout.CENTER);
        bottomPanel.add(buttonPanel, BorderLayout.SOUTH);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    // ==================== CRUD operations ====================

    /**
     * Finds the smallest positive ID that is not currently used by
     * any employee. Example: if existing IDs are 001, 002, 005,
     * the next new employee gets ID 003 (filling the gap) instead
     * of continuing from 006.
     */
    private int getNextAvailableId() {
        int candidate = 1;
        boolean used;
        do {
            used = false;
            for (Employee employee : employees) {
                if (employee.getId() == candidate) {
                    used = true;
                    candidate++;
                    break;
                }
            }
        } while (used);
        return candidate;
    }

    // CREATE - add a new employee record
    private void addEmployee() {
        String name = nameField.getText().trim();
        String position = positionField.getText().trim();
        String salaryText = salaryField.getText().trim();

        if (name.isEmpty() || position.isEmpty() || salaryText.isEmpty()) {
            showError("All fields (Name, Position, Salary) must be filled.");
            return;
        }

        double salary;
        try {
            salary = Double.parseDouble(salaryText);
            if (salary < 0) {
                showError("Salary cannot be negative.");
                return;
            }
        } catch (NumberFormatException ex) {
            showError("Salary must be a valid number.");
            return;
        }

        Employee employee = new Employee(getNextAvailableId(), name, position, salary);
        employees.add(employee);

        // Keep the list sorted by ID so gap-filled entries appear in order
        employees.sort((a, b) -> Integer.compare(a.getId(), b.getId()));

        refreshTable();
        clearForm();
    }

    // UPDATE - edit the selected employee record
    private void updateEmployee() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow == -1) {
            showError("Select an employee from the table first.");
            return;
        }

        String name = nameField.getText().trim();
        String position = positionField.getText().trim();
        String salaryText = salaryField.getText().trim();

        if (name.isEmpty() || position.isEmpty() || salaryText.isEmpty()) {
            showError("All fields (Name, Position, Salary) must be filled.");
            return;
        }

        double salary;
        try {
            salary = Double.parseDouble(salaryText);
            if (salary < 0) {
                showError("Salary cannot be negative.");
                return;
            }
        } catch (NumberFormatException ex) {
            showError("Salary must be a valid number.");
            return;
        }

        Employee employee = employees.get(selectedRow);
        employee.setName(name);
        employee.setPosition(position);
        employee.setSalary(salary);

        refreshTable();
        clearForm();
    }

    // DELETE - remove the selected employee record (standard confirm dialog)
    private void deleteEmployee() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow == -1) {
            showError("Select an employee from the table first.");
            return;
        }

        Employee employee = employees.get(selectedRow);
        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Delete employee \"" + employee.getName() + "\" (ID "
                        + String.format("%03d", employee.getId()) + ")?",
                "Confirm Delete",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            employees.remove(selectedRow);
            refreshTable();
            clearForm();
        }
    }

    // READ - reload all employee data from the list into the table
    private void refreshTable() {
        tableModel.setRowCount(0);
        int rowNumber = 1;
        for (Employee employee : employees) {
            tableModel.addRow(new Object[]{
                    rowNumber++,
                    String.format("%03d", employee.getId()),
                    employee.getName(),
                    employee.getPosition(),
                    String.format("%,.2f", employee.getSalary())
            });
        }
    }

    // EXPORT - save all employee data to a CSV file (opens in Excel)
    private void exportToCsv() {
        if (employees.isEmpty()) {
            showError("No employee data to export.");
            return;
        }

        try (PrintWriter writer = new PrintWriter(new FileWriter("employees.csv"))) {

            // Tell Excel to use comma as separator (fixes regional settings issue)
            writer.println("sep=,");

            // Header row
            writer.println("No,ID,Name,Position,Salary");

            // Data rows:
            // - Locale.US forces dot as decimal separator, so the comma
            //   stays reserved as the CSV column separator
            // - ID is wrapped as ="001" so Excel treats it as text and
            //   keeps the leading zeros
            int rowNumber = 1;
            for (Employee employee : employees) {
                writer.printf(Locale.US, "%d,=\"%03d\",%s,%s,%.2f%n",
                        rowNumber++,
                        employee.getId(),
                        employee.getName(),
                        employee.getPosition(),
                        employee.getSalary());
            }

            JOptionPane.showMessageDialog(this,
                    "Data exported successfully!",
                    "Export Complete", JOptionPane.INFORMATION_MESSAGE);

        } catch (IOException ex) {
            showError("Failed to export: " + ex.getMessage());
        }
    }

    // ==================== Helpers ====================

    // Fill the form fields when a table row is selected
    private void fillFormFromSelection() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow == -1) {
            return;
        }
        Employee employee = employees.get(selectedRow);
        nameField.setText(employee.getName());
        positionField.setText(employee.getPosition());
        salaryField.setText(String.valueOf(employee.getSalary()));
    }

    private void clearForm() {
        nameField.setText("");
        positionField.setText("");
        salaryField.setText("");
        table.clearSelection();
        nameField.requestFocusInWindow();
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Input Error", JOptionPane.ERROR_MESSAGE);
    }

    private JLabel makeFormLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.BOLD, 13));
        label.setForeground(COLOR_SUBTEXT);
        return label;
    }

    private JTextField makeTextField() {
        JTextField field = new JTextField();
        field.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        field.setBackground(COLOR_PANEL);
        field.setForeground(COLOR_TEXT);
        field.setCaretColor(COLOR_TEXT);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(90, 90, 95), 1),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)));
        return field;
    }

    private RoundedButton makeButton(String text, Color background, Color foreground) {
        RoundedButton button = new RoundedButton(text);
        button.setFont(new Font("Segoe UI", Font.BOLD, 14));
        button.setBackground(background);
        button.setForeground(foreground);
        return button;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Task1_EmployeeManagementSystem app = new Task1_EmployeeManagementSystem();
            app.setVisible(true);
        });
    }
}

/**
 * Employee model class (OOP) - represents a single employee record
 * with encapsulated fields and getter/setter methods.
 */
class Employee {

    private final int id;
    private String name;
    private String position;
    private double salary;

    public Employee(int id, String name, String position, double salary) {
        this.id = id;
        this.name = name;
        this.position = position;
        this.salary = salary;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public double getSalary() {
        return salary;
    }

    public void setSalary(double salary) {
        this.salary = salary;
    }
}

/**
 * A JButton subclass with rounded corners for a modern look,
 * consistent with the other tasks in this internship project.
 */
class RoundedButton extends JButton {

    private static final int ARC_SIZE = 16;

    public RoundedButton(String label) {
        super(label);
        setContentAreaFilled(false);
        setFocusPainted(false);
        setBorderPainted(false);
        setOpaque(false);
        setPreferredSize(new Dimension(120, 42));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2.setColor(isEnabled() ? getBackground() : new Color(90, 90, 90));
        g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), ARC_SIZE, ARC_SIZE));

        g2.dispose();
        super.paintComponent(g);
    }

    @Override
    protected void paintBorder(Graphics g) {
        // Intentionally empty - keeps the rounded look clean
    }
}