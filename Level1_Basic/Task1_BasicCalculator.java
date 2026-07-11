import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.RoundRectangle2D;

/**
 * Level 1 (Basic) - Task 1: Basic Calculator
 * Codveda Technology - Java Development Internship
 * Author: Joyce Stephanie Naibaho
 */
public class Task1_BasicCalculator extends JFrame implements ActionListener {

    private final JLabel historyLabel;
    private final JTextField display;

    private double firstNumber = 0;
    private String currentOperator = "";
    private boolean startNewNumber = true;

    // True while the display shows "Error". Blocks any operation that
    // would try to parse the display text as a number.
    private boolean hasError = false;

    private static final String ERROR_TEXT = "Error";

    // Color palette
    private static final Color COLOR_NUMBER = new Color(51, 51, 51);
    private static final Color COLOR_OPERATOR = new Color(255, 149, 0);
    private static final Color COLOR_CLEAR = new Color(255, 69, 58);
    private static final Color COLOR_UTILITY = new Color(90, 120, 150);
    private static final Color COLOR_DISPLAY_BG = new Color(28, 28, 30);
    private static final Color COLOR_HISTORY_TEXT = new Color(150, 150, 150);

    public Task1_BasicCalculator() {
        setTitle("Simple Calculator - Codveda Internship");
        setSize(340, 480);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);
        getContentPane().setBackground(Color.BLACK);

        historyLabel = new JLabel(" ");
        historyLabel.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        historyLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        historyLabel.setForeground(COLOR_HISTORY_TEXT);
        historyLabel.setBackground(COLOR_DISPLAY_BG);
        historyLabel.setOpaque(true);
        historyLabel.setBorder(BorderFactory.createEmptyBorder(8, 15, 0, 15));

        // Main display (shows current input or result)
        display = new JTextField("0");
        display.setEditable(false);
        display.setFont(new Font("Segoe UI", Font.BOLD, 32));
        display.setHorizontalAlignment(JTextField.RIGHT);
        display.setBackground(COLOR_DISPLAY_BG);
        display.setForeground(Color.WHITE);
        display.setBorder(BorderFactory.createEmptyBorder(0, 15, 10, 15));

        JPanel displayPanel = new JPanel(new BorderLayout());
        displayPanel.setBackground(COLOR_DISPLAY_BG);
        displayPanel.add(historyLabel, BorderLayout.NORTH);
        displayPanel.add(display, BorderLayout.CENTER);
        displayPanel.setPreferredSize(new Dimension(300, 90));

        JPanel buttonPanel = new JPanel(new GridLayout(5, 4, 8, 8));
        buttonPanel.setBackground(Color.BLACK);

        String[] buttons = {
                "C", "DEL", "%", "/",
                "7", "8", "9", "X",
                "4", "5", "6", "-",
                "1", "2", "3", "+",
                "±", "0", ".", "="
        };

        for (String label : buttons) {
            RoundedButton button = new RoundedButton(label);
            button.setFont(new Font("Segoe UI", Font.BOLD, 16));
            button.addActionListener(this);
            styleButton(button, label);
            buttonPanel.add(button);
        }

        setLayout(new BorderLayout(10, 10));
        add(displayPanel, BorderLayout.NORTH);
        add(buttonPanel, BorderLayout.CENTER);
        ((JPanel) getContentPane()).setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
    }

    // Assign colors based on button category
    private void styleButton(JButton button, String label) {
        if ("/X-+=".contains(label)) {
            // Arithmetic operators
            button.setBackground(COLOR_OPERATOR);
            button.setForeground(Color.WHITE);
        } else if (label.equals("C") || label.equals("DEL")) {
            // Clear / delete actions
            button.setBackground(COLOR_CLEAR);
            button.setForeground(Color.WHITE);
        } else if (label.equals("±") || label.equals("%")) {
            // Utility actions (sign toggle & percentage)
            button.setBackground(COLOR_UTILITY);
            button.setForeground(Color.WHITE);
        } else {
            // Numbers and decimal point
            button.setBackground(COLOR_NUMBER);
            button.setForeground(Color.WHITE);
        }
    }

    // Arithmetic operations 
    // One method per operation, as required by the task objectives.

    private double add(double a, double b) {
        return a + b;
    }

    private double subtract(double a, double b) {
        return a - b;
    }

    private double multiply(double a, double b) {
        return a * b;
    }

    /**
     * Divides a by b.
     *
     * @throws ArithmeticException if b is zero. Java would silently
     *                             return Infinity for double division,
     *                             so the check is explicit.
     */
    private double divide(double a, double b) {
        if (b == 0) {
            throw new ArithmeticException("Cannot divide by zero");
        }
        return a / b;
    }

    /** Dispatches to the correct arithmetic method based on the operator symbol. */
    private double applyOperation(double a, double b, String operator) {
        switch (operator) {
            case "+":
                return add(a, b);
            case "-":
                return subtract(a, b);
            case "X":
                return multiply(a, b);
            case "/":
                return divide(a, b);
            default:
                throw new IllegalArgumentException("Unknown operator: " + operator);
        }
    }

    // Button handling 

    @Override
    public void actionPerformed(ActionEvent e) {
        String command = e.getActionCommand();

        switch (command) {
            case "0": case "1": case "2": case "3": case "4":
            case "5": case "6": case "7": case "8": case "9":
                appendDigit(command);
                break;
            case ".":
                appendDecimalPoint();
                break;
            case "+": case "-": case "X": case "/":
                setOperator(command);
                break;
            case "=":
                calculateResult();
                break;
            case "C":
                clearAll();
                break;
            case "DEL":
                backspace();
                break;
            case "±":
                toggleSign();
                break;
            case "%":
                applyPercentage();
                break;
        }
    }

    private void appendDigit(String digit) {
        if (hasError) {
            clearAll();
        }
        if (startNewNumber || display.getText().equals("0")) {
            display.setText(digit);
            startNewNumber = false;
        } else {
            display.setText(display.getText() + digit);
        }
    }

    private void appendDecimalPoint() {
        if (hasError) {
            clearAll();
        }
        if (startNewNumber) {
            display.setText("0.");
            startNewNumber = false;
        } else if (!display.getText().contains(".")) {
            display.setText(display.getText() + ".");
        }
    }

    /**
     * Registers an operator. If an operation is already pending and the
     * user has typed a second number, that pending operation is evaluated
     * first, so chained input like "2 + 3 + 4 =" correctly yields 9.
     */
    private void setOperator(String operator) {
        if (hasError) {
            return;
        }

        double currentValue = Double.parseDouble(display.getText());

        if (!currentOperator.isEmpty() && !startNewNumber) {
            try {
                double result = applyOperation(firstNumber, currentValue, currentOperator);
                display.setText(formatResult(result));
                firstNumber = result;
            } catch (ArithmeticException ex) {
                enterErrorState();
                return;
            }
        } else {
            firstNumber = currentValue;
        }

        currentOperator = operator;
        startNewNumber = true;
        historyLabel.setText(formatResult(firstNumber) + " " + operator);
    }

    private void calculateResult() {
        if (hasError || currentOperator.isEmpty()) {
            return;
        }

        double secondNumber = Double.parseDouble(display.getText());

        try {
            double result = applyOperation(firstNumber, secondNumber, currentOperator);

            historyLabel.setText(formatResult(firstNumber) + " " + currentOperator + " "
                    + formatResult(secondNumber) + " =");
            display.setText(formatResult(result));

            currentOperator = "";
            startNewNumber = true;
        } catch (ArithmeticException ex) {
            enterErrorState();
        }
    }

    /**
     * Puts the calculator into a locked error state. Only C, DEL, or
     * typing a fresh number can bring it back, which prevents the display
     * text "Error" from ever reaching Double.parseDouble().
     */
    private void enterErrorState() {
        historyLabel.setText(" ");
        display.setText(ERROR_TEXT);
        hasError = true;
        firstNumber = 0;
        currentOperator = "";
        startNewNumber = true;
    }

    private void clearAll() {
        display.setText("0");
        historyLabel.setText(" ");
        firstNumber = 0;
        currentOperator = "";
        startNewNumber = true;
        hasError = false;
    }

    private void backspace() {
        if (hasError) {
            clearAll();
            return;
        }
        String text = display.getText();
        if (text.length() > 1) {
            display.setText(text.substring(0, text.length() - 1));
        } else {
            display.setText("0");
            startNewNumber = true;
        }
    }

    private void toggleSign() {
        if (hasError) {
            return;
        }
        double value = Double.parseDouble(display.getText());
        display.setText(formatResult(multiply(value, -1)));
    }

    private void applyPercentage() {
        if (hasError) {
            return;
        }
        double value = Double.parseDouble(display.getText());
        display.setText(formatResult(divide(value, 100)));
    }

    /**
     * Removes the trailing ".0" from whole numbers. The magnitude check
     * guards against casting a double that is too large for a long, which
     * would otherwise produce a meaningless clamped value.
     */
    private String formatResult(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return ERROR_TEXT;
        }
        if (value == Math.floor(value) && Math.abs(value) < 1e15) {
            return String.valueOf((long) value);
        }
        return String.valueOf(value);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Task1_BasicCalculator calculator = new Task1_BasicCalculator();
            calculator.setVisible(true);
        });
    }

    /**
     * A JButton subclass that renders with rounded corners instead of the
     * default sharp-edged rectangle. Declared as a nested class so it does
     * not collide with the helper buttons defined in the other task files.
     */
    private static class RoundedButton extends JButton {

        private static final int ARC_SIZE = 20;

        RoundedButton(String label) {
            super(label);
            setContentAreaFilled(false);   // we paint the background ourselves
            setFocusPainted(false);
            setBorderPainted(false);
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g2.setColor(getBackground());
            g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), ARC_SIZE, ARC_SIZE));

            g2.dispose();
            super.paintComponent(g);
        }

        @Override
        protected void paintBorder(Graphics g) {
        }
    }
}