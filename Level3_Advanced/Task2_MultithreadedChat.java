import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.font.TextAttribute;
import java.awt.geom.RoundRectangle2D;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Level 3 (Advanced) - Task 2: Multithreaded Chat Application
 * Codveda Technology - Java Development Internship
 *
 * A chat server and client built on Java Sockets. The server accepts many
 * clients at once, giving each its own thread, and broadcasts every message
 * to everyone connected. Run instructions are in README.md.
 */
public class Task2_MultithreadedChat {

    static final int DEFAULT_PORT = 5000;
    static final String DEFAULT_HOST = "localhost";

    static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    // ---- Shared palette ----
    static final Color BG = new Color(0x15, 0x17, 0x1A);
    static final Color SURFACE = new Color(0x1D, 0x20, 0x25);
    static final Color SURFACE_ALT = new Color(0x26, 0x2A, 0x30);
    static final Color BORDER = new Color(0x30, 0x35, 0x3C);
    static final Color BORDER_STRONG = new Color(0x4A, 0x51, 0x5A);

    static final Color TEXT = new Color(0xE6, 0xE8, 0xEB);
    static final Color TEXT_MUTED = new Color(0xA2, 0xA9, 0xB2);
    static final Color TEXT_FAINT = new Color(0x84, 0x8B, 0x94);

    static final Color ACCENT = new Color(0x3D, 0x6E, 0x9E);
    static final Color ACCENT_HOVER = new Color(0x4B, 0x82, 0xB6);
    static final Color DANGER = new Color(0x9E, 0x4A, 0x44);
    static final Color DANGER_HOVER = new Color(0xB5, 0x59, 0x52);
    static final Color STATUS_OK = new Color(0x7C, 0xAD, 0x88);
    static final Color STATUS_FAIL = new Color(0xC4, 0x6B, 0x63);
    static final Color SYSTEM_TINT = new Color(0xC0, 0x93, 0x4F);

    static final Color BTN_SECONDARY = new Color(0x36, 0x3D, 0x45);
    static final Color BTN_SECONDARY_HOVER = new Color(0x43, 0x4B, 0x55);
    static final Color BTN_SECONDARY_BORDER = new Color(0x63, 0x6B, 0x76);

    /** Server console leans on a cool slate; the client leans warmer and softer. */
    static final Color SERVER_TINT = new Color(0x5B, 0x86, 0xB0);

    static final Font FONT_TITLE = new Font("Segoe UI", Font.PLAIN, 19);
    static final Font FONT_BODY = new Font("Segoe UI", Font.PLAIN, 13);
    static final Font FONT_MONO = new Font("Consolas", Font.PLAIN, 13);
    static final Font FONT_MONO_SMALL = new Font("Consolas", Font.PLAIN, 11);

    /** One colour per speaker, picked from the name so it stays stable across windows. */
    static final Color[] SPEAKER_COLORS = {
            new Color(0x6A, 0x9E, 0xC4),
            new Color(0x7F, 0xA8, 0x6B),
            new Color(0xC0, 0x8A, 0x5E),
            new Color(0x9B, 0x84, 0xC4),
            new Color(0x5E, 0xA8, 0xA0),
            new Color(0xC4, 0x7F, 0x93),
    };

    static Color colorFor(String username) {
        int index = Math.floorMod(username.hashCode(), SPEAKER_COLORS.length);
        return SPEAKER_COLORS[index];
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new LauncherWindow().setVisible(true));
    }

    // =================================================================
    //  Launcher
    // =================================================================

    /**
     * Lets one JVM open a server and several clients, which makes the
     * broadcast behaviour easy to see side by side.
     */
    static class LauncherWindow extends JFrame {

        private final JTextField hostField = makeTextField(DEFAULT_HOST);
        private final JTextField portField = makeTextField(String.valueOf(DEFAULT_PORT));
        private final JTextField usernameField = makeTextField("");

        LauncherWindow() {
            setTitle("Chat Launcher - Codveda Internship");
            setSize(470, 480);
            setMinimumSize(new Dimension(440, 460));
            setDefaultCloseOperation(EXIT_ON_CLOSE);
            setLocationRelativeTo(null);
            getContentPane().setBackground(BG);

            setLayout(new BorderLayout());
            ((JPanel) getContentPane()).setBorder(new EmptyBorder(22, 26, 22, 26));

            JLabel title = new JLabel("Multithreaded Chat");
            title.setFont(FONT_TITLE);
            title.setForeground(TEXT);

            JLabel subtitle = new JLabel("Level 3 \u00b7 Task 2 \u2014 sockets, threads, broadcast");
            subtitle.setFont(FONT_BODY);
            subtitle.setForeground(TEXT_MUTED);

            JPanel titleBlock = new JPanel(new GridLayout(2, 1, 0, 4));
            titleBlock.setBackground(BG);
            titleBlock.setBorder(new EmptyBorder(0, 0, 22, 0));
            titleBlock.add(title);
            titleBlock.add(subtitle);

            // BoxLayout lets each row keep its own height. GridLayout would
            // divide the panel evenly and clip the last field.
            JPanel fields = new JPanel();
            fields.setLayout(new BoxLayout(fields, BoxLayout.Y_AXIS));
            fields.setBackground(BG);
            addField(fields, "Host", hostField);
            addField(fields, "Port", portField);
            addField(fields, "Username (clients only)", usernameField);

            FlatButton serverButton = new FlatButton("Start server", FlatButton.Variant.PRIMARY);
            serverButton.addActionListener(e -> startServer());

            FlatButton clientButton = new FlatButton("Join as client", FlatButton.Variant.SECONDARY);
            clientButton.addActionListener(e -> joinAsClient());

            usernameField.addActionListener(e -> joinAsClient());

            JPanel buttons = new JPanel(new GridLayout(1, 2, 8, 0));
            buttons.setBackground(BG);
            buttons.setBorder(new EmptyBorder(10, 0, 0, 0));
            buttons.add(serverButton);
            buttons.add(clientButton);

            JPanel body = new JPanel(new BorderLayout());
            body.setBackground(BG);
            body.add(fields, BorderLayout.NORTH);
            body.add(buttons, BorderLayout.SOUTH);

            add(titleBlock, BorderLayout.NORTH);
            add(body, BorderLayout.CENTER);
        }

        /** Stacks a label above its field and pins the field height. */
        private void addField(JPanel parent, String labelText, JTextField field) {
            JLabel label = makeFormLabel(labelText);
            label.setAlignmentX(Component.LEFT_ALIGNMENT);

            field.setAlignmentX(Component.LEFT_ALIGNMENT);
            int height = field.getPreferredSize().height;
            field.setMaximumSize(new Dimension(Integer.MAX_VALUE, height));

            parent.add(label);
            parent.add(Box.createVerticalStrut(6));
            parent.add(field);
            parent.add(Box.createVerticalStrut(16));
        }

        private void startServer() {
            Integer port = parsePort();
            if (port == null) {
                return;
            }
            new ServerWindow(port).setVisible(true);
        }

        private void joinAsClient() {
            Integer port = parsePort();
            if (port == null) {
                return;
            }

            String username = usernameField.getText().trim();
            if (username.isEmpty()) {
                showError(this, "Enter a username before joining.");
                return;
            }
            if (username.contains(":")) {
                // The wire format is "[time] name: text", so a colon in the
                // name would make the transcript ambiguous.
                showError(this, "Usernames cannot contain a colon.");
                return;
            }

            String host = hostField.getText().trim();
            if (host.isEmpty()) {
                showError(this, "Enter a host address.");
                return;
            }

            new ClientWindow(host, port, username).setVisible(true);
        }

        private Integer parsePort() {
            try {
                int port = Integer.parseInt(portField.getText().trim());
                if (port < 1024 || port > 65535) {
                    showError(this, "Port must be between 1024 and 65535.");
                    return null;
                }
                return port;
            } catch (NumberFormatException ex) {
                showError(this, "Port must be a whole number.");
                return null;
            }
        }
    }

    // =================================================================
    //  Server core
    // =================================================================

    /**
     * Owns the ServerSocket and the list of connected clients.
     *
     * The handler list is a CopyOnWriteArrayList: broadcast() iterates it
     * while other threads may be adding or removing entries. A plain
     * ArrayList would throw ConcurrentModificationException there.
     */
    static class ChatServer {

        private final int port;
        private final Consumer<String> onMessage;
        private final Runnable onRosterChanged;

        private final List<ClientHandler> handlers = new CopyOnWriteArrayList<>();
        private ServerSocket serverSocket;

        /** volatile so the accept loop thread sees the change made by stop(). */
        private volatile boolean running;

        ChatServer(int port, Consumer<String> onMessage, Runnable onRosterChanged) {
            this.port = port;
            this.onMessage = onMessage;
            this.onRosterChanged = onRosterChanged;
        }

        void start() throws IOException {
            serverSocket = new ServerSocket(port);
            running = true;

            // Daemon thread: the JVM can exit without waiting for it
            Thread acceptThread = new Thread(this::acceptLoop, "chat-accept-loop");
            acceptThread.setDaemon(true);
            acceptThread.start();
        }

        /**
         * accept() blocks until a client connects. Each accepted socket gets
         * its own thread, which is what lets several people chat at once
         * instead of queuing behind each other.
         */
        private void acceptLoop() {
            while (running) {
                try {
                    Socket socket = serverSocket.accept();

                    ClientHandler handler = new ClientHandler(socket, this);
                    handlers.add(handler);

                    Thread thread = new Thread(handler, "chat-client-" + socket.getPort());
                    thread.setDaemon(true);
                    thread.start();

                } catch (SocketException ex) {
                    // stop() closes the socket, which makes accept() throw.
                    // Expected during shutdown, so only report it otherwise.
                    if (running) {
                        onMessage.accept(system("Accept failed: " + ex.getMessage()));
                    }
                } catch (IOException ex) {
                    onMessage.accept(system("Accept failed: " + ex.getMessage()));
                }
            }
        }

        /** Sends one line to every connected client, and to the server log. */
        void broadcast(String message) {
            onMessage.accept(message);
            for (ClientHandler handler : handlers) {
                handler.send(message);
            }
        }

        void rosterChanged() {
            onRosterChanged.run();
        }

        void remove(ClientHandler handler) {
            handlers.remove(handler);
            rosterChanged();
        }

        /** Names of clients that finished the handshake, for the roster panel. */
        List<String> connectedUsernames() {
            List<String> names = new ArrayList<>();
            for (ClientHandler handler : handlers) {
                if (handler.joined) {
                    names.add(handler.username);
                }
            }
            return names;
        }

        void stop() {
            running = false;

            for (ClientHandler handler : handlers) {
                handler.close();
            }
            handlers.clear();

            try {
                if (serverSocket != null) {
                    serverSocket.close();   // unblocks accept()
                }
            } catch (IOException ignored) {
                // Shutting down anyway
            }
            rosterChanged();
        }
    }

    /** One instance per connected client, run on its own thread. */
    static class ClientHandler implements Runnable {

        private final Socket socket;
        private final ChatServer server;

        private PrintWriter out;
        private volatile String username;
        private volatile boolean joined;

        ClientHandler(Socket socket, ChatServer server) {
            this.socket = socket;
            this.server = server;
        }

        @Override
        public void run() {
            try (BufferedReader in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {

                // autoFlush = true, otherwise messages sit in the buffer
                out = new PrintWriter(new OutputStreamWriter(
                        socket.getOutputStream(), StandardCharsets.UTF_8), true);

                // The client sends its username as the very first line
                username = in.readLine();
                if (username == null || username.isBlank()) {
                    return;
                }

                joined = true;
                server.rosterChanged();
                server.broadcast(system(username + " joined the chat"));

                String line;
                while ((line = in.readLine()) != null) {
                    if (line.equals("/quit")) {
                        break;
                    }
                    if (!line.isBlank()) {
                        server.broadcast(format(username, line));
                    }
                }

            } catch (IOException ex) {
                // A client that closes its window makes readLine() throw.
                // Normal enough to not deserve a log line of its own.
            } finally {
                server.remove(this);
                close();
                if (joined) {
                    server.broadcast(system(username + " left the chat"));
                }
            }
        }

        void send(String message) {
            if (out != null) {
                out.println(message);
            }
        }

        void close() {
            try {
                socket.close();
            } catch (IOException ignored) {
                // Already gone
            }
        }
    }

    // =================================================================
    //  Server window: an operator console
    // =================================================================

    static class ServerWindow extends JFrame {

        private final int port;
        private final JTextPane logPane = new JTextPane();
        private final DefaultListModel<String> rosterModel = new DefaultListModel<>();
        private final JLabel countLabel = new JLabel();
        private final JLabel messageCountLabel = new JLabel();
        private final StatusDot statusDot = new StatusDot();
        private final FlatButton stopButton;

        private ChatServer server;
        private int messageCount;

        ServerWindow(int port) {
            this.port = port;

            setTitle("Chat Server :" + port + " - Codveda Internship");
            setSize(720, 540);
            setMinimumSize(new Dimension(660, 480));
            setDefaultCloseOperation(DISPOSE_ON_CLOSE);
            setLocationRelativeTo(null);
            getContentPane().setBackground(BG);

            stopButton = new FlatButton("Stop server", FlatButton.Variant.DANGER);
            stopButton.addActionListener(e -> stopServer());

            buildUI();

            addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    stopServer();
                }
            });

            startServer();
        }

        private void buildUI() {
            setLayout(new BorderLayout());
            ((JPanel) getContentPane()).setBorder(new EmptyBorder(18, 22, 16, 22));

            add(buildHeader(), BorderLayout.NORTH);
            add(buildCenter(), BorderLayout.CENTER);
            add(buildFooter(), BorderLayout.SOUTH);
        }

        /** A running-light, a monospaced address, and a hairline. Console furniture. */
        private JPanel buildHeader() {
            JLabel title = new JLabel("Server console");
            title.setFont(FONT_TITLE);
            title.setForeground(TEXT);

            JLabel address = new JLabel("tcp://0.0.0.0:" + port);
            address.setFont(FONT_MONO_SMALL);
            address.setForeground(SERVER_TINT);

            JPanel titleBlock = new JPanel();
            titleBlock.setLayout(new BoxLayout(titleBlock, BoxLayout.Y_AXIS));
            titleBlock.setBackground(BG);
            title.setAlignmentX(Component.LEFT_ALIGNMENT);
            address.setAlignmentX(Component.LEFT_ALIGNMENT);
            titleBlock.add(title);
            titleBlock.add(Box.createVerticalStrut(4));
            titleBlock.add(address);

            JPanel dotRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
            dotRow.setBackground(BG);
            dotRow.add(statusDot);

            JPanel header = new JPanel(new BorderLayout());
            header.setBackground(BG);
            header.setBorder(BorderFactory.createCompoundBorder(
                    new MatteBorder(0, 0, 1, 0, BORDER),
                    new EmptyBorder(0, 0, 14, 0)));
            header.add(titleBlock, BorderLayout.WEST);
            header.add(dotRow, BorderLayout.EAST);
            return header;
        }

        private JPanel buildCenter() {
            logPane.setEditable(false);
            logPane.setBackground(SURFACE);
            logPane.setBorder(new EmptyBorder(10, 12, 10, 12));
            logPane.setFont(FONT_MONO);

            JScrollPane logScroll = new JScrollPane(logPane);
            logScroll.getViewport().setBackground(SURFACE);
            logScroll.setBorder(BorderFactory.createLineBorder(BORDER, 1));
            logScroll.getVerticalScrollBar().setUnitIncrement(16);

            JPanel center = new JPanel(new BorderLayout(16, 0));
            center.setBackground(BG);
            center.setBorder(new EmptyBorder(16, 0, 0, 0));
            center.add(logScroll, BorderLayout.CENTER);
            center.add(buildRoster(), BorderLayout.EAST);
            return center;
        }

        /** Live list of who is attached right now. */
        private JPanel buildRoster() {
            JList<String> rosterList = new JList<>(rosterModel);
            rosterList.setBackground(SURFACE);
            rosterList.setForeground(TEXT);
            rosterList.setFont(FONT_MONO);
            rosterList.setFixedCellHeight(28);
            rosterList.setSelectionBackground(SURFACE);
            rosterList.setSelectionForeground(TEXT);
            rosterList.setBorder(new EmptyBorder(6, 10, 6, 10));
            rosterList.setCellRenderer((list, value, index, selected, focused) -> {
                JLabel label = new JLabel("\u25CF  " + value);
                label.setFont(FONT_MONO);
                label.setForeground(colorFor(value));
                label.setOpaque(false);
                label.setBorder(new EmptyBorder(3, 2, 3, 2));
                return label;
            });

            JLabel caption = makeFormLabel("Connected clients");
            caption.setBorder(new EmptyBorder(0, 0, 8, 0));

            countLabel.setFont(FONT_MONO_SMALL);
            countLabel.setForeground(TEXT_FAINT);
            countLabel.setBorder(new EmptyBorder(8, 2, 0, 0));

            JScrollPane rosterScroll = new JScrollPane(rosterList);
            rosterScroll.getViewport().setBackground(SURFACE);
            rosterScroll.setBorder(BorderFactory.createLineBorder(BORDER, 1));

            JPanel panel = new JPanel(new BorderLayout());
            panel.setBackground(BG);
            panel.setPreferredSize(new Dimension(190, 0));
            panel.add(caption, BorderLayout.NORTH);
            panel.add(rosterScroll, BorderLayout.CENTER);
            panel.add(countLabel, BorderLayout.SOUTH);
            return panel;
        }

        private JPanel buildFooter() {
            messageCountLabel.setFont(FONT_MONO_SMALL);
            messageCountLabel.setForeground(TEXT_FAINT);

            JPanel footer = new JPanel(new BorderLayout(15, 0));
            footer.setBackground(BG);
            footer.setBorder(BorderFactory.createCompoundBorder(
                    new MatteBorder(1, 0, 0, 0, BORDER),
                    new EmptyBorder(14, 0, 0, 0)));
            footer.add(messageCountLabel, BorderLayout.CENTER);
            footer.add(stopButton, BorderLayout.EAST);

            JPanel wrapper = new JPanel(new BorderLayout());
            wrapper.setBackground(BG);
            wrapper.setBorder(new EmptyBorder(16, 0, 0, 0));
            wrapper.add(footer, BorderLayout.CENTER);
            return wrapper;
        }

        private void startServer() {
            // Callbacks arrive on socket threads, so every UI touch is
            // pushed back onto the Event Dispatch Thread.
            server = new ChatServer(port,
                    message -> SwingUtilities.invokeLater(() -> appendLog(message)),
                    () -> SwingUtilities.invokeLater(this::refreshRoster));

            try {
                server.start();
                appendLog(system("Server listening on port " + port));
                statusDot.setState("listening", STATUS_OK);
                refreshRoster();
            } catch (IOException ex) {
                statusDot.setState("failed", STATUS_FAIL);
                stopButton.setEnabled(false);
                showError(this, "Could not start the server on port " + port + ".\n\n"
                        + ex.getMessage()
                        + "\n\nAnother program may already be using this port.");
            }
        }

        private void stopServer() {
            if (server != null) {
                server.stop();
                server = null;
            }
            stopButton.setEnabled(false);
            rosterModel.clear();
            countLabel.setText("0 attached");
            appendLog(system("Server stopped"));
            statusDot.setState("stopped", STATUS_FAIL);
        }

        private void refreshRoster() {
            rosterModel.clear();
            if (server == null) {
                countLabel.setText("0 attached");
                return;
            }
            List<String> names = server.connectedUsernames();
            for (String name : names) {
                rosterModel.addElement(name);
            }
            countLabel.setText(names.size() + " attached");
        }

        /**
         * Writes one log line with the timestamp dimmed and system notices
         * tinted, so joins and departures stand apart from chat traffic.
         */
        private void appendLog(String line) {
            String time = "";
            String body = line;

            int close = line.indexOf("] ");
            if (line.startsWith("[") && close > 0) {
                time = line.substring(0, close + 2);
                body = line.substring(close + 2);
            }

            boolean isSystem = body.startsWith("* ");
            if (!isSystem) {
                messageCount++;
                messageCountLabel.setText(messageCount + " message" + (messageCount == 1 ? "" : "s")
                        + " relayed");
            }

            SimpleAttributeSet timeStyle = new SimpleAttributeSet();
            StyleConstants.setForeground(timeStyle, TEXT_FAINT);

            SimpleAttributeSet bodyStyle = new SimpleAttributeSet();
            StyleConstants.setForeground(bodyStyle, isSystem ? SYSTEM_TINT : TEXT);

            StyledDocument doc = logPane.getStyledDocument();
            try {
                doc.insertString(doc.getLength(), time, timeStyle);
                doc.insertString(doc.getLength(), body + "\n", bodyStyle);
            } catch (Exception ignored) {
                // BadLocationException cannot happen when appending at the end
            }
            logPane.setCaretPosition(doc.getLength());
        }
    }

    /** Small pulsing indicator: a filled circle plus a word. */
    static class StatusDot extends JPanel {

        private Color color = TEXT_FAINT;
        private String text = "starting";

        StatusDot() {
            setOpaque(false);
            setPreferredSize(new Dimension(120, 22));
        }

        void setState(String text, Color color) {
            this.text = text;
            this.color = color;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int cy = getHeight() / 2;

            g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 60));
            g2.fillOval(0, cy - 8, 16, 16);
            g2.setColor(color);
            g2.fillOval(4, cy - 4, 8, 8);

            g2.setFont(FONT_MONO_SMALL);
            g2.setColor(color);
            g2.drawString(text, 22, cy + 4);

            g2.dispose();
        }
    }

    // =================================================================
    //  Client core
    // =================================================================

    /** Holds the socket and a background thread that reads incoming lines. */
    static class ChatClient {

        private final Socket socket;
        private final PrintWriter out;

        ChatClient(String host, int port, String username,
                   Consumer<String> onMessage, Runnable onDisconnect) throws IOException {

            socket = new Socket(host, port);

            out = new PrintWriter(new OutputStreamWriter(
                    socket.getOutputStream(), StandardCharsets.UTF_8), true);

            BufferedReader in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));

            // Announce who we are before anything else
            out.println(username);

            // readLine() blocks. On the Event Dispatch Thread that would
            // freeze the window, so incoming traffic gets its own thread.
            Thread listener = new Thread(() -> {
                try {
                    String line;
                    while ((line = in.readLine()) != null) {
                        onMessage.accept(line);
                    }
                } catch (IOException ex) {
                    // Socket closed, either by us or by the server
                } finally {
                    onDisconnect.run();
                }
            }, "chat-listener");

            listener.setDaemon(true);
            listener.start();
        }

        void send(String text) {
            out.println(text);
        }

        void disconnect() {
            try {
                out.println("/quit");
                socket.close();
            } catch (IOException ignored) {
                // Leaving anyway
            }
        }
    }

    // =================================================================
    //  Client window: a chat app
    // =================================================================

    static class ClientWindow extends JFrame {

        private final String username;

        private final JPanel messagePanel = new JPanel();
        private final JScrollPane messageScroll;
        private final JTextField inputField = makeTextField("");
        private final JLabel statusLabel = new JLabel();
        private final FlatButton sendButton;
        private final FlatButton leaveButton;

        private ChatClient client;

        ClientWindow(String host, int port, String username) {
            this.username = username;

            setTitle(username + " \u2014 Chat - Codveda Internship");
            setSize(520, 600);
            setMinimumSize(new Dimension(460, 520));
            setDefaultCloseOperation(DISPOSE_ON_CLOSE);
            setLocationByPlatform(true);
            getContentPane().setBackground(BG);

            messagePanel.setLayout(new BoxLayout(messagePanel, BoxLayout.Y_AXIS));
            messagePanel.setBackground(SURFACE);
            messagePanel.setBorder(new EmptyBorder(10, 12, 10, 12));

            messageScroll = new JScrollPane(messagePanel);
            messageScroll.getViewport().setBackground(SURFACE);
            messageScroll.setBorder(BorderFactory.createLineBorder(BORDER, 1));
            messageScroll.getVerticalScrollBar().setUnitIncrement(18);

            sendButton = new FlatButton("Send", FlatButton.Variant.PRIMARY);
            sendButton.addActionListener(e -> sendMessage());
            inputField.addActionListener(e -> sendMessage());

            leaveButton = new FlatButton("Leave", FlatButton.Variant.DANGER);
            leaveButton.setPreferredSize(new Dimension(88, 32));
            leaveButton.addActionListener(e -> leaveChat());

            buildUI();

            addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    if (client != null) {
                        client.disconnect();
                    }
                }
            });

            connect(host, port);
        }

        private void buildUI() {
            setLayout(new BorderLayout());
            ((JPanel) getContentPane()).setBorder(new EmptyBorder(18, 22, 16, 22));

            add(buildHeader(), BorderLayout.NORTH);
            add(messageScroll, BorderLayout.CENTER);
            add(buildComposer(), BorderLayout.SOUTH);
        }

        /** An avatar chip in the speaker's own colour, so windows are told apart at a glance. */
        private JPanel buildHeader() {
            Avatar avatar = new Avatar(username);

            JLabel name = new JLabel(username);
            name.setFont(new Font("Segoe UI", Font.PLAIN, 16));
            name.setForeground(TEXT);

            statusLabel.setFont(FONT_MONO_SMALL);
            statusLabel.setForeground(TEXT_FAINT);

            JPanel textBlock = new JPanel();
            textBlock.setLayout(new BoxLayout(textBlock, BoxLayout.Y_AXIS));
            textBlock.setBackground(BG);
            name.setAlignmentX(Component.LEFT_ALIGNMENT);
            statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            textBlock.add(name);
            textBlock.add(Box.createVerticalStrut(3));
            textBlock.add(statusLabel);

            JPanel row = new JPanel(new BorderLayout(12, 0));
            row.setBackground(BG);
            row.add(avatar, BorderLayout.WEST);
            row.add(textBlock, BorderLayout.CENTER);

            JPanel leaveWrapper = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 3));
            leaveWrapper.setBackground(BG);
            leaveWrapper.add(leaveButton);
            row.add(leaveWrapper, BorderLayout.EAST);

            JPanel header = new JPanel(new BorderLayout());
            header.setBackground(BG);
            header.setBorder(BorderFactory.createCompoundBorder(
                    new MatteBorder(0, 0, 1, 0, BORDER),
                    new EmptyBorder(0, 0, 14, 0)));
            header.add(row, BorderLayout.CENTER);
            return header;
        }

        private JPanel buildComposer() {
            JPanel inputRow = new JPanel(new BorderLayout(10, 0));
            inputRow.setBackground(BG);
            inputRow.add(inputField, BorderLayout.CENTER);
            inputRow.add(sendButton, BorderLayout.EAST);

            JPanel composer = new JPanel(new BorderLayout());
            composer.setBackground(BG);
            composer.setBorder(new EmptyBorder(14, 0, 0, 0));
            composer.add(inputRow, BorderLayout.CENTER);
            return composer;
        }

        private void connect(String host, int port) {
            try {
                client = new ChatClient(host, port, username,
                        message -> SwingUtilities.invokeLater(() -> handleIncoming(message)),
                        () -> SwingUtilities.invokeLater(this::onDisconnected));

                statusLabel.setText("connected \u00b7 " + host + ":" + port);
                statusLabel.setForeground(STATUS_OK);

            } catch (IOException ex) {
                statusLabel.setText("offline");
                statusLabel.setForeground(STATUS_FAIL);
                inputField.setEnabled(false);
                sendButton.setEnabled(false);
                leaveButton.setEnabled(false);
                showError(this, "Could not connect to " + host + ":" + port + ".\n\n"
                        + ex.getMessage()
                        + "\n\nMake sure the server is running first.");
            }
        }

        /**
         * Splits "[time] name: text" and "[time] * notice" back apart so the
         * transcript can be drawn as bubbles instead of raw lines. Usernames
         * are validated to contain no colon, so the first ": " is the divider.
         */
        private void handleIncoming(String line) {
            String time = "";
            String rest = line;

            int close = line.indexOf("] ");
            if (line.startsWith("[") && close > 0) {
                time = line.substring(1, close);
                rest = line.substring(close + 2);
            }

            if (rest.startsWith("* ")) {
                addNotice(rest.substring(2));
                return;
            }

            int colon = rest.indexOf(": ");
            if (colon <= 0) {
                addNotice(rest);
                return;
            }

            String sender = rest.substring(0, colon);
            String text = rest.substring(colon + 2);
            addBubble(sender, time, text, sender.equals(username));
        }

        private void addBubble(String sender, String time, String text, boolean self) {
            Bubble bubble = new Bubble(sender, time, text, self);

            JPanel row = new JPanel(new FlowLayout(self ? FlowLayout.RIGHT : FlowLayout.LEFT, 0, 3));
            row.setOpaque(false);
            row.setAlignmentX(Component.LEFT_ALIGNMENT);
            row.add(bubble);
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height));

            messagePanel.add(row);
            scrollToBottom();
        }

        /** Joins and departures render as a centred hairline notice, not a bubble. */
        private void addNotice(String text) {
            JLabel label = new JLabel(text);
            label.setFont(FONT_MONO_SMALL);
            label.setForeground(TEXT_FAINT);
            label.setBorder(new EmptyBorder(5, 10, 5, 10));

            JPanel row = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 4));
            row.setOpaque(false);
            row.setAlignmentX(Component.LEFT_ALIGNMENT);
            row.add(label);
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height));

            messagePanel.add(row);
            scrollToBottom();
        }

        private void scrollToBottom() {
            messagePanel.revalidate();
            SwingUtilities.invokeLater(() -> {
                JScrollBar bar = messageScroll.getVerticalScrollBar();
                bar.setValue(bar.getMaximum());
            });
        }

        private void sendMessage() {
            String text = inputField.getText().trim();
            if (text.isEmpty() || client == null) {
                return;
            }
            client.send(text);
            inputField.setText("");
        }

        /**
         * Leaves without closing the window, so the transcript stays readable.
         * The socket close makes the listener thread finish, which calls
         * onDisconnected() for us.
         */
        private void leaveChat() {
            if (client == null) {
                return;
            }
            client.disconnect();
            client = null;
        }

        /** Called when the listener thread ends: server stopped, or we left. */
        private void onDisconnected() {
            inputField.setEnabled(false);
            sendButton.setEnabled(false);
            leaveButton.setEnabled(false);
            statusLabel.setText("disconnected");
            statusLabel.setForeground(STATUS_FAIL);
            addNotice("Disconnected from the server");
        }
    }

    /** A rounded speech bubble. Own messages sit right and carry the accent fill. */
    static class Bubble extends JPanel {

        private static final int ARC = 12;

        /** Past this width the text wraps; below it, the bubble hugs the text. */
        private static final int MAX_BODY_WIDTH = 260;

        /** Keeps a two-letter message from collapsing around its timestamp. */
        private static final int MIN_BUBBLE_WIDTH = 96;

        private static final Font BODY_FONT = new Font("Segoe UI", Font.PLAIN, 14);

        private final boolean self;

        Bubble(String sender, String time, String text, boolean self) {
            this.self = self;

            setOpaque(false);
            setLayout(new BorderLayout(0, 3));
            setBorder(new EmptyBorder(8, 13, 6, 13));

            // Own messages need no name label: the alignment already says who spoke
            if (!self) {
                JLabel head = new JLabel(sender);
                head.setFont(FONT_MONO_SMALL);
                head.setForeground(colorFor(sender));
                add(head, BorderLayout.NORTH);
            }

            JLabel body = makeBody(text);
            body.setForeground(self ? Color.WHITE : TEXT);
            add(body, BorderLayout.CENTER);

            // The stamp sits bottom-right, quiet enough to ignore while reading
            JLabel stamp = new JLabel(shortTime(time), SwingConstants.RIGHT);
            stamp.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            stamp.setForeground(self ? new Color(0xC2, 0xD6, 0xE8) : TEXT_FAINT);
            add(stamp, BorderLayout.SOUTH);
        }

        /**
         * A fixed CSS width would make every bubble the same size. Measuring the
         * string first means short messages stay small and only long ones get a
         * wrap width imposed on them.
         */
        private static JLabel makeBody(String text) {
            JLabel label = new JLabel();
            label.setFont(BODY_FONT);

            int naturalWidth = label.getFontMetrics(BODY_FONT).stringWidth(text);
            boolean needsWrap = naturalWidth > MAX_BODY_WIDTH;

            String style = needsWrap ? " style='width:" + MAX_BODY_WIDTH + "px'" : "";
            label.setText("<html><body" + style + ">" + escape(text) + "</body></html>");
            return label;
        }

        @Override
        public Dimension getPreferredSize() {
            Dimension size = super.getPreferredSize();
            size.width = Math.max(size.width, MIN_BUBBLE_WIDTH);
            return size;
        }

        /** The wire carries HH:mm:ss; seconds are noise in a conversation. */
        private static String shortTime(String time) {
            return time.length() >= 5 ? time.substring(0, 5) : time;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g2.setColor(self ? ACCENT : SURFACE_ALT);
            g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), ARC, ARC));

            if (!self) {
                g2.setColor(BORDER);
                g2.draw(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, ARC, ARC));
            }

            g2.dispose();
            super.paintComponent(g);
        }

        private static String escape(String text) {
            return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        }
    }

    /** Circular chip holding the speaker's initial, tinted by their name. */
    static class Avatar extends JPanel {

        private final String initial;
        private final Color color;

        Avatar(String username) {
            this.initial = username.substring(0, 1).toUpperCase();
            this.color = colorFor(username);
            setOpaque(false);
            setPreferredSize(new Dimension(38, 38));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g2.setColor(color);
            g2.fillOval(0, 0, 38, 38);

            g2.setFont(new Font("Segoe UI", Font.PLAIN, 17));
            g2.setColor(BG);
            FontMetrics fm = g2.getFontMetrics();
            int x = (38 - fm.stringWidth(initial)) / 2;
            int y = (38 - fm.getHeight()) / 2 + fm.getAscent();
            g2.drawString(initial, x, y);

            g2.dispose();
        }
    }

    // =================================================================
    //  Message formatting
    // =================================================================

    static String format(String username, String message) {
        return "[" + LocalTime.now().format(TIME_FMT) + "] " + username + ": " + message;
    }

    static String system(String message) {
        return "[" + LocalTime.now().format(TIME_FMT) + "] * " + message;
    }

    // =================================================================
    //  Shared UI helpers
    // =================================================================

    static JTextField makeTextField(String initialText) {
        JTextField field = new JTextField(initialText);
        field.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        field.setBackground(SURFACE);
        field.setForeground(TEXT);
        field.setCaretColor(TEXT);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_STRONG, 1),
                new EmptyBorder(8, 10, 8, 10)));
        return field;
    }

    static JLabel makeFormLabel(String text) {
        JLabel label = new JLabel(text.toUpperCase());
        label.setFont(tracked(new Font("Segoe UI", Font.PLAIN, 11), 0.10));
        label.setForeground(TEXT_MUTED);
        return label;
    }

    static void showError(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Chat Error", JOptionPane.ERROR_MESSAGE);
    }

    static Font tracked(Font base, double tracking) {
        Map<TextAttribute, Object> attributes = new HashMap<>();
        attributes.put(TextAttribute.TRACKING, tracking);
        return base.deriveFont(attributes);
    }

    /** Flat button with three variants, matching the other tasks in this repository. */
    static class FlatButton extends JButton {

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
            // Painted in paintComponent so it can react to hover state
        }
    }
}