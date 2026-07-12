import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.font.TextAttribute;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Level 3 (Advanced) - Task 1: Library Management System with JDBC
 * Codveda Technology - Java Development Internship
 *
 * CRUD for books and users over MySQL, with borrowing and returning
 * committed as single database transactions. Setup and run instructions
 * are in README.md.
 */

public class Task1_LibraryManagementSystem extends JFrame {

    //  Database configuration 

    /**
     * connectionTimeZone=SERVER tells Connector/J to read DATETIME values using
     * the server's own time zone. Hard-coding serverTimezone=UTC while MySQL
     * runs in local time makes NOW() come back shifted by the offset.
     */
    private static final String DB_URL =
            "jdbc:mysql://localhost:3306/library_db?useSSL=false&connectionTimeZone=SERVER";

    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "";  //Your Password

    // Palette: neutral graphite with a single restrained accent 
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
    private static final Color DANGER = new Color(0x9E, 0x4A, 0x44);
    private static final Color DANGER_HOVER = new Color(0xB5, 0x59, 0x52);
    private static final Color STATUS_OK = new Color(0x7C, 0xAD, 0x88);
    private static final Color STATUS_FAIL = new Color(0xC4, 0x6B, 0x63);

    private static final Color BTN_SECONDARY = new Color(0x36, 0x3D, 0x45);
    private static final Color BTN_SECONDARY_HOVER = new Color(0x43, 0x4B, 0x55);
    private static final Color BTN_SECONDARY_BORDER = new Color(0x63, 0x6B, 0x76);

    // Typography 
    private static final Font FONT_TITLE = new Font("Segoe UI", Font.PLAIN, 19);
    private static final Font FONT_BODY = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font FONT_TABLE = new Font("Consolas", Font.PLAIN, 13);

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private JLabel statusLabel;

    public Task1_LibraryManagementSystem() {
        setTitle("Library Management System - Codveda Internship");
        setSize(940, 700);
        setMinimumSize(new Dimension(880, 660));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(BG);

        buildUI();
    }

    private void buildUI() {
        setLayout(new BorderLayout());
        ((JPanel) getContentPane()).setBorder(new EmptyBorder(22, 26, 20, 26));

        JLabel titleLabel = new JLabel("Library Management System");
        titleLabel.setFont(FONT_TITLE);
        titleLabel.setForeground(TEXT);

        JLabel subtitleLabel = new JLabel("Level 3 \u00b7 Task 1 \u2014 JDBC, MySQL, transactions");
        subtitleLabel.setFont(FONT_BODY);
        subtitleLabel.setForeground(TEXT_MUTED);

        JPanel titleBlock = new JPanel(new GridLayout(2, 1, 0, 4));
        titleBlock.setBackground(BG);
        titleBlock.setBorder(new EmptyBorder(0, 0, 18, 0));
        titleBlock.add(titleLabel);
        titleBlock.add(subtitleLabel);

        LoansPanel loansPanel = new LoansPanel(this);
        BooksPanel booksPanel = new BooksPanel(this, loansPanel);
        UsersPanel usersPanel = new UsersPanel(this, loansPanel);
        loansPanel.setSourcePanels(booksPanel, usersPanel);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setBackground(SURFACE_ALT);
        tabs.setForeground(TEXT);
        tabs.setFont(FONT_BODY);
        tabs.setBorder(BorderFactory.createEmptyBorder());
        tabs.addTab("  Books  ", booksPanel);
        tabs.addTab("  Users  ", usersPanel);
        tabs.addTab("  Loans  ", loansPanel);

        statusLabel = new JLabel("Connecting to library_db\u2026");
        statusLabel.setFont(FONT_BODY);
        statusLabel.setForeground(TEXT_FAINT);

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setBackground(BG);
        bottom.setBorder(BorderFactory.createCompoundBorder(
                new MatteBorder(1, 0, 0, 0, BORDER),
                new EmptyBorder(14, 0, 0, 0)));
        bottom.add(statusLabel, BorderLayout.WEST);

        JPanel bottomWrapper = new JPanel(new BorderLayout());
        bottomWrapper.setBackground(BG);
        bottomWrapper.setBorder(new EmptyBorder(16, 0, 0, 0));
        bottomWrapper.add(bottom, BorderLayout.CENTER);

        add(titleBlock, BorderLayout.NORTH);
        add(tabs, BorderLayout.CENTER);
        add(bottomWrapper, BorderLayout.SOUTH);

        // Initial load
        try {
            booksPanel.reload();
            usersPanel.reload();
            loansPanel.reload();
            setStatus("Connected to library_db", STATUS_OK);
        } catch (SQLException ex) {
            setStatus("Database connection failed", STATUS_FAIL);
            showDatabaseError(this, ex);
        }
    }

    void setStatus(String message, Color color) {
        statusLabel.setText(message);
        statusLabel.setForeground(color);
    }

    // Database access

    /**
     * Opens a new connection. Every caller wraps this in try-with-resources,
     * so connections are never left open.
     *
     * Connector/J registers its driver automatically through the service
     * loader, so Class.forName("com.mysql.cj.jdbc.Driver") is not needed
     * on any modern JDBC driver.
     */
    private static Connection openConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }

    // Models

    static class Book {
        final int id;
        final String title;
        final String author;
        final String isbn;
        final int totalCopies;
        final int availableCopies;

        Book(int id, String title, String author, String isbn, int totalCopies, int availableCopies) {
            this.id = id;
            this.title = title;
            this.author = author;
            this.isbn = isbn;
            this.totalCopies = totalCopies;
            this.availableCopies = availableCopies;
        }

        /** Shown inside the borrow combo box. */
        @Override
        public String toString() {
            return title + "  (" + availableCopies + " available)";
        }
    }

    static class User {
        final int id;
        final String name;
        final String email;
        final LocalDate joinedAt;

        User(int id, String name, String email, LocalDate joinedAt) {
            this.id = id;
            this.name = name;
            this.email = email;
            this.joinedAt = joinedAt;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    static class Loan {
        final int id;
        final int bookId;
        final String bookTitle;
        final String userName;
        final Timestamp borrowedAt;
        final Timestamp returnedAt;

        Loan(int id, int bookId, String bookTitle, String userName,
             Timestamp borrowedAt, Timestamp returnedAt) {
            this.id = id;
            this.bookId = bookId;
            this.bookTitle = bookTitle;
            this.userName = userName;
            this.borrowedAt = borrowedAt;
            this.returnedAt = returnedAt;
        }

        boolean isActive() {
            return returnedAt == null;
        }
    }

    // Data access objects 

    /**
     * All statements are PreparedStatement, never string concatenation.
     * A title like O'Reilly would break a concatenated query and, worse,
     * would leave the application open to SQL injection.
     */
    static class BookDao {

        static List<Book> findAll() throws SQLException {
            String sql = "SELECT book_id, title, author, isbn, total_copies, available_copies "
                    + "FROM books ORDER BY book_id";
            List<Book> books = new ArrayList<>();

            try (Connection conn = openConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {

                while (rs.next()) {
                    books.add(new Book(
                            rs.getInt("book_id"),
                            rs.getString("title"),
                            rs.getString("author"),
                            rs.getString("isbn"),
                            rs.getInt("total_copies"),
                            rs.getInt("available_copies")));
                }
            }
            return books;
        }

        static void insert(String title, String author, String isbn, int copies) throws SQLException {
            String sql = "INSERT INTO books (title, author, isbn, total_copies, available_copies) "
                    + "VALUES (?, ?, ?, ?, ?)";

            try (Connection conn = openConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setString(1, title);
                ps.setString(2, author);
                ps.setString(3, isbn);
                ps.setInt(4, copies);
                ps.setInt(5, copies);
                ps.executeUpdate();
            }
        }

        /**
         * Updating total_copies has to keep available_copies consistent.
         * If two of five copies are on loan and the total drops to three,
         * exactly one should remain available.
         */
        static void update(int bookId, String title, String author, String isbn, int newTotal)
                throws SQLException {

            String sql = "UPDATE books SET title = ?, author = ?, isbn = ?, "
                    + "available_copies = available_copies + (? - total_copies), "
                    + "total_copies = ? WHERE book_id = ?";

            try (Connection conn = openConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setString(1, title);
                ps.setString(2, author);
                ps.setString(3, isbn);
                ps.setInt(4, newTotal);
                ps.setInt(5, newTotal);
                ps.setInt(6, bookId);
                ps.executeUpdate();
            }
        }

        static void delete(int bookId) throws SQLException {
            try (Connection conn = openConnection()) {

                String check = "SELECT COUNT(*) FROM transactions WHERE book_id = ?";
                try (PreparedStatement ps = conn.prepareStatement(check)) {
                    ps.setInt(1, bookId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next() && rs.getInt(1) > 0) {
                            throw new SQLException("This book has loan history and cannot be deleted.");
                        }
                    }
                }

                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM books WHERE book_id = ?")) {
                    ps.setInt(1, bookId);
                    ps.executeUpdate();
                }
            }
        }
    }

    static class UserDao {

        static List<User> findAll() throws SQLException {
            String sql = "SELECT user_id, name, email, joined_at FROM users ORDER BY user_id";
            List<User> users = new ArrayList<>();

            try (Connection conn = openConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {

                while (rs.next()) {
                    users.add(new User(
                            rs.getInt("user_id"),
                            rs.getString("name"),
                            rs.getString("email"),
                            rs.getDate("joined_at").toLocalDate()));
                }
            }
            return users;
        }

        static void insert(String name, String email) throws SQLException {
            String sql = "INSERT INTO users (name, email, joined_at) VALUES (?, ?, ?)";

            try (Connection conn = openConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setString(1, name);
                ps.setString(2, email);
                ps.setDate(3, java.sql.Date.valueOf(LocalDate.now()));
                ps.executeUpdate();
            }
        }

        static void update(int userId, String name, String email) throws SQLException {
            String sql = "UPDATE users SET name = ?, email = ? WHERE user_id = ?";

            try (Connection conn = openConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setString(1, name);
                ps.setString(2, email);
                ps.setInt(3, userId);
                ps.executeUpdate();
            }
        }

        static void delete(int userId) throws SQLException {
            try (Connection conn = openConnection()) {

                String check = "SELECT COUNT(*) FROM transactions WHERE user_id = ?";
                try (PreparedStatement ps = conn.prepareStatement(check)) {
                    ps.setInt(1, userId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next() && rs.getInt(1) > 0) {
                            throw new SQLException("This user has loan history and cannot be deleted.");
                        }
                    }
                }

                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM users WHERE user_id = ?")) {
                    ps.setInt(1, userId);
                    ps.executeUpdate();
                }
            }
        }
    }

    static class LoanDao {

        static List<Loan> findAll() throws SQLException {
            String sql = "SELECT t.transaction_id, t.book_id, b.title, u.name, "
                    + "t.borrowed_at, t.returned_at "
                    + "FROM transactions t "
                    + "JOIN books b ON b.book_id = t.book_id "
                    + "JOIN users u ON u.user_id = t.user_id "
                    + "ORDER BY t.returned_at IS NOT NULL, t.transaction_id DESC";

            List<Loan> loans = new ArrayList<>();

            try (Connection conn = openConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {

                while (rs.next()) {
                    loans.add(new Loan(
                            rs.getInt("transaction_id"),
                            rs.getInt("book_id"),
                            rs.getString("title"),
                            rs.getString("name"),
                            rs.getTimestamp("borrowed_at"),
                            rs.getTimestamp("returned_at")));
                }
            }
            return loans;
        }

        /**
         * Borrowing touches two tables, so both statements run in one
         * transaction: either the loan record and the decremented counter
         * both land, or neither does.
         *
         * SELECT ... FOR UPDATE locks the book row until commit, so two
         * simultaneous clicks cannot both claim the same last copy.
         */

        static void borrow(int bookId, int userId) throws SQLException {
            Connection conn = null;
            try {
                conn = openConnection();
                conn.setAutoCommit(false);

                int available;
                String lockSql = "SELECT available_copies FROM books WHERE book_id = ? FOR UPDATE";
                try (PreparedStatement ps = conn.prepareStatement(lockSql)) {
                    ps.setInt(1, bookId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            throw new SQLException("Book no longer exists.");
                        }
                        available = rs.getInt(1);
                    }
                }

                if (available <= 0) {
                    throw new SQLException("No copies of this book are available.");
                }

                String decSql = "UPDATE books SET available_copies = available_copies - 1 "
                        + "WHERE book_id = ?";
                try (PreparedStatement ps = conn.prepareStatement(decSql)) {
                    ps.setInt(1, bookId);
                    ps.executeUpdate();
                }

                String insSql = "INSERT INTO transactions (book_id, user_id, borrowed_at) "
                        + "VALUES (?, ?, NOW())";
                try (PreparedStatement ps = conn.prepareStatement(insSql)) {
                    ps.setInt(1, bookId);
                    ps.setInt(2, userId);
                    ps.executeUpdate();
                }

                conn.commit();

            } catch (SQLException ex) {
                rollbackQuietly(conn);
                throw ex;
            } finally {
                closeQuietly(conn);
            }
        }

        /**
         * Returning is the mirror image. The UPDATE guards on
         * "returned_at IS NULL", so a double click cannot return the same
         * loan twice and inflate available_copies.
         */
        static void returnLoan(int transactionId, int bookId) throws SQLException {
            Connection conn = null;
            try {
                conn = openConnection();
                conn.setAutoCommit(false);

                String updSql = "UPDATE transactions SET returned_at = NOW() "
                        + "WHERE transaction_id = ? AND returned_at IS NULL";

                int rowsAffected;
                try (PreparedStatement ps = conn.prepareStatement(updSql)) {
                    ps.setInt(1, transactionId);
                    rowsAffected = ps.executeUpdate();
                }

                if (rowsAffected == 0) {
                    throw new SQLException("This loan has already been returned.");
                }

                String incSql = "UPDATE books SET available_copies = available_copies + 1 "
                        + "WHERE book_id = ?";
                try (PreparedStatement ps = conn.prepareStatement(incSql)) {
                    ps.setInt(1, bookId);
                    ps.executeUpdate();
                }

                conn.commit();

            } catch (SQLException ex) {
                rollbackQuietly(conn);
                throw ex;
            } finally {
                closeQuietly(conn);
            }
        }

        private static void rollbackQuietly(Connection conn) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ignored) {
                    // The original exception is more useful than a rollback failure
                }
            }
        }

        private static void closeQuietly(Connection conn) {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException ignored) {
                    // Nothing useful left to do at this point
                }
            }
        }
    }

    // Books tab 
    static class BooksPanel extends JPanel {

        private final Task1_LibraryManagementSystem frame;
        private final LoansPanel loansPanel;

        private final DefaultTableModel model;
        private final JTable table;
        private final JTextField titleField = makeTextField();
        private final JTextField authorField = makeTextField();
        private final JTextField isbnField = makeTextField();
        private final JTextField copiesField = makeTextField();

        private List<Book> books = new ArrayList<>();

        BooksPanel(Task1_LibraryManagementSystem frame, LoansPanel loansPanel) {
            this.frame = frame;
            this.loansPanel = loansPanel;

            setLayout(new BorderLayout(0, 16));
            setBackground(BG);
            setBorder(new EmptyBorder(18, 0, 0, 0));

            model = new DefaultTableModel(
                    new String[]{"ID", "Title", "Author", "ISBN", "Total", "Available"}, 0) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };

            table = makeTable(model);
            table.getSelectionModel().addListSelectionListener(e -> {
                if (!e.getValueIsAdjusting()) {
                    fillFormFromSelection();
                }
            });

            add(wrapTable(table), BorderLayout.CENTER);
            add(buildForm(), BorderLayout.SOUTH);
        }

        private JPanel buildForm() {
            JPanel fields = new JPanel(new GridLayout(2, 4, 12, 6));
            fields.setBackground(BG);
            fields.add(makeFormLabel("Title"));
            fields.add(makeFormLabel("Author"));
            fields.add(makeFormLabel("ISBN"));
            fields.add(makeFormLabel("Total copies"));
            fields.add(titleField);
            fields.add(authorField);
            fields.add(isbnField);
            fields.add(copiesField);

            FlatButton addButton = new FlatButton("Add", FlatButton.Variant.PRIMARY);
            addButton.addActionListener(e -> addBook());

            FlatButton updateButton = new FlatButton("Update", FlatButton.Variant.SECONDARY);
            updateButton.addActionListener(e -> updateBook());

            FlatButton deleteButton = new FlatButton("Delete", FlatButton.Variant.DANGER);
            deleteButton.addActionListener(e -> deleteBook());

            FlatButton clearButton = new FlatButton("Clear", FlatButton.Variant.SECONDARY);
            clearButton.addActionListener(e -> clearForm());

            JPanel buttons = new JPanel(new GridLayout(1, 4, 8, 0));
            buttons.setBackground(BG);
            buttons.add(addButton);
            buttons.add(updateButton);
            buttons.add(deleteButton);
            buttons.add(clearButton);

            JPanel form = new JPanel(new BorderLayout(0, 12));
            form.setBackground(BG);
            form.add(fields, BorderLayout.CENTER);
            form.add(buttons, BorderLayout.SOUTH);
            return form;
        }

        void reload() throws SQLException {
            books = BookDao.findAll();
            model.setRowCount(0);
            for (Book book : books) {
                model.addRow(new Object[]{
                        book.id, book.title, book.author, book.isbn,
                        book.totalCopies, book.availableCopies});
            }
        }

        List<Book> getBooks() {
            return books;
        }

        private void addBook() {
            String title = titleField.getText().trim();
            String author = authorField.getText().trim();
            String isbn = isbnField.getText().trim();

            if (title.isEmpty() || author.isEmpty() || isbn.isEmpty()) {
                showError(frame, "Title, author, and ISBN are required.");
                return;
            }

            Integer copies = parseCopies();
            if (copies == null) {
                return;
            }

            try {
                BookDao.insert(title, author, isbn, copies);
                reload();
                loansPanel.reload();
                clearForm();
                frame.setStatus("Book added: " + title, STATUS_OK);
            } catch (SQLException ex) {
                frame.setStatus("Insert failed", STATUS_FAIL);
                showDatabaseError(frame, ex);
            }
        }

        private void updateBook() {
            int row = table.getSelectedRow();
            if (row == -1) {
                showError(frame, "Select a book from the table first.");
                return;
            }

            String title = titleField.getText().trim();
            String author = authorField.getText().trim();
            String isbn = isbnField.getText().trim();

            if (title.isEmpty() || author.isEmpty() || isbn.isEmpty()) {
                showError(frame, "Title, author, and ISBN are required.");
                return;
            }

            Integer copies = parseCopies();
            if (copies == null) {
                return;
            }

            Book book = books.get(row);
            int onLoan = book.totalCopies - book.availableCopies;
            if (copies < onLoan) {
                showError(frame, onLoan + " copies are currently on loan. "
                        + "Total copies cannot be lower than that.");
                return;
            }

            try {
                BookDao.update(book.id, title, author, isbn, copies);
                reload();
                loansPanel.reload();
                clearForm();
                frame.setStatus("Book updated: " + title, STATUS_OK);
            } catch (SQLException ex) {
                frame.setStatus("Update failed", STATUS_FAIL);
                showDatabaseError(frame, ex);
            }
        }

        private void deleteBook() {
            int row = table.getSelectedRow();
            if (row == -1) {
                showError(frame, "Select a book from the table first.");
                return;
            }

            Book book = books.get(row);
            int confirm = JOptionPane.showConfirmDialog(frame,
                    "Delete \"" + book.title + "\"?",
                    "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

            if (confirm != JOptionPane.YES_OPTION) {
                return;
            }

            try {
                BookDao.delete(book.id);
                reload();
                loansPanel.reload();
                clearForm();
                frame.setStatus("Book deleted: " + book.title, STATUS_OK);
            } catch (SQLException ex) {
                frame.setStatus("Delete failed", STATUS_FAIL);
                showDatabaseError(frame, ex);
            }
        }

        private Integer parseCopies() {
            try {
                int copies = Integer.parseInt(copiesField.getText().trim());
                if (copies < 1) {
                    showError(frame, "Total copies must be at least 1.");
                    return null;
                }
                return copies;
            } catch (NumberFormatException ex) {
                showError(frame, "Total copies must be a whole number.");
                return null;
            }
        }

        private void fillFormFromSelection() {
            int row = table.getSelectedRow();
            if (row == -1 || row >= books.size()) {
                return;
            }
            Book book = books.get(row);
            titleField.setText(book.title);
            authorField.setText(book.author);
            isbnField.setText(book.isbn);
            copiesField.setText(String.valueOf(book.totalCopies));
        }

        private void clearForm() {
            titleField.setText("");
            authorField.setText("");
            isbnField.setText("");
            copiesField.setText("");
            table.clearSelection();
        }
    }

    // Users tab 
    static class UsersPanel extends JPanel {

        private final Task1_LibraryManagementSystem frame;
        private final LoansPanel loansPanel;

        private final DefaultTableModel model;
        private final JTable table;
        private final JTextField nameField = makeTextField();
        private final JTextField emailField = makeTextField();

        private List<User> users = new ArrayList<>();

        UsersPanel(Task1_LibraryManagementSystem frame, LoansPanel loansPanel) {
            this.frame = frame;
            this.loansPanel = loansPanel;

            setLayout(new BorderLayout(0, 16));
            setBackground(BG);
            setBorder(new EmptyBorder(18, 0, 0, 0));

            model = new DefaultTableModel(new String[]{"ID", "Name", "Email", "Joined"}, 0) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };

            table = makeTable(model);
            table.getSelectionModel().addListSelectionListener(e -> {
                if (!e.getValueIsAdjusting()) {
                    fillFormFromSelection();
                }
            });

            add(wrapTable(table), BorderLayout.CENTER);
            add(buildForm(), BorderLayout.SOUTH);
        }

        private JPanel buildForm() {
            JPanel fields = new JPanel(new GridLayout(2, 2, 12, 6));
            fields.setBackground(BG);
            fields.add(makeFormLabel("Name"));
            fields.add(makeFormLabel("Email"));
            fields.add(nameField);
            fields.add(emailField);

            FlatButton addButton = new FlatButton("Add", FlatButton.Variant.PRIMARY);
            addButton.addActionListener(e -> addUser());

            FlatButton updateButton = new FlatButton("Update", FlatButton.Variant.SECONDARY);
            updateButton.addActionListener(e -> updateUser());

            FlatButton deleteButton = new FlatButton("Delete", FlatButton.Variant.DANGER);
            deleteButton.addActionListener(e -> deleteUser());

            FlatButton clearButton = new FlatButton("Clear", FlatButton.Variant.SECONDARY);
            clearButton.addActionListener(e -> clearForm());

            JPanel buttons = new JPanel(new GridLayout(1, 4, 8, 0));
            buttons.setBackground(BG);
            buttons.add(addButton);
            buttons.add(updateButton);
            buttons.add(deleteButton);
            buttons.add(clearButton);

            JPanel form = new JPanel(new BorderLayout(0, 12));
            form.setBackground(BG);
            form.add(fields, BorderLayout.CENTER);
            form.add(buttons, BorderLayout.SOUTH);
            return form;
        }

        void reload() throws SQLException {
            users = UserDao.findAll();
            model.setRowCount(0);
            for (User user : users) {
                model.addRow(new Object[]{user.id, user.name, user.email, user.joinedAt});
            }
        }

        List<User> getUsers() {
            return users;
        }

        private void addUser() {
            String name = nameField.getText().trim();
            String email = emailField.getText().trim();

            if (name.isEmpty() || email.isEmpty()) {
                showError(frame, "Name and email are required.");
                return;
            }
            if (!email.contains("@") || !email.contains(".")) {
                showError(frame, "Enter a valid email address.");
                return;
            }

            try {
                UserDao.insert(name, email);
                reload();
                loansPanel.reload();
                clearForm();
                frame.setStatus("User added: " + name, STATUS_OK);
            } catch (SQLException ex) {
                frame.setStatus("Insert failed", STATUS_FAIL);
                showDatabaseError(frame, ex);
            }
        }

        private void updateUser() {
            int row = table.getSelectedRow();
            if (row == -1) {
                showError(frame, "Select a user from the table first.");
                return;
            }

            String name = nameField.getText().trim();
            String email = emailField.getText().trim();

            if (name.isEmpty() || email.isEmpty()) {
                showError(frame, "Name and email are required.");
                return;
            }

            try {
                UserDao.update(users.get(row).id, name, email);
                reload();
                loansPanel.reload();
                clearForm();
                frame.setStatus("User updated: " + name, STATUS_OK);
            } catch (SQLException ex) {
                frame.setStatus("Update failed", STATUS_FAIL);
                showDatabaseError(frame, ex);
            }
        }

        private void deleteUser() {
            int row = table.getSelectedRow();
            if (row == -1) {
                showError(frame, "Select a user from the table first.");
                return;
            }

            User user = users.get(row);
            int confirm = JOptionPane.showConfirmDialog(frame,
                    "Delete user \"" + user.name + "\"?",
                    "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

            if (confirm != JOptionPane.YES_OPTION) {
                return;
            }

            try {
                UserDao.delete(user.id);
                reload();
                loansPanel.reload();
                clearForm();
                frame.setStatus("User deleted: " + user.name, STATUS_OK);
            } catch (SQLException ex) {
                frame.setStatus("Delete failed", STATUS_FAIL);
                showDatabaseError(frame, ex);
            }
        }

        private void fillFormFromSelection() {
            int row = table.getSelectedRow();
            if (row == -1 || row >= users.size()) {
                return;
            }
            nameField.setText(users.get(row).name);
            emailField.setText(users.get(row).email);
        }

        private void clearForm() {
            nameField.setText("");
            emailField.setText("");
            table.clearSelection();
        }
    }

    // Loans tab

    static class LoansPanel extends JPanel {

        private final Task1_LibraryManagementSystem frame;
        private BooksPanel booksPanel;
        private UsersPanel usersPanel;

        private final DefaultTableModel model;
        private final JTable table;
        private final JComboBox<Book> bookCombo = new JComboBox<>();
        private final JComboBox<User> userCombo = new JComboBox<>();

        private List<Loan> loans = new ArrayList<>();

        LoansPanel(Task1_LibraryManagementSystem frame) {
            this.frame = frame;

            setLayout(new BorderLayout(0, 16));
            setBackground(BG);
            setBorder(new EmptyBorder(18, 0, 0, 0));

            model = new DefaultTableModel(
                    new String[]{"ID", "Book", "Borrower", "Borrowed at", "Returned at", "Status"}, 0) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };

            table = makeTable(model);

            add(wrapTable(table), BorderLayout.CENTER);
            add(buildForm(), BorderLayout.SOUTH);
        }

        void setSourcePanels(BooksPanel booksPanel, UsersPanel usersPanel) {
            this.booksPanel = booksPanel;
            this.usersPanel = usersPanel;
        }

        private JPanel buildForm() {
            styleCombo(bookCombo);
            styleCombo(userCombo);

            JPanel fields = new JPanel(new GridLayout(2, 2, 12, 6));
            fields.setBackground(BG);
            fields.add(makeFormLabel("Book"));
            fields.add(makeFormLabel("Borrower"));
            fields.add(bookCombo);
            fields.add(userCombo);

            FlatButton borrowButton = new FlatButton("Borrow", FlatButton.Variant.PRIMARY);
            borrowButton.addActionListener(e -> borrowBook());

            FlatButton returnButton = new FlatButton("Return", FlatButton.Variant.SECONDARY);
            returnButton.addActionListener(e -> returnBook());

            FlatButton refreshButton = new FlatButton("Refresh", FlatButton.Variant.SECONDARY);
            refreshButton.addActionListener(e -> refreshAll());

            JPanel buttons = new JPanel(new GridLayout(1, 3, 8, 0));
            buttons.setBackground(BG);
            buttons.add(borrowButton);
            buttons.add(returnButton);
            buttons.add(refreshButton);

            JPanel form = new JPanel(new BorderLayout(0, 12));
            form.setBackground(BG);
            form.add(fields, BorderLayout.CENTER);
            form.add(buttons, BorderLayout.SOUTH);
            return form;
        }

        void reload() throws SQLException {
            loans = LoanDao.findAll();
            model.setRowCount(0);
            for (Loan loan : loans) {
                model.addRow(new Object[]{
                        loan.id,
                        loan.bookTitle,
                        loan.userName,
                        formatTimestamp(loan.borrowedAt),
                        formatTimestamp(loan.returnedAt),
                        loan.isActive() ? "On loan" : "Returned"});
            }
            reloadCombos();
        }

        /** Rebuilds the dropdowns so availability counts stay in step with the tables. */
        private void reloadCombos() throws SQLException {
            if (booksPanel == null || usersPanel == null) {
                return;
            }

            bookCombo.removeAllItems();
            for (Book book : booksPanel.getBooks()) {
                bookCombo.addItem(book);
            }

            userCombo.removeAllItems();
            for (User user : usersPanel.getUsers()) {
                userCombo.addItem(user);
            }
        }

        private void borrowBook() {
            Book book = (Book) bookCombo.getSelectedItem();
            User user = (User) userCombo.getSelectedItem();

            if (book == null || user == null) {
                showError(frame, "Select both a book and a borrower.");
                return;
            }
            if (book.availableCopies <= 0) {
                showError(frame, "\"" + book.title + "\" has no copies available.");
                return;
            }

            try {
                LoanDao.borrow(book.id, user.id);
                refreshAll();
                frame.setStatus(user.name + " borrowed " + book.title, STATUS_OK);
            } catch (SQLException ex) {
                frame.setStatus("Borrow failed, changes rolled back", STATUS_FAIL);
                showDatabaseError(frame, ex);
            }
        }

        private void returnBook() {
            int row = table.getSelectedRow();
            if (row == -1) {
                showError(frame, "Select a loan from the table first.");
                return;
            }

            Loan loan = loans.get(row);
            if (!loan.isActive()) {
                showError(frame, "This loan was already returned.");
                return;
            }

            try {
                LoanDao.returnLoan(loan.id, loan.bookId);
                refreshAll();
                frame.setStatus(loan.bookTitle + " returned", STATUS_OK);
            } catch (SQLException ex) {
                frame.setStatus("Return failed, changes rolled back", STATUS_FAIL);
                showDatabaseError(frame, ex);
            }
        }

        /** Borrowing changes books, users stay put, loans change. Reload what moved. */
        private void refreshAll() {
            try {
                if (booksPanel != null) {
                    booksPanel.reload();
                }
                reload();
            } catch (SQLException ex) {
                frame.setStatus("Refresh failed", STATUS_FAIL);
                showDatabaseError(frame, ex);
            }
        }

        private static String formatTimestamp(Timestamp timestamp) {
            return timestamp == null ? "\u2014" : timestamp.toLocalDateTime().format(DATE_FMT);
        }
    }

    // Shared UI helpers 
    private static JTable makeTable(DefaultTableModel model) {
        JTable table = new JTable(model);
        table.setBackground(SURFACE);
        table.setForeground(TEXT);
        table.setShowVerticalLines(false);
        table.setGridColor(BORDER);
        table.setRowHeight(29);
        table.setFont(FONT_TABLE);
        table.setSelectionBackground(SURFACE_ALT);
        table.setSelectionForeground(TEXT);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setAutoCreateRowSorter(false);   // row index must match the backing list

        JTableHeader header = table.getTableHeader();
        header.setBackground(SURFACE_ALT);
        header.setForeground(TEXT_MUTED);
        header.setReorderingAllowed(false);
        header.setPreferredSize(new Dimension(0, 32));
        header.setBorder(new MatteBorder(0, 0, 1, 0, BORDER));

        DefaultTableCellRenderer headerRenderer = new DefaultTableCellRenderer();
        headerRenderer.setBackground(SURFACE_ALT);
        headerRenderer.setForeground(TEXT_MUTED);
        headerRenderer.setFont(tracked(new Font("Segoe UI", Font.PLAIN, 11), 0.10));
        headerRenderer.setHorizontalAlignment(SwingConstants.CENTER);

        DefaultTableCellRenderer centered = new DefaultTableCellRenderer();
        centered.setHorizontalAlignment(SwingConstants.CENTER);

        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setHeaderRenderer(headerRenderer);
            table.getColumnModel().getColumn(i).setCellRenderer(centered);
        }
        table.getColumnModel().getColumn(0).setMaxWidth(56);

        return table;
    }

    private static JScrollPane wrapTable(JTable table) {
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.getViewport().setBackground(SURFACE);
        scrollPane.setBorder(BorderFactory.createLineBorder(BORDER, 1));
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        return scrollPane;
    }

    private static JLabel makeFormLabel(String text) {
        JLabel label = new JLabel(text.toUpperCase());
        label.setFont(tracked(new Font("Segoe UI", Font.PLAIN, 11), 0.10));
        label.setForeground(TEXT_MUTED);
        return label;
    }

    private static JTextField makeTextField() {
        JTextField field = new JTextField();
        field.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        field.setBackground(SURFACE);
        field.setForeground(TEXT);
        field.setCaretColor(TEXT);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_STRONG, 1),
                new EmptyBorder(8, 10, 8, 10)));
        return field;
    }

    private static void styleCombo(JComboBox<?> combo) {
        combo.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        combo.setBackground(SURFACE);
        combo.setForeground(TEXT);
        combo.setBorder(BorderFactory.createLineBorder(BORDER_STRONG, 1));
    }

    private static void showError(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Input Error", JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Turns raw SQLException detail into something a user can act on.
     * A duplicate ISBN and a dead database need very different responses.
     */
    private static void showDatabaseError(Component parent, SQLException ex) {
        String message;

        if (ex.getSQLState() != null && ex.getSQLState().startsWith("23")) {
            message = "That value already exists, or it is referenced elsewhere.\n\n"
                    + ex.getMessage();
        } else if (ex.getSQLState() != null && ex.getSQLState().startsWith("08")) {
            message = "Cannot reach the database.\n\n"
                    + "Check that MySQL is running, that library_db exists,\n"
                    + "and that DB_USER and DB_PASSWORD are correct.\n\n"
                    + ex.getMessage();
        } else {
            message = ex.getMessage();
        }

        JOptionPane.showMessageDialog(parent, message, "Database Error", JOptionPane.ERROR_MESSAGE);
    }

    private static Font tracked(Font base, double tracking) {
        Map<TextAttribute, Object> attributes = new HashMap<>();
        attributes.put(TextAttribute.TRACKING, tracking);
        return base.deriveFont(attributes);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Task1_LibraryManagementSystem app = new Task1_LibraryManagementSystem();
            app.setVisible(true);
        });
    }

    /** Flat button with three variants, matching the other tasks in this repository. */
    private static class FlatButton extends JButton {

        enum Variant { PRIMARY, SECONDARY, DANGER }

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

            } else if (variant == Variant.DANGER) {
                g2.setColor(hovered ? DANGER_HOVER : DANGER);
                g2.fillRoundRect(0, 0, w, h, ARC, ARC);
                setForeground(Color.WHITE);

            } else {
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
        }
    }
}