package com.myProject.spamDetector;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.sql.*;

public class SpamDetectorUI {
    private JFrame frame;
    private JTextArea textArea;
    private JLabel resultLabel;
    private JLabel emailDisplayLabel;
    private JButton monitorButton;
    private volatile boolean isMonitoring = false;
    private ScheduledExecutorService scheduler;
    private String userEmail = null;
    private String userPassword = null;
    private final SpamClassifier classifier;
    
   
    private static final String DB_URL = "jdbc:sqlite:spamDetectorDatabase.db";

    public SpamDetectorUI() {
        classifier = new SpamClassifier();
        initializeDatabase();
        loadCredentials();
        initializeUI();
    }
    
    private void loadCredentials() {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT EMAIL, PASSWORD FROM LAST ORDER BY ID DESC LIMIT 1")) {

            if (rs.next()) {
                userEmail = rs.getString("EMAIL");
                userPassword = rs.getString("PASSWORD");
                GmailServiceWrapper.setCredentials(userEmail, userPassword);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showError("Failed to load mailbox credentials: " + e.getMessage());
        }
    }
    
    private void initializeDatabase() {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {
            String sql = "CREATE TABLE IF NOT EXISTS USERS (" +
                    "ID INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "EMAIL TEXT NOT NULL," +
                    "PASSWORD TEXT NOT NULL)";
            stmt.executeUpdate(sql);
            
            sql = "CREATE TABLE IF NOT EXISTS LAST (" +
                    "ID INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "EMAIL TEXT NOT NULL," +
                    "PASSWORD TEXT NOT NULL)";
            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            e.printStackTrace();
            showError("Database initialization failed: " + e.getMessage());
        }
    }
    
    private void saveCredentials(String email, String password) {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement("INSERT INTO USERS (EMAIL, PASSWORD) VALUES (?, ?)")) {
            pstmt.setString(1, email);
            pstmt.setString(2, password);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            showError("Failed to save mailbox credentials: " + e.getMessage());
        }
    }
    
    private void saveLast(String email, String password) {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement("INSERT INTO LAST (EMAIL, PASSWORD) VALUES (?, ?)")) {
            pstmt.setString(1, email);
            pstmt.setString(2, password);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            showError("Save the Last User inavailable: " + e.getMessage());
        }
    }
    
    private void clearLast() {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement("DELETE FROM LAST")) {
			pstmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
            showError("Failed to clear the history user list: " + e.getMessage());
        }
    }

    private void clearUsersTable() {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement("DELETE FROM USERS")) {
            pstmt.executeUpdate();
            JOptionPane.showMessageDialog(frame, "All email credentials have been deleted.", "Success", JOptionPane.INFORMATION_MESSAGE);
        } catch (SQLException e) {
            e.printStackTrace();
            showError("Failed to clear the USERS table: " + e.getMessage());
        }
    }
    
    private void initializeUI() {
    	UIManager.put("OptionPane.yesButtonText", "Yes"); // my system language is Chinese, so all button related to "OK", "Cancel" and so on are all in Chinese, 
    	UIManager.put("OptionPane.noButtonText", "No");  // these four lines are to set buttons like this to English
    	UIManager.put("OptionPane.okButtonText", "OK");
    	UIManager.put("OptionPane.cancelButtonText", "Cancel");

        frame = new JFrame("Spam detection system");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 550);
        frame.setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        emailDisplayLabel = new JLabel(userEmail == null ? "Current Email: Not set" : "Current Email: " + userEmail, SwingConstants.CENTER);
        emailDisplayLabel.setFont(new Font("微软雅黑", Font.BOLD, 14));

        textArea = new JTextArea();
        textArea.setLineWrap(true);
        textArea.setEditable(true);
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(750, 300));

        JPanel controlPanel = new JPanel(new GridLayout(1, 3, 10, 0));
        JButton checkButton = new JButton("Manual detection");
        checkButton.addActionListener(this::manualCheck);

        JButton emailSettingsButton = new JButton("Set up your mailbox");
        emailSettingsButton.addActionListener(this::openEmailSettingsDialog);

        monitorButton = new JButton("Start real-time monitoring");
        monitorButton.setBackground(new Color(144, 238, 144));
        monitorButton.addActionListener(this::toggleMonitoring);

        controlPanel.add(emailSettingsButton);
        controlPanel.add(checkButton);
        controlPanel.add(monitorButton);

        resultLabel = new JLabel("System Ready", SwingConstants.CENTER);
        resultLabel.setFont(new Font("微软雅黑", Font.BOLD, 18));
        resultLabel.setBorder(BorderFactory.createTitledBorder("Detection status"));
        resultLabel.setPreferredSize(new Dimension(150, 50)); // make the size of status frame fixed instead of changing along with text
        resultLabel.setMinimumSize(new Dimension(150, 50)); 
        resultLabel.setMaximumSize(new Dimension(150, 50));

        mainPanel.add(scrollPane, BorderLayout.CENTER);
        mainPanel.add(controlPanel, BorderLayout.SOUTH);
        mainPanel.add(resultLabel, BorderLayout.EAST);
        mainPanel.add(emailDisplayLabel, BorderLayout.NORTH);
        JLabel label = new JLabel("<html>c<br>o<br>n<br>t<br>e<br>n<br>t:</html>", SwingConstants.LEFT);
        mainPanel.add(label, BorderLayout.WEST);

        
        frame.add(mainPanel);
        frame.setVisible(true);
    }

    private void openEmailSettingsDialog(ActionEvent e) {
        // Create main panel
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));

        // Input panel
        JPanel inputPanel = new JPanel(new GridLayout(2, 2, 5, 5));
        JTextField emailField = new JTextField();
        JPasswordField passwordField = new JPasswordField();

        inputPanel.add(new JLabel("Gmail Account:"));
        inputPanel.add(emailField);
        inputPanel.add(new JLabel("APP Password:"));
        inputPanel.add(passwordField);

        // Buttons panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton clearHistoryButton = new JButton("Clear history user");
        clearHistoryButton.addActionListener(evt -> {
            int confirm = JOptionPane.showConfirmDialog(frame, 
                "Are you sure to delete ALL saved credentials?", 
                "Confirm Clear History", 
                JOptionPane.YES_NO_OPTION);

            if (confirm == JOptionPane.YES_OPTION) {
                clearLast();
                clearUsersTable();
                userEmail = null;
                userPassword = null;
                emailDisplayLabel.setText("Current Email: Not set");
                JOptionPane.showMessageDialog(frame, 
                    "All user history has been cleared!", 
                    "Success", 
                    JOptionPane.INFORMATION_MESSAGE);
            }
        });

        buttonPanel.add(clearHistoryButton);

        mainPanel.add(inputPanel, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        // Display dialog
        int option = JOptionPane.showConfirmDialog(frame, 
            mainPanel, 
            "Set up Gmail account", 
            JOptionPane.OK_CANCEL_OPTION, 
            JOptionPane.PLAIN_MESSAGE);

        if (option == JOptionPane.OK_OPTION) {
            // Handle email/password input
            String enteredEmail = emailField.getText().trim();
            String enteredPassword = new String(passwordField.getPassword());
            String existingPassword = getPasswordForEmail(enteredEmail);

            if (existingPassword != null) {
                userPassword = existingPassword;
                JOptionPane.showMessageDialog(frame, 
                    "The password for this mailbox has been found.", 
                    "Message", 
                    JOptionPane.INFORMATION_MESSAGE);
            } else {
                userPassword = enteredPassword;
                saveCredentials(enteredEmail, enteredPassword);
            }

            userEmail = enteredEmail;
            clearLast();
            saveLast(userEmail, userPassword);
            GmailServiceWrapper.setCredentials(userEmail, userPassword);
            emailDisplayLabel.setText("Current Email: " + userEmail);
        }
    }

    
    private String getPasswordForEmail(String email) {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement("SELECT PASSWORD FROM USERS WHERE EMAIL = ?")) {
            pstmt.setString(1, email);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getString("PASSWORD");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showError("Failed to obtain email password: " + e.getMessage());
        }
        return null;
    }

    private void toggleMonitoring(ActionEvent e) {
        if (userEmail == null || userPassword == null) {
            openEmailSettingsDialog(null);
        }

        isMonitoring = !isMonitoring;

        SwingUtilities.invokeLater(() -> {
            if (isMonitoring) {
                textArea.setText(""); // Clear text when monitoring starts
                textArea.setEditable(false);
                monitorButton.setText("Stop real-time monitoring");
                monitorButton.setBackground(new Color(255, 99, 71));
                resultLabel.setText("System Ready");
                resultLabel.setForeground(Color.BLACK);
                startMonitoring();
            } else {
                stopMonitoring();
                textArea.setText(""); // Clear text when monitoring stops
                textArea.setEditable(true);
                monitorButton.setText("Start real-time monitoring");
                monitorButton.setBackground(new Color(144, 238, 144));
                resultLabel.setText("System Ready");
                resultLabel.setForeground(Color.BLACK);
            }
        });
    }

    private void startMonitoring() {
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleWithFixedDelay(() -> {
            try {
                String emailContent = GmailServiceWrapper.readMessagesFromGmail();
                SwingUtilities.invokeLater(() -> {
                    textArea.setText(""); // Ensure whiteboard is cleared before new content
                    if (!emailContent.isEmpty() && !emailContent.equals("No unread emails")) {
                        textArea.setText(emailContent); // Show only new email
                        processContent(emailContent);
                    }
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    showError("Login failed, please reset your Gmail account");
                    openEmailSettingsDialog(null);
                });
            }
        }, 0, 45, TimeUnit.SECONDS);
    }

    private void stopMonitoring() {
        if (scheduler != null) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(1, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException ex) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    private void manualCheck(ActionEvent e) {
        if (isMonitoring) {
            showResult("Please turn off real-time monitoring first", Color.ORANGE);
            return;
        }

        String content = textArea.getText().trim();
        if (content.isEmpty()) {
            showResult("Please enter the content to be tested", Color.ORANGE);
            return;
        }

        processContent(content);
    }

    private void processContent(String content) {
        try {
            boolean isSpam = classifier.isSpam(content);
            SwingUtilities.invokeLater(() -> {
                showResult(isSpam ? "Spam!" : "Normal",
                          isSpam ? new Color(220, 20, 60) : new Color(34, 139, 34));
            });
        } catch (Exception e) {
            SwingUtilities.invokeLater(() ->
                showResult("Analysis failed: " + e.getMessage(), Color.RED));
        }
    }

    private void showResult(String message, Color color) {
        resultLabel.setText(message);
        resultLabel.setForeground(color);
        if (color == Color.RED) {
            Toolkit.getDefaultToolkit().beep();
        }
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(frame, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(SpamDetectorUI::new);
    }
}
