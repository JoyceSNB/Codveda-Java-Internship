import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.font.TextAttribute;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Level 2 (Intermediate) - Task 2: File Handling - Reading and Writing to a File
 * Codveda Technology - Java Development Internship
 *
 * Reads a plain text file, processes its content (line count, word count,
 * character count, unique words, and word frequency), displays the result
 * in a Swing GUI, and writes the processed data out to a new report file.
 *
 * File-related exceptions are handled explicitly:
 *   - FileNotFoundException : the given path does not exist or is a directory
 *   - IOException           : the file exists but cannot be read or written
 */
public class Task2_FileHandling extends JFrame {

    /** Matches a word: letters, digits, and apostrophes (so "don't" stays one word). */
    private static final Pattern WORD_PATTERN = Pattern.compile("[\\p{L}\\p{N}']+");

    private static final int TOP_WORD_LIMIT = 10;

    private AnalysisReport report;

    private JTextField pathField;
    private JTable wordTable;
    private DefaultTableModel wordTableModel;
    private JLabel statusLabel;
    private FlatButton saveButton;

    private final Map<String, JLabel> statValues = new HashMap<>();

    // ---- Palette: neutral graphite with a single restrained accent ----
    private static final Color BG = new Color(0x15, 0x17, 0x1A);
    private static final Color SURFACE = new Color(0x1D, 0x20, 0x25);
    private static final Color SURFACE_ALT = new Color(0x26, 0x2A, 0x30);
    private static final Color BORDER = new Color(0x30, 0x35, 0x3C);
    private static final Color BORDER_STRONG = new Color(0x4A, 0x51, 0x5A);

    private static final Color TEXT = new Color(0xE6, 0xE8, 0xEB);
    private static final Color TEXT_MUTED = new Color(0xA2, 0xA9, 0xB2);
    private static final Color TEXT_FAINT = new Color(0x84, 0x8B, 0x94);

    private static final Color ACCENT = new Color(0x3D, 0x6E, 0x9E);
    private static final Color ACCENT_HOVER = new Color(0x4B, 0x82, 0xB6);
    private static final Color STATUS_OK = new Color(0x7C, 0xAD, 0x88);
    private static final Color STATUS_FAIL = new Color(0xC4, 0x6B, 0x63);

    // Secondary buttons sit on the window background, so they need their own
    // lighter fill and a clearly visible border to read as buttons at all.
    private static final Color BTN_SECONDARY = new Color(0x36, 0x3D, 0x45);
    private static final Color BTN_SECONDARY_HOVER = new Color(0x43, 0x4B, 0x55);
    private static final Color BTN_SECONDARY_BORDER = new Color(0x63, 0x6B, 0x76);

    // ---- Typography ----
    private static final Font FONT_TITLE = new Font("Segoe UI", Font.PLAIN, 19);
    private static final Font FONT_BODY = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font FONT_VALUE = new Font("Consolas", Font.PLAIN, 16);
    private static final Font FONT_TABLE = new Font("Consolas", Font.PLAIN, 13);

    public Task2_FileHandling() {
        setTitle("File Handling - Codveda Internship");
        setSize(820, 690);
        setMinimumSize(new Dimension(740, 670));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(BG);

        buildUI();
    }

    // ==================== UI ====================

    private void buildUI() {
        setLayout(new BorderLayout());
        ((JPanel) getContentPane()).setBorder(new EmptyBorder(22, 26, 20, 26));

        add(buildHeaderPanel(), BorderLayout.NORTH);
        add(buildCenterPanel(), BorderLayout.CENTER);
        add(buildBottomPanel(), BorderLayout.SOUTH);
    }

    private JPanel buildHeaderPanel() {
        JLabel titleLabel = new JLabel("Text File Analyzer");
        titleLabel.setFont(FONT_TITLE);
        titleLabel.setForeground(TEXT);

        JLabel subtitleLabel = new JLabel("Level 2 \u00b7 Task 2 \u2014 reading and writing files");
        subtitleLabel.setFont(FONT_BODY);
        subtitleLabel.setForeground(TEXT_MUTED);

        JPanel titleBlock = new JPanel(new GridLayout(2, 1, 0, 4));
        titleBlock.setBackground(BG);
        titleBlock.setBorder(new EmptyBorder(0, 0, 18, 0));
        titleBlock.add(titleLabel);
        titleBlock.add(subtitleLabel);

        pathField = new JTextField();
        pathField.setFont(new Font("Consolas", Font.PLAIN, 13));
        pathField.setBackground(SURFACE);
        pathField.setForeground(TEXT);
        pathField.setCaretColor(TEXT);
        pathField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_STRONG, 1),
                new EmptyBorder(9, 11, 9, 11)));
        pathField.addActionListener(e -> analyzeSelectedFile());

        FlatButton browseButton = new FlatButton("Browse", FlatButton.Variant.SECONDARY);
        browseButton.addActionListener(e -> browseForFile());

        FlatButton analyzeButton = new FlatButton("Analyze", FlatButton.Variant.PRIMARY);
        analyzeButton.addActionListener(e -> analyzeSelectedFile());

        JPanel actionPanel = new JPanel(new GridLayout(1, 2, 8, 0));
        actionPanel.setBackground(BG);
        actionPanel.add(browseButton);
        actionPanel.add(analyzeButton);

        JPanel inputRow = new JPanel(new BorderLayout(10, 0));
        inputRow.setBackground(BG);
        inputRow.add(pathField, BorderLayout.CENTER);
        inputRow.add(actionPanel, BorderLayout.EAST);

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(BG);
        header.add(titleBlock, BorderLayout.NORTH);
        header.add(inputRow, BorderLayout.CENTER);
        return header;
    }

    private JPanel buildCenterPanel() {
        JPanel center = new JPanel(new BorderLayout(18, 0));
        center.setBackground(BG);
        center.setBorder(new EmptyBorder(18, 0, 0, 0));

        center.add(buildStatsPanel(), BorderLayout.WEST);
        center.add(buildWordTablePanel(), BorderLayout.CENTER);
        return center;
    }

    private JPanel buildStatsPanel() {
        // BoxLayout lets each row keep its preferred height. GridLayout would
        // divide the available height equally and clip the taller value labels.
        JPanel stack = new JPanel();
        stack.setLayout(new BoxLayout(stack, BoxLayout.Y_AXIS));
        stack.setBackground(SURFACE);

        String[] labels = {
                "Lines", "Words", "Characters (with spaces)",
                "Characters (no spaces)", "Unique words", "Longest word"
        };

        for (int i = 0; i < labels.length; i++) {
            stack.add(makeStatRow(labels[i], i < labels.length - 1));
        }

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(SURFACE);
        panel.setBorder(BorderFactory.createLineBorder(BORDER, 1));
        panel.setPreferredSize(new Dimension(290, 0));
        panel.add(stack, BorderLayout.NORTH);
        return panel;
    }

    private JPanel makeStatRow(String name, boolean withDivider) {
        JLabel nameLabel = new JLabel(name.toUpperCase());
        nameLabel.setFont(tracked(new Font("Segoe UI", Font.PLAIN, 11), 0.10));
        nameLabel.setForeground(TEXT_MUTED);

        JLabel valueLabel = new JLabel("\u2014");
        valueLabel.setFont(FONT_VALUE);
        valueLabel.setForeground(TEXT);

        statValues.put(name, valueLabel);

        JPanel row = new JPanel(new BorderLayout(0, 4));
        row.setBackground(SURFACE);
        row.setBorder(BorderFactory.createCompoundBorder(
                withDivider ? new MatteBorder(0, 0, 1, 0, BORDER) : new EmptyBorder(0, 0, 0, 0),
                new EmptyBorder(10, 16, 10, 16)));
        row.add(nameLabel, BorderLayout.NORTH);
        row.add(valueLabel, BorderLayout.CENTER);

        // Pin the height so BoxLayout never stretches or squeezes the row
        int height = row.getPreferredSize().height;
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
        row.setMinimumSize(new Dimension(0, height));
        return row;
    }

    private JPanel buildWordTablePanel() {
        String[] columns = {"No", "Word", "Count"};
        wordTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        wordTable = new JTable(wordTableModel);
        wordTable.setBackground(SURFACE);
        wordTable.setForeground(TEXT);
        wordTable.setShowVerticalLines(false);
        wordTable.setGridColor(BORDER);
        wordTable.setRowHeight(31);
        wordTable.setFont(FONT_TABLE);
        wordTable.setSelectionBackground(SURFACE_ALT);
        wordTable.setSelectionForeground(TEXT);
        wordTable.setIntercellSpacing(new Dimension(0, 0));

        JTableHeader header = wordTable.getTableHeader();
        header.setBackground(SURFACE_ALT);
        header.setForeground(TEXT_MUTED);
        header.setFont(tracked(new Font("Segoe UI", Font.PLAIN, 11), 0.10));
        header.setBorder(new MatteBorder(0, 0, 1, 0, BORDER));
        header.setReorderingAllowed(false);
        header.setPreferredSize(new Dimension(0, 32));

        DefaultTableCellRenderer headerRenderer = new DefaultTableCellRenderer();
        headerRenderer.setBackground(SURFACE_ALT);
        headerRenderer.setForeground(TEXT_MUTED);
        headerRenderer.setFont(tracked(new Font("Segoe UI", Font.PLAIN, 11), 0.10));
        headerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        for (int i = 0; i < wordTable.getColumnCount(); i++) {
            wordTable.getColumnModel().getColumn(i).setHeaderRenderer(headerRenderer);
        }

        // Every column is centered, so the three columns read as one block
        DefaultTableCellRenderer centered = new DefaultTableCellRenderer();
        centered.setHorizontalAlignment(SwingConstants.CENTER);

        for (int i = 0; i < wordTable.getColumnCount(); i++) {
            wordTable.getColumnModel().getColumn(i).setCellRenderer(centered);
        }
        wordTable.getColumnModel().getColumn(0).setMinWidth(60);
        wordTable.getColumnModel().getColumn(0).setMaxWidth(60);
        wordTable.getColumnModel().getColumn(2).setMinWidth(90);
        wordTable.getColumnModel().getColumn(2).setMaxWidth(90);

        JLabel caption = new JLabel(("Top " + TOP_WORD_LIMIT + " most frequent words").toUpperCase());
        caption.setFont(tracked(new Font("Segoe UI", Font.PLAIN, 11), 0.10));
        caption.setForeground(TEXT_MUTED);
        caption.setBorder(new EmptyBorder(0, 0, 9, 0));

        JScrollPane scrollPane = new JScrollPane(wordTable);
        scrollPane.getViewport().setBackground(SURFACE);
        scrollPane.setBorder(BorderFactory.createLineBorder(BORDER, 1));
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BG);
        panel.add(caption, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildBottomPanel() {
        statusLabel = new JLabel("Select a .txt file, or type a path and press Enter.");
        statusLabel.setFont(FONT_BODY);
        statusLabel.setForeground(TEXT_FAINT);

        saveButton = new FlatButton("Save report", FlatButton.Variant.PRIMARY);
        saveButton.addActionListener(e -> saveReportToFile());
        saveButton.setEnabled(false);

        FlatButton clearButton = new FlatButton("Clear", FlatButton.Variant.SECONDARY);
        clearButton.addActionListener(e -> clearResults());

        JPanel buttons = new JPanel(new GridLayout(1, 2, 8, 0));
        buttons.setBackground(BG);
        buttons.add(clearButton);
        buttons.add(saveButton);

        JPanel bottom = new JPanel(new BorderLayout(15, 0));
        bottom.setBackground(BG);
        bottom.setBorder(BorderFactory.createCompoundBorder(
                new MatteBorder(1, 0, 0, 0, BORDER),
                new EmptyBorder(16, 0, 0, 0)));
        bottom.add(statusLabel, BorderLayout.CENTER);
        bottom.add(buttons, BorderLayout.EAST);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(BG);
        wrapper.setBorder(new EmptyBorder(18, 0, 0, 0));
        wrapper.add(bottom, BorderLayout.CENTER);
        return wrapper;
    }

    // ==================== File selection ====================

    private void browseForFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select a text file");
        chooser.setFileFilter(new FileNameExtensionFilter("Text files (*.txt)", "txt"));

        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            pathField.setText(chooser.getSelectedFile().getAbsolutePath());
            analyzeSelectedFile();
        }
    }

    // ==================== READ + PROCESS ====================

    private void analyzeSelectedFile() {
        String path = pathField.getText().trim();
        if (path.isEmpty()) {
            showError("Please enter a file path or click Browse.");
            setStatus("No file selected.", TEXT_FAINT);
            return;
        }

        try {
            report = readAndProcess(new File(path));
            displayReport(report);
            saveButton.setEnabled(true);
            setStatus("Analyzed " + report.fileName, STATUS_OK);

        } catch (FileNotFoundException ex) {
            // Thrown when the path does not exist, or points to a directory
            clearResults();
            showError("File not found:\n" + path
                    + "\n\nCheck that the path is correct and that it points to a file.");
            setStatus("File not found.", STATUS_FAIL);

        } catch (IOException ex) {
            // The file exists but could not be read (permissions, I/O failure, etc.)
            clearResults();
            showError("Could not read the file:\n" + ex.getMessage());
            setStatus("Read failed.", STATUS_FAIL);
        }
    }

    /**
     * Reads the file line by line and builds the analysis report.
     *
     * @throws FileNotFoundException if the path does not exist or is a directory
     * @throws IOException           if reading fails partway through
     */
    private AnalysisReport readAndProcess(File file) throws IOException {
        // FileReader throws FileNotFoundException for a missing file, but for a
        // directory the behaviour is platform-dependent, so check explicitly.
        if (file.isDirectory()) {
            throw new FileNotFoundException(file.getPath() + " is a directory, not a file");
        }

        AnalysisReport result = new AnalysisReport();
        result.fileName = file.getName();
        result.filePath = file.getAbsolutePath();

        Map<String, Integer> frequencies = new HashMap<>();

        // try-with-resources closes the reader automatically, even if an
        // exception is thrown midway through reading.
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                result.lineCount++;
                result.charCountWithSpaces += line.length();

                for (char c : line.toCharArray()) {
                    if (!Character.isWhitespace(c)) {
                        result.charCountNoSpaces++;
                    }
                }

                Matcher matcher = WORD_PATTERN.matcher(line);
                while (matcher.find()) {
                    String word = matcher.group().toLowerCase();
                    result.wordCount++;
                    frequencies.merge(word, 1, Integer::sum);

                    if (word.length() > result.longestWord.length()) {
                        result.longestWord = word;
                    }
                }
            }
        }

        result.uniqueWordCount = frequencies.size();
        result.topWords = rankTopWords(frequencies);
        return result;
    }

    /** Sorts by frequency (descending), then alphabetically for ties. */
    private List<Map.Entry<String, Integer>> rankTopWords(Map<String, Integer> frequencies) {
        List<Map.Entry<String, Integer>> entries = new ArrayList<>(frequencies.entrySet());
        entries.sort(Comparator
                .comparing(Map.Entry<String, Integer>::getValue, Comparator.reverseOrder())
                .thenComparing(Map.Entry::getKey));

        return entries.subList(0, Math.min(TOP_WORD_LIMIT, entries.size()));
    }

    // ==================== Display ====================

    private void displayReport(AnalysisReport r) {
        statValues.get("Lines").setText(String.format("%,d", r.lineCount));
        statValues.get("Words").setText(String.format("%,d", r.wordCount));
        statValues.get("Characters (with spaces)").setText(String.format("%,d", r.charCountWithSpaces));
        statValues.get("Characters (no spaces)").setText(String.format("%,d", r.charCountNoSpaces));
        statValues.get("Unique words").setText(String.format("%,d", r.uniqueWordCount));
        statValues.get("Longest word").setText(r.longestWord.isEmpty() ? "\u2014" : r.longestWord);

        wordTableModel.setRowCount(0);
        int rank = 1;
        for (Map.Entry<String, Integer> entry : r.topWords) {
            wordTableModel.addRow(new Object[]{rank++, entry.getKey(), entry.getValue()});
        }
    }

    private void clearResults() {
        report = null;
        for (JLabel value : statValues.values()) {
            value.setText("\u2014");
        }
        wordTableModel.setRowCount(0);
        saveButton.setEnabled(false);
    }

    // ==================== WRITE ====================

    private void saveReportToFile() {
        if (report == null) {
            showError("Analyze a file first.");
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save analysis report");
        chooser.setSelectedFile(new File("analysis_report.txt"));

        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File output = chooser.getSelectedFile();

        if (output.exists()) {
            int confirm = JOptionPane.showConfirmDialog(this,
                    "\"" + output.getName() + "\" already exists. Overwrite it?",
                    "Confirm Overwrite", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (confirm != JOptionPane.YES_OPTION) {
                return;
            }
        }

        try (PrintWriter writer = new PrintWriter(new FileWriter(output))) {
            writeReport(writer, report);

            JOptionPane.showMessageDialog(this,
                    "Report saved to:\n" + output.getAbsolutePath(),
                    "Save Complete", JOptionPane.INFORMATION_MESSAGE);
            setStatus("Report saved to " + output.getName(), STATUS_OK);

        } catch (IOException ex) {
            showError("Failed to write the report:\n" + ex.getMessage());
            setStatus("Write failed.", STATUS_FAIL);
        }
    }

    private void writeReport(PrintWriter writer, AnalysisReport r) {
        String timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        writer.println("========================================");
        writer.println("        TEXT FILE ANALYSIS REPORT");
        writer.println("========================================");
        writer.println("Source file : " + r.filePath);
        writer.println("Generated   : " + timestamp);
        writer.println();
        writer.println("--- SUMMARY ---");
        writer.printf("Lines                    : %,d%n", r.lineCount);
        writer.printf("Words                    : %,d%n", r.wordCount);
        writer.printf("Characters (with spaces) : %,d%n", r.charCountWithSpaces);
        writer.printf("Characters (no spaces)   : %,d%n", r.charCountNoSpaces);
        writer.printf("Unique words             : %,d%n", r.uniqueWordCount);
        writer.println("Longest word             : " + (r.longestWord.isEmpty() ? "-" : r.longestWord));
        writer.println();
        writer.println("--- TOP " + TOP_WORD_LIMIT + " MOST FREQUENT WORDS ---");

        if (r.topWords.isEmpty()) {
            writer.println("(no words found)");
        } else {
            int rank = 1;
            for (Map.Entry<String, Integer> entry : r.topWords) {
                writer.printf("%2d. %-20s %,d%n", rank++, entry.getKey(), entry.getValue());
            }
        }

        writer.println();
        writer.println("========================================");
        writer.println("End of report");
    }

    // ==================== Helpers ====================

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "File Error", JOptionPane.ERROR_MESSAGE);
    }

    private void setStatus(String message, Color color) {
        statusLabel.setText(message);
        statusLabel.setForeground(color);
    }

    /** Applies letter-spacing to a font, so the small caps labels read as deliberate. */
    private static Font tracked(Font base, double tracking) {
        Map<TextAttribute, Object> attributes = new HashMap<>();
        attributes.put(TextAttribute.TRACKING, tracking);
        return base.deriveFont(attributes);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Task2_FileHandling app = new Task2_FileHandling();
            app.setVisible(true);
        });
    }

    // ==================== Model ====================

    /** Holds the processed statistics for one analyzed file. */
    private static class AnalysisReport {
        String fileName = "";
        String filePath = "";
        int lineCount = 0;
        int wordCount = 0;
        int charCountWithSpaces = 0;
        int charCountNoSpaces = 0;
        int uniqueWordCount = 0;
        String longestWord = "";
        List<Map.Entry<String, Integer>> topWords = new ArrayList<>();
    }

    /**
     * A flat, square-cornered button with two variants: a filled primary and
     * an outlined secondary. Declared as a nested class so it does not collide
     * with the helper buttons defined in the other task files.
     */
    private static class FlatButton extends JButton {

        enum Variant { PRIMARY, SECONDARY }

        private static final int ARC = 4;
        private final Variant variant;

        FlatButton(String label, Variant variant) {
            super(label);
            this.variant = variant;

            setFont(new Font("Segoe UI", Font.PLAIN, 13));
            setContentAreaFilled(false);
            setFocusPainted(false);
            setBorderPainted(false);
            setOpaque(false);
            setPreferredSize(new Dimension(112, 36));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setForeground(variant == Variant.PRIMARY ? Color.WHITE : TEXT);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            boolean hovered = getModel().isRollover();
            int w = getWidth();
            int h = getHeight();

            if (!isEnabled()) {
                g2.setColor(SURFACE);
                g2.fillRoundRect(0, 0, w, h, ARC, ARC);
                g2.setColor(BORDER);
                g2.drawRoundRect(0, 0, w - 1, h - 1, ARC, ARC);
                setForeground(TEXT_FAINT);

            } else if (variant == Variant.PRIMARY) {
                g2.setColor(hovered ? ACCENT_HOVER : ACCENT);
                g2.fillRoundRect(0, 0, w, h, ARC, ARC);
                setForeground(Color.WHITE);

            } else {
                // Secondary: filled with a lighter neutral plus a bright border,
                // so it never disappears against the window background.
                g2.setColor(hovered ? BTN_SECONDARY_HOVER : BTN_SECONDARY);
                g2.fillRoundRect(0, 0, w, h, ARC, ARC);
                g2.setColor(BTN_SECONDARY_BORDER);
                g2.drawRoundRect(0, 0, w - 1, h - 1, ARC, ARC);
                setForeground(TEXT);
            }

            g2.dispose();
            super.paintComponent(g);
        }

        @Override
        protected void paintBorder(Graphics g) {
            // Border is painted in paintComponent so it can react to hover state
        }
    }
}