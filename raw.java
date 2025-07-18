import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.swing.border.*;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.RenderingHints;
import java.sql.*;
import java.util.Vector;
import javax.swing.table.DefaultTableModel;

// Main class that will be executed (must match filename)
public class raw {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ChitFundUI());
    }
}

// Other classes (non-public since they're in the same file)
class ChitFundSystem {
    private String chitFundId;
    private double totalAmount;
    private int numberOfMonths;
    private List<Participant> participants;
    private List<Bid> bids;

    public ChitFundSystem(String chitFundId, double totalAmount, int numberOfMonths) {
        this.chitFundId = chitFundId;
        this.totalAmount = totalAmount;
        this.numberOfMonths = numberOfMonths;
        this.participants = new ArrayList<>();
        this.bids = new ArrayList<>();
    }

    public String getChitFundId() {
        return chitFundId;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public int getNumberOfMonths() {
        return numberOfMonths;
    }

    public List<Participant> getParticipants() {
        return participants;
    }

    public List<Bid> getBids() {
        return bids;
    }

    public void addParticipant(Participant participant) {
        participants.add(participant);
    }

    public void addBid(Bid bid) {
        bids.add(bid);
    }

    public void distributeAmount() {
        if (!bids.isEmpty()) {
            Bid highestBid = bids.stream().max(Comparator.comparing(Bid::getBidAmount)).get();
            Participant winner = highestBid.getParticipant();
            winner.receiveAmount(highestBid.getBidAmount());
            JOptionPane.showMessageDialog(null, "Amount " + highestBid.getBidAmount() +
                    " distributed to: " + winner.getName());
        } else {
            JOptionPane.showMessageDialog(null, "No bids available for distribution.");
        }
    }
}

class Participant {
    private String participantId;
    private double amountReceived;
    public String name;

    public Participant(String participantId, String name) {
        this.participantId = participantId;
        this.name = name;
        this.amountReceived = 0.0;
    }

    public String getParticipantId() {
        return participantId;
    }

    public String getName() {
        return name;
    }

    public double getAmountReceived() {
        return amountReceived;
    }

    public void receiveAmount(double amount) {
        this.amountReceived += amount;
    }

    public void makeBid(ChitFundSystem chitFund, double bidAmount) {
        Bid bid = new Bid(this, bidAmount);
        chitFund.addBid(bid);
    }
}

class Bid {
    private Participant participant;
    private double bidAmount;

    public Bid(Participant participant, double bidAmount) {
        this.participant = participant;
        this.bidAmount = bidAmount;
    }

    public Participant getParticipant() {
        return participant;
    }

    public double getBidAmount() {
        return bidAmount;
    }
}

class ChitFundManagementSystem {
    private List<ChitFundSystem> chitFunds;

    public ChitFundManagementSystem() {
        this.chitFunds = new ArrayList<>();
    }

    public void addChitFund(ChitFundSystem chitFund) {
        chitFunds.add(chitFund);
    }

    public void addParticipantToChitFund(String chitFundId, Participant participant) {
        for (ChitFundSystem chitFund : chitFunds) {
            if (chitFund.getChitFundId().equals(chitFundId)) {
                chitFund.addParticipant(participant);
                break;
            }
        }
    }

    public void conductBidding(String chitFundId) {
        for (ChitFundSystem chitFund : chitFunds) {
            if (chitFund.getChitFundId().equals(chitFundId)) {
                chitFund.distributeAmount();
                break;
            }
        }
    }

    public List<ChitFundSystem> getChitFunds() {
        return chitFunds;
    }
}

class ChitFundUI extends JFrame {
    private ChitFundManagementSystem system;
    private JTextArea outputArea;
    private Connection dbConnection;
    // Attractive color scheme
    private static final Color PRIMARY_COLOR = new Color(41, 128, 185);    // Blue
    private static final Color ACCENT_COLOR = new Color(155, 89, 182);     // Purple
    private static final Color SUCCESS_COLOR = new Color(46, 204, 113);    // Green
    private static final Color WARNING_COLOR = new Color(230, 126, 34);    // Orange
    private static final Color DANGER_COLOR = new Color(231, 76, 60);      // Red
    private static final Color BACKGROUND_COLOR = new Color(236, 240, 241);
    private static final Color TEXT_COLOR = new Color(44, 62, 80);

    public ChitFundUI() {
        system = new ChitFundManagementSystem();
        initializeDatabase();
        loadDataFromDatabase();
        initializeUI();
    }

    private void initializeDatabase() {
        try {
            // Try to load the SQLite JDBC driver
            try {
                Class.forName("org.sqlite.JDBC");
            } catch (ClassNotFoundException e) {
                // If driver not found, show detailed error message
                JOptionPane.showMessageDialog(this, 
                    "SQLite JDBC Driver not found!\n\n" +
                    "Please make sure:\n" +
                    "1. You have downloaded sqlite-jdbc-3.36.0.3.jar\n" +
                    "2. The JAR file is in the 'lib' folder\n" +
                    "3. You are running the program using compile_and_run.bat\n\n" +
                    "Error details: " + e.getMessage(),
                    "Database Error",
                    JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Create a connection to the database
            try {
                dbConnection = DriverManager.getConnection("jdbc:sqlite:chitfund.db");
                
                // Create tables if they don't exist
                Statement stmt = dbConnection.createStatement();
                
                // Create ChitFund table
                stmt.execute("CREATE TABLE IF NOT EXISTS ChitFund (" +
                        "id TEXT PRIMARY KEY, " +
                        "totalAmount REAL, " +
                        "numberOfMonths INTEGER)");
                
                // Create Participant table
                stmt.execute("CREATE TABLE IF NOT EXISTS Participant (" +
                        "id TEXT PRIMARY KEY, " +
                        "name TEXT, " +
                        "amountReceived REAL)");
                
                // Create Bid table
                stmt.execute("CREATE TABLE IF NOT EXISTS Bid (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "chitFundId TEXT, " +
                        "participantId TEXT, " +
                        "bidAmount REAL, " +
                        "FOREIGN KEY(chitFundId) REFERENCES ChitFund(id), " +
                        "FOREIGN KEY(participantId) REFERENCES Participant(id))");
                
                stmt.close();
                
                // Show success message
                System.out.println("Database initialized successfully!");
                
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, 
                    "Database connection failed!\n\n" +
                    "Error details: " + e.getMessage(),
                    "Database Error",
                    JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, 
                "Unexpected error during database initialization!\n\n" +
                "Error details: " + e.getMessage(),
                "Database Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadDataFromDatabase() {
        try {
            // Load all chit funds
            Statement stmt = dbConnection.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM ChitFund");
            while (rs.next()) {
                String id = rs.getString("id");
                double totalAmount = rs.getDouble("totalAmount");
                int numberOfMonths = rs.getInt("numberOfMonths");
                ChitFundSystem chitFund = new ChitFundSystem(id, totalAmount, numberOfMonths);
                system.addChitFund(chitFund);
            }
            rs.close();

            // Load all participants and add to their chit funds
            rs = stmt.executeQuery("SELECT * FROM Participant");
            List<Participant> allParticipants = new ArrayList<>();
            while (rs.next()) {
                String pid = rs.getString("id");
                String name = rs.getString("name");
                double amountReceived = rs.getDouble("amountReceived");
                Participant participant = new Participant(pid, name);
                // Set amountReceived if needed
                for (ChitFundSystem cf : system.getChitFunds()) {
                    cf.addParticipant(participant);
                }
                allParticipants.add(participant);
            }
            rs.close();

            // Load all bids and add to their chit funds
            rs = stmt.executeQuery("SELECT * FROM Bid");
            while (rs.next()) {
                String chitFundId = rs.getString("chitFundId");
                String participantId = rs.getString("participantId");
                double bidAmount = rs.getDouble("bidAmount");
                // Find chit fund and participant
                ChitFundSystem cf = null;
                for (ChitFundSystem c : system.getChitFunds()) {
                    if (c.getChitFundId().equals(chitFundId)) {
                        cf = c;
                        break;
                    }
                }
                Participant participant = null;
                for (Participant p : allParticipants) {
                    if (p.getParticipantId().equals(participantId)) {
                        participant = p;
                        break;
                    }
                }
                if (cf != null && participant != null) {
                    Bid bid = new Bid(participant, bidAmount);
                    cf.addBid(bid);
                }
            }
            rs.close();
            stmt.close();
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error loading data from database: " + e.getMessage());
        }
    }

    private void initializeUI() {
        setTitle("Chit Fund System");
        setSize(800, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(0, 0));
        getContentPane().setBackground(BACKGROUND_COLOR);

        // Title panel with gradient effect
        JPanel titlePanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                int w = getWidth();
                int h = getHeight();
                GradientPaint gp = new GradientPaint(0, 0, PRIMARY_COLOR, w, 0, ACCENT_COLOR);
                g2d.setPaint(gp);
                g2d.fillRect(0, 0, w, h);
            }
        };
        titlePanel.setLayout(new FlowLayout(FlowLayout.CENTER));
        titlePanel.setPreferredSize(new Dimension(800, 50));

        JLabel titleLabel = new JLabel("Chit Fund System");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        titleLabel.setForeground(Color.WHITE);
        titlePanel.add(titleLabel);

        // Main content panel
        JPanel mainPanel = new JPanel(new BorderLayout(5, 5));
        mainPanel.setBackground(BACKGROUND_COLOR);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // Button panel with grid layout
        JPanel buttonPanel = new JPanel(new GridLayout(5, 1, 5, 5));
        buttonPanel.setBackground(BACKGROUND_COLOR);

        // Create colorful buttons
        JButton createChitFundBtn = createColorfulButton("Create Chit Fund", PRIMARY_COLOR);
        JButton addParticipantBtn = createColorfulButton("Add Participant", ACCENT_COLOR);
        JButton makeBidBtn = createColorfulButton("Make Bid", SUCCESS_COLOR);
        JButton conductBiddingBtn = createColorfulButton("Conduct Bidding", WARNING_COLOR);
        JButton displayInfoBtn = createColorfulButton("Display Information", DANGER_COLOR);
        JButton showDatabaseBtn = createColorfulButton("Show Database Data", new Color(142, 68, 173));

        // Add buttons to panel
        buttonPanel.add(createChitFundBtn);
        buttonPanel.add(addParticipantBtn);
        buttonPanel.add(makeBidBtn);
        buttonPanel.add(conductBiddingBtn);
        buttonPanel.add(displayInfoBtn);
        buttonPanel.add(showDatabaseBtn);

        // Add action listeners
        createChitFundBtn.addActionListener(e -> createChitFund());
        addParticipantBtn.addActionListener(e -> addParticipant());
        makeBidBtn.addActionListener(e -> makeBid());
        conductBiddingBtn.addActionListener(e -> conductBidding());
        displayInfoBtn.addActionListener(e -> displayInformation());
        showDatabaseBtn.addActionListener(e -> showDatabaseData());

        // Output area with modern styling
        outputArea = new JTextArea();
        outputArea.setEditable(false);
        outputArea.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        outputArea.setBackground(Color.WHITE);
        outputArea.setForeground(TEXT_COLOR);
        outputArea.setLineWrap(true);
        outputArea.setWrapStyleWord(true);
        
        JScrollPane scrollPane = new JScrollPane(outputArea);
        scrollPane.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(PRIMARY_COLOR, 1),
            BorderFactory.createEmptyBorder(2, 2, 2, 2)
        ));

        // Add components
        mainPanel.add(buttonPanel, BorderLayout.WEST);
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        add(titlePanel, BorderLayout.NORTH);
        add(mainPanel, BorderLayout.CENTER);

        setLocationRelativeTo(null);
        setVisible(true);
    }

    private JButton createColorfulButton(String text, Color color) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.BOLD, 14));
        button.setForeground(Color.WHITE);
        button.setBackground(color);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(180, 40));

        // Modern hover effect
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(color.darker());
            }

            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(color);
            }
        });

        return button;
    }

    private void createChitFund() {
        String id = JOptionPane.showInputDialog("Enter Chit Fund ID:");
        String amountStr = JOptionPane.showInputDialog("Enter Total Amount:");
        String monthsStr = JOptionPane.showInputDialog("Enter Number of Months:");

        try {
            double amount = Double.parseDouble(amountStr);
            int months = Integer.parseInt(monthsStr);
            
            // Save to database
            PreparedStatement pstmt = dbConnection.prepareStatement(
                "INSERT INTO ChitFund (id, totalAmount, numberOfMonths) VALUES (?, ?, ?)");
            pstmt.setString(1, id);
            pstmt.setDouble(2, amount);
            pstmt.setInt(3, months);
            pstmt.executeUpdate();
            pstmt.close();

            ChitFundSystem chitFund = new ChitFundSystem(id, amount, months);
            system.addChitFund(chitFund);
            outputArea.append("Created new Chit Fund: " + id + "\n");
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error creating chit fund: " + e.getMessage());
        }
    }

    private void addParticipant() {
        if (system.getChitFunds().isEmpty()) {
            JOptionPane.showMessageDialog(this, "No chit funds available");
            return;
        }

        String chitFundId = JOptionPane.showInputDialog("Enter Chit Fund ID:");
        String participantId = JOptionPane.showInputDialog("Enter Participant ID:");
        String name = JOptionPane.showInputDialog("Enter Participant Name:");

        try {
            // Save to database
            PreparedStatement pstmt = dbConnection.prepareStatement(
                "INSERT INTO Participant (id, name, amountReceived) VALUES (?, ?, ?)");
            pstmt.setString(1, participantId);
            pstmt.setString(2, name);
            pstmt.setDouble(3, 0.0);
            pstmt.executeUpdate();
            pstmt.close();

            Participant participant = new Participant(participantId, name);
            system.addParticipantToChitFund(chitFundId, participant);
            outputArea.append("Added participant " + name + " to chit fund " + chitFundId + "\n");
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error adding participant: " + e.getMessage());
        }
    }

    private void makeBid() {
        if (system.getChitFunds().isEmpty()) {
            JOptionPane.showMessageDialog(this, "No chit funds available");
            return;
        }

        String chitFundId = JOptionPane.showInputDialog("Enter Chit Fund ID:");
        String participantId = JOptionPane.showInputDialog("Enter Participant ID:");
        String bidAmountStr = JOptionPane.showInputDialog("Enter Bid Amount:");

        try {
            double bidAmount = Double.parseDouble(bidAmountStr);

            // Save to database
            PreparedStatement pstmt = dbConnection.prepareStatement(
                "INSERT INTO Bid (chitFundId, participantId, bidAmount) VALUES (?, ?, ?)");
            pstmt.setString(1, chitFundId);
            pstmt.setString(2, participantId);
            pstmt.setDouble(3, bidAmount);
            pstmt.executeUpdate();
            pstmt.close();

            for (ChitFundSystem cf : system.getChitFunds()) {
                if (cf.getChitFundId().equals(chitFundId)) {
                    Participant participant = null;
                    for (Participant p : cf.getParticipants()) {
                        if (p.getParticipantId().equals(participantId)) {
                            participant = p;
                            break;
                        }
                    }

                    if (participant != null) {
                        participant.makeBid(cf, bidAmount);
                        outputArea.append("Bid of " + bidAmount + " made by participant " + participant.getName() + "\n");
                    } else {
                        JOptionPane.showMessageDialog(this, "Participant not found in Chit Fund");
                    }
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error making bid: " + e.getMessage());
        }
    }

    private void conductBidding() {
        if (system.getChitFunds().isEmpty()) {
            JOptionPane.showMessageDialog(this, "No chit funds available");
            return;
        }

        String chitFundId = JOptionPane.showInputDialog("Enter Chit Fund ID:");
        system.conductBidding(chitFundId);
    }

    private void displayInformation() {
        outputArea.setText("");
        try {
            Statement stmt = dbConnection.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM ChitFund");
            if (!rs.isBeforeFirst()) {
                outputArea.append("No chit funds available\n");
                rs.close();
                stmt.close();
                return;
            }
            while (rs.next()) {
                String chitFundId = rs.getString("id");
                double totalAmount = rs.getDouble("totalAmount");
                int numberOfMonths = rs.getInt("numberOfMonths");
                outputArea.append("Chit Fund ID: " + chitFundId + "\n");
                outputArea.append("Total Amount: " + totalAmount + "\n");
                outputArea.append("Number of Months: " + numberOfMonths + "\n");

                // Participants
                outputArea.append("Participants:\n");
                PreparedStatement pStmt = dbConnection.prepareStatement("SELECT * FROM Participant");
                ResultSet pRs = pStmt.executeQuery();
                while (pRs.next()) {
                    outputArea.append("  " + pRs.getString("name") + " (ID: " + pRs.getString("id") + ")\n");
                }
                pRs.close();
                pStmt.close();

                // Bids
                outputArea.append("Bids:\n");
                PreparedStatement bStmt = dbConnection.prepareStatement("SELECT * FROM Bid WHERE chitFundId = ?");
                bStmt.setString(1, chitFundId);
                ResultSet bRs = bStmt.executeQuery();
                while (bRs.next()) {
                    String participantId = bRs.getString("participantId");
                    double bidAmount = bRs.getDouble("bidAmount");
                    // Get participant name
                    PreparedStatement nameStmt = dbConnection.prepareStatement("SELECT name FROM Participant WHERE id = ?");
                    nameStmt.setString(1, participantId);
                    ResultSet nameRs = nameStmt.executeQuery();
                    String name = nameRs.next() ? nameRs.getString("name") : participantId;
                    nameRs.close();
                    nameStmt.close();
                    outputArea.append("  " + name + " bid: " + bidAmount + "\n");
                }
                bRs.close();
                bStmt.close();
                outputArea.append("\n");
            }
            rs.close();
            stmt.close();
        } catch (Exception e) {
            e.printStackTrace();
            outputArea.append("Error displaying information: " + e.getMessage() + "\n");
        }
    }

    private void showDatabaseData() {
        try {
            // Create a new window for displaying database data
            JFrame dataFrame = new JFrame("Database Data");
            dataFrame.setSize(800, 600);
            dataFrame.setLayout(new BorderLayout());

            // Create tabbed pane
            JTabbedPane tabbedPane = new JTabbedPane();

            // ChitFund Table
            JTable chitFundTable = createTable("SELECT * FROM ChitFund");
            tabbedPane.addTab("Chit Funds", new JScrollPane(chitFundTable));

            // Participant Table
            JTable participantTable = createTable("SELECT * FROM Participant");
            tabbedPane.addTab("Participants", new JScrollPane(participantTable));

            // Bid Table
            JTable bidTable = createTable("SELECT * FROM Bid");
            tabbedPane.addTab("Bids", new JScrollPane(bidTable));

            dataFrame.add(tabbedPane, BorderLayout.CENTER);
            dataFrame.setLocationRelativeTo(this);
            dataFrame.setVisible(true);
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error loading database data: " + e.getMessage());
        }
    }

    private JTable createTable(String query) throws SQLException {
        Statement stmt = dbConnection.createStatement();
        ResultSet rs = stmt.executeQuery(query);
        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();

        // Create column names
        Vector<String> columnNames = new Vector<>();
        for (int i = 1; i <= columnCount; i++) {
            columnNames.add(metaData.getColumnName(i));
        }

        // Create data
        Vector<Vector<Object>> data = new Vector<>();
        while (rs.next()) {
            Vector<Object> row = new Vector<>();
            for (int i = 1; i <= columnCount; i++) {
                row.add(rs.getObject(i));
            }
            data.add(row);
        }

        return new JTable(new DefaultTableModel(data, columnNames));
    }
}