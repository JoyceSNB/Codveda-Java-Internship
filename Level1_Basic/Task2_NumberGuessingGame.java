import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.Random;

/**
 * Level 1 (Basic) - Task 2: Simple Number Guessing Game
 * Codveda Technology - Java Development Internship
 *
 * A GUI number guessing game built with Java Swing. The program
 * generates a random number and the user tries to guess it,
 * receiving "too high" / "too low" feedback within a limited
 * number of attempts. Shows a custom styled result dialog when
 * the round ends, with a hand-drawn checkmark/cross icon instead
 * of emoji.
 */
public class Task2_NumberGuessingGame extends JFrame {

    private static final int MIN_RANGE = 1;
    private static final int MAX_RANGE = 100;
    private static final int MAX_ATTEMPTS = 7;

    private final Random random = new Random();
    private int targetNumber;
    private int attemptsUsed;
    private boolean gameOver;

    private JLabel feedbackLabel;
    private JLabel attemptsLabel;
    private JTextField guessField;
    private RoundedActionButton guessButton;
    private RoundedActionButton restartButton;

    // Color palette
    private static final Color COLOR_BG = new Color(28, 28, 30);
    private static final Color COLOR_PANEL = new Color(44, 44, 48);
    private static final Color COLOR_ACCENT = new Color(90, 200, 250);
    private static final Color COLOR_SUCCESS = new Color(76, 217, 100);
    private static final Color COLOR_WARNING = new Color(255, 149, 0);
    private static final Color COLOR_DANGER = new Color(255, 69, 58);
    private static final Color COLOR_TEXT = Color.WHITE;
    private static final Color COLOR_SUBTEXT = new Color(160, 160, 165);

    public Task2_NumberGuessingGame() {
        setTitle("Number Guessing Game - Codveda Internship");
        setSize(380, 420);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);
        getContentPane().setBackground(COLOR_BG);

        buildUI();
        startNewGame();
    }

    private void buildUI() {
        setLayout(new BorderLayout(15, 15));
        ((JPanel) getContentPane()).setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25));

        JLabel titleLabel = new JLabel("Number Guessing Game", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        titleLabel.setForeground(COLOR_TEXT);

        JLabel infoLabel = new JLabel(
                "Guess a number between " + MIN_RANGE + " and " + MAX_RANGE,
                SwingConstants.CENTER);
        infoLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        infoLabel.setForeground(COLOR_SUBTEXT);

        JPanel headerPanel = new JPanel(new GridLayout(2, 1, 0, 6));
        headerPanel.setBackground(COLOR_BG);
        headerPanel.add(titleLabel);
        headerPanel.add(infoLabel);

        feedbackLabel = new JLabel("Make your first guess!", SwingConstants.CENTER);
        feedbackLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        feedbackLabel.setForeground(COLOR_ACCENT);

        attemptsLabel = new JLabel("", SwingConstants.CENTER);
        attemptsLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        attemptsLabel.setForeground(new Color(220, 220, 225));

        JPanel centerPanel = new JPanel(new GridLayout(2, 1, 0, 10));
        centerPanel.setBackground(COLOR_PANEL);
        centerPanel.setBorder(BorderFactory.createEmptyBorder(25, 15, 25, 15));
        centerPanel.add(feedbackLabel);
        centerPanel.add(attemptsLabel);

        guessField = new JTextField();
        guessField.setFont(new Font("Segoe UI", Font.BOLD, 18));
        guessField.setHorizontalAlignment(JTextField.CENTER);
        guessField.setBackground(COLOR_PANEL);
        guessField.setForeground(COLOR_TEXT);
        guessField.setCaretColor(COLOR_TEXT);
        guessField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(COLOR_ACCENT, 1),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)));
        guessField.addActionListener(this::onGuessSubmitted);

        guessButton = new RoundedActionButton("Guess");
        guessButton.setFont(new Font("Segoe UI", Font.BOLD, 16));
        guessButton.setBackground(COLOR_ACCENT);
        guessButton.setForeground(Color.BLACK);
        guessButton.addActionListener(this::onGuessSubmitted);

        restartButton = new RoundedActionButton("New Game");
        restartButton.setFont(new Font("Segoe UI", Font.BOLD, 16));
        restartButton.setBackground(COLOR_WARNING);
        restartButton.setForeground(Color.WHITE);
        restartButton.addActionListener(e -> startNewGame());

        JPanel buttonPanel = new JPanel(new GridLayout(1, 2, 10, 0));
        buttonPanel.setBackground(COLOR_BG);
        buttonPanel.add(guessButton);
        buttonPanel.add(restartButton);

        JPanel bottomPanel = new JPanel(new BorderLayout(0, 12));
        bottomPanel.setBackground(COLOR_BG);
        bottomPanel.add(guessField, BorderLayout.NORTH);
        bottomPanel.add(buttonPanel, BorderLayout.SOUTH);

        add(headerPanel, BorderLayout.NORTH);
        add(centerPanel, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    private void startNewGame() {
        targetNumber = random.nextInt(MAX_RANGE - MIN_RANGE + 1) + MIN_RANGE;
        attemptsUsed = 0;
        gameOver = false;

        feedbackLabel.setText("Make your first guess!");
        feedbackLabel.setForeground(COLOR_ACCENT);
        updateAttemptsLabel();

        guessField.setText("");
        guessField.setEnabled(true);
        guessButton.setEnabled(true);
        guessField.requestFocusInWindow();
    }

    private void onGuessSubmitted(ActionEvent e) {
        if (gameOver) {
            return;
        }

        String rawInput = guessField.getText().trim();
        int guess;

        try {
            guess = Integer.parseInt(rawInput);
        } catch (NumberFormatException ex) {
            feedbackLabel.setText("Please enter a valid whole number.");
            feedbackLabel.setForeground(COLOR_DANGER);
            return;
        }

        if (guess < MIN_RANGE || guess > MAX_RANGE) {
            feedbackLabel.setText("Enter a number between " + MIN_RANGE + " and " + MAX_RANGE + ".");
            feedbackLabel.setForeground(COLOR_DANGER);
            return;
        }

        attemptsUsed++;
        guessField.setText("");

        if (guess == targetNumber) {
            feedbackLabel.setText("Correct! The number was " + targetNumber + "!");
            feedbackLabel.setForeground(COLOR_SUCCESS);
            updateAttemptsLabel();
            endGame(true);
            return;
        }

        if (guess < targetNumber) {
            feedbackLabel.setText("Too low! Try a higher number.");
            feedbackLabel.setForeground(COLOR_WARNING);
        } else {
            feedbackLabel.setText("Too high! Try a lower number.");
            feedbackLabel.setForeground(COLOR_WARNING);
        }

        updateAttemptsLabel();

        if (attemptsUsed >= MAX_ATTEMPTS) {
            feedbackLabel.setText("Out of attempts! The number was " + targetNumber + ".");
            feedbackLabel.setForeground(COLOR_DANGER);
            endGame(false);
        }
    }

    private void updateAttemptsLabel() {
        int remaining = MAX_ATTEMPTS - attemptsUsed;
        attemptsLabel.setText("Attempts remaining: " + Math.max(remaining, 0) + " / " + MAX_ATTEMPTS);
    }

    // Ends the current round and shows a custom styled result dialog
    private void endGame(boolean won) {
        gameOver = true;
        guessField.setEnabled(false);
        guessButton.setEnabled(false);

        String title = won ? "You Win!" : "Game Over";

        String message;
        if (won) {
            // Proper singular/plural grammar based on attemptsUsed
            String attemptWord = (attemptsUsed == 1) ? "attempt" : "attempts";
            message = "You guessed it in " + attemptsUsed + " " + attemptWord + "!";
        } else {
            message = "The correct number was " + targetNumber + ".";
        }

        Color themeColor = won ? COLOR_SUCCESS : COLOR_DANGER;

        ResultDialog.show(this, won, title, message, themeColor, this::startNewGame);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Task2_NumberGuessingGame game = new Task2_NumberGuessingGame();
            game.setVisible(true);
        });
    }
}

class ResultDialog extends JDialog {

    private static final Color COLOR_BG = new Color(28, 28, 30);
    private static final Color COLOR_SUBTEXT = new Color(180, 180, 185);

    private ResultDialog(Frame owner, boolean won, String title, String message,
                          Color themeColor, Runnable onPlayAgain) {
        super(owner, true);
        setUndecorated(true);
        getContentPane().setBackground(COLOR_BG);

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(COLOR_BG);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(themeColor, 2, true),
                BorderFactory.createEmptyBorder(28, 30, 28, 30)));

        IconPanel iconPanel = new IconPanel(won, themeColor);
        iconPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        iconPanel.setMaximumSize(new Dimension(70, 70));
        iconPanel.setPreferredSize(new Dimension(70, 70));

        JLabel titleLabel = new JLabel(title, SwingConstants.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        titleLabel.setForeground(themeColor);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(14, 0, 6, 0));

        JLabel messageLabel = new JLabel(message, SwingConstants.CENTER);
        messageLabel.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        messageLabel.setForeground(COLOR_SUBTEXT);
        messageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        messageLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 22, 0));

        RoundedActionButton playAgainButton = new RoundedActionButton("Play Again");
        playAgainButton.setFont(new Font("Segoe UI", Font.BOLD, 15));
        playAgainButton.setBackground(themeColor);
        playAgainButton.setForeground(Color.BLACK);
        playAgainButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        playAgainButton.addActionListener(e -> {
            dispose();
            onPlayAgain.run();
        });

        panel.add(iconPanel);
        panel.add(titleLabel);
        panel.add(messageLabel);
        panel.add(playAgainButton);

        setContentPane(panel);
        pack();
        setLocationRelativeTo(owner);
    }

    public static void show(Frame owner, boolean won, String title, String message,
                             Color themeColor, Runnable onPlayAgain) {
        new ResultDialog(owner, won, title, message, themeColor, onPlayAgain).setVisible(true);
    }
}

class IconPanel extends JPanel {

    private final boolean won;
    private final Color themeColor;

    public IconPanel(boolean won, Color themeColor) {
        this.won = won;
        this.themeColor = themeColor;
        setOpaque(false);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int size = Math.min(getWidth(), getHeight());
        int x = (getWidth() - size) / 2;
        int y = (getHeight() - size) / 2;

        g2.setColor(themeColor);
        g2.setStroke(new BasicStroke(6, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        int padding = size / 4;

        if (won) {
            // Draw a checkmark only
            g2.drawLine(x + padding, y + size / 2, x + size / 2 - 2, y + size - padding);
            g2.drawLine(x + size / 2 - 2, y + size - padding, x + size - padding, y + padding);
        } else {
            // Draw a cross (X) only
            g2.drawLine(x + padding, y + padding, x + size - padding, y + size - padding);
            g2.drawLine(x + size - padding, y + padding, x + padding, y + size - padding);
        }

        g2.dispose();
    }
}

class RoundedActionButton extends JButton {

    private static final int ARC_SIZE = 16;

    public RoundedActionButton(String label) {
        super(label);
        setContentAreaFilled(false);
        setFocusPainted(false);
        setBorderPainted(false);
        setOpaque(false);
        setPreferredSize(new Dimension(140, 45));
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
    }
}