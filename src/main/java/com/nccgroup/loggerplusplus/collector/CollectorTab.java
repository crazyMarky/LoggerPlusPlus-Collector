package com.nccgroup.loggerplusplus.collector;

import com.coreyd97.BurpExtenderUtilities.Preferences;
import com.nccgroup.loggerplusplus.LoggerPlusPlus;
import com.nccgroup.loggerplusplus.util.Globals;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class CollectorTab extends JPanel {

    private final Preferences preferences;
    private final CollectorController collectorController;

    private JCheckBox enabledCheckbox;
    private JTextField serverUrlField;
    private JTextField subsystemNameField;
    private JTextArea domainWhitelistArea;
    private JPasswordField secretKeyField;
    private JButton testConnectionButton;
    private JTextArea logArea;
    
    // Filter UI components
    private JCheckBox filterEmptyResponseCheckbox;
    private JCheckBox filterStaticResourcesCheckbox;
    private JCheckBox filterStatusCodesCheckbox;
    private JTextField allowedStatusCodesField;

    public CollectorTab(Preferences preferences, CollectorController collectorController) {
        this.preferences = preferences;
        this.collectorController = collectorController;
        initializeUI();
    }

    private void initializeUI() {
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(5, 5, 5, 5));

        // Create main panel with GridBagLayout for form elements
        JPanel mainPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(3, 3, 3, 3);

        // Enable/Disable Checkbox
        enabledCheckbox = new JCheckBox("Enable Collector");
        enabledCheckbox.setSelected(collectorController.isEnabled());
        enabledCheckbox.addActionListener(e -> {
            collectorController.setEnabled(enabledCheckbox.isSelected());
            updateComponentStates();
        });
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        mainPanel.add(enabledCheckbox, gbc);

        // Initialize fields
        serverUrlField = new JTextField(collectorController.getServerUrl());
        serverUrlField.setToolTipText("The URL to send HTTP traffic to (e.g., http://127.0.0.1:5000/recv)");
        
        subsystemNameField = new JTextField(collectorController.getSubsystemName());
        subsystemNameField.setToolTipText("Name to identify this subsystem");
        
        domainWhitelistArea = new JTextArea(5, 20);
        domainWhitelistArea.setText(String.join("\n", collectorController.getDomainWhitelist()));
        domainWhitelistArea.setToolTipText("Enter domains to whitelist, one per line");
        
        secretKeyField = new JPasswordField(collectorController.getSecretKey());
        secretKeyField.setToolTipText("Secret key for authentication with the server");
        
        // Create a tabbed pane for better organization
         JTabbedPane tabbedPane = new JTabbedPane();
         
         // Create connection panel
         JPanel connectionPanel = new JPanel(new GridBagLayout());
         GridBagConstraints connGbc = new GridBagConstraints();
         connGbc.fill = GridBagConstraints.HORIZONTAL;
         connGbc.insets = new Insets(3, 3, 3, 3);
         
         // Move server URL and subsystem name to connection panel
         JLabel serverUrlLabel = new JLabel("Server URL:");
         connGbc.gridx = 0;
         connGbc.gridy = 0;
         connGbc.gridwidth = 1;
         connGbc.weightx = 0.0;
         connectionPanel.add(serverUrlLabel, connGbc);
         
         connGbc.gridx = 1;
         connGbc.gridy = 0;
         connGbc.weightx = 1.0;
         connectionPanel.add(serverUrlField, connGbc);
         
         JLabel subsystemLabel = new JLabel("Subsystem Name:");
         connGbc.gridx = 0;
         connGbc.gridy = 1;
         connGbc.weightx = 0.0;
         connectionPanel.add(subsystemLabel, connGbc);
         
         connGbc.gridx = 1;
         connGbc.gridy = 1;
         connGbc.weightx = 1.0;
         connectionPanel.add(subsystemNameField, connGbc);
         
         JLabel secretLabel = new JLabel("Secret Key:");
         connGbc.gridx = 0;
         connGbc.gridy = 2;
         connGbc.weightx = 0.0;
         connectionPanel.add(secretLabel, connGbc);
         
         connGbc.gridx = 1;
         connGbc.gridy = 2;
         connGbc.weightx = 1.0;
         connectionPanel.add(secretKeyField, connGbc);
         
         // Test Connection Button
         testConnectionButton = new JButton("Test Connection");
         testConnectionButton.addActionListener(e -> testConnection());
         connGbc.gridx = 0;
         connGbc.gridy = 3;
         connGbc.gridwidth = 2;
         connGbc.weightx = 1.0;
         connectionPanel.add(testConnectionButton, connGbc);
         
         // Create filter panel
         JPanel filterPanel = new JPanel(new GridBagLayout());
         GridBagConstraints filterGbc = new GridBagConstraints();
         filterGbc.fill = GridBagConstraints.HORIZONTAL;
         filterGbc.insets = new Insets(3, 3, 3, 3);
         
         // Filter Empty Response
         filterEmptyResponseCheckbox = new JCheckBox("Filter Empty Responses");
         filterEmptyResponseCheckbox.setSelected(collectorController.isFilterEmptyResponse());
         filterEmptyResponseCheckbox.setToolTipText("Don't send requests with empty responses to the collector");
         filterGbc.gridx = 0;
         filterGbc.gridy = 0;
         filterGbc.gridwidth = 2;
         filterPanel.add(filterEmptyResponseCheckbox, filterGbc);
         
         // Filter Static Resources
         filterStaticResourcesCheckbox = new JCheckBox("Filter Static Resources");
         filterStaticResourcesCheckbox.setSelected(collectorController.isFilterStaticResources());
         filterStaticResourcesCheckbox.setToolTipText("Don't send static resources (CSS, JS, images, etc.) to the collector");
         filterGbc.gridx = 0;
         filterGbc.gridy = 1;
         filterGbc.gridwidth = 2;
         filterPanel.add(filterStaticResourcesCheckbox, filterGbc);
         
         // Filter Status Codes
         JPanel statusCodesPanel = new JPanel(new BorderLayout(5, 0));
         filterStatusCodesCheckbox = new JCheckBox("Filter Status Codes (Allow only):");
         filterStatusCodesCheckbox.setSelected(collectorController.isFilterStatusCodes());
         statusCodesPanel.add(filterStatusCodesCheckbox, BorderLayout.WEST);
         
         allowedStatusCodesField = new JTextField(10);
         allowedStatusCodesField.setText(String.join(", ", collectorController.getAllowedStatusCodes().stream()
                 .map(String::valueOf).toArray(String[]::new)));
         allowedStatusCodesField.setToolTipText("Enter allowed status codes, comma separated (e.g., 200, 201, 204)");
         statusCodesPanel.add(allowedStatusCodesField, BorderLayout.CENTER);
         
         filterGbc.gridx = 0;
         filterGbc.gridy = 2;
         filterGbc.gridwidth = 2;
         filterPanel.add(statusCodesPanel, filterGbc);
         
         // Create whitelist panel
         JPanel whitelistPanel = new JPanel(new BorderLayout());
         whitelistPanel.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
         JLabel whitelistLabel = new JLabel("Domain Whitelist (one per line):");
         whitelistPanel.add(whitelistLabel, BorderLayout.NORTH);
         whitelistPanel.add(new JScrollPane(domainWhitelistArea), BorderLayout.CENTER);
         
         // Add panels to tabbed pane
         tabbedPane.addTab("Connection", connectionPanel);
         tabbedPane.addTab("Filters", filterPanel);
         tabbedPane.addTab("Whitelist", whitelistPanel);
         
         // Add tabbed pane to main panel
         gbc.gridx = 0;
         gbc.gridy = 1;
         gbc.gridwidth = 2;
         gbc.weightx = 1.0;
         gbc.weighty = 1.0;
         gbc.fill = GridBagConstraints.BOTH;
         mainPanel.add(tabbedPane, gbc);

        // Test Connection Button is already in the Connection tab

        // Save Button
        JButton saveButton = new JButton("Save Configuration");
        saveButton.addActionListener(e -> saveConfiguration());
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        mainPanel.add(saveButton, gbc);

        // Log Area
        logArea = new JTextArea(10, 40);
        logArea.setEditable(false);
        JScrollPane logScrollPane = new JScrollPane(logArea);
        logScrollPane.setBorder(BorderFactory.createTitledBorder("Log"));

        // Add components to main panel
        add(mainPanel, BorderLayout.NORTH);
        add(logScrollPane, BorderLayout.CENTER);

        // Update component states based on enabled status
        updateComponentStates();
    }

    private void updateComponentStates() {
        boolean enabled = enabledCheckbox.isSelected();
        serverUrlField.setEnabled(enabled);
        subsystemNameField.setEnabled(enabled);
        domainWhitelistArea.setEnabled(enabled);
        secretKeyField.setEnabled(enabled);
        testConnectionButton.setEnabled(enabled);
        
        // Update filter components
        filterEmptyResponseCheckbox.setEnabled(enabled);
        filterStaticResourcesCheckbox.setEnabled(enabled);
        filterStatusCodesCheckbox.setEnabled(enabled);
        allowedStatusCodesField.setEnabled(enabled && filterStatusCodesCheckbox.isSelected());
        
        // Add listener to status codes checkbox
        filterStatusCodesCheckbox.addActionListener(e -> {
            allowedStatusCodesField.setEnabled(enabled && filterStatusCodesCheckbox.isSelected());
        });
    }

    private void saveConfiguration() {
        collectorController.setServerUrl(serverUrlField.getText());
        collectorController.setSubsystemName(subsystemNameField.getText());
        
        // Parse domain whitelist
        String whitelistText = domainWhitelistArea.getText();
        Set<String> domains = new HashSet<>();
        if (whitelistText != null && !whitelistText.trim().isEmpty()) {
            domains.addAll(Arrays.asList(whitelistText.split("\\r?\\n")));
        }
        collectorController.setDomainWhitelist(domains);
        
        // Set secret key
        collectorController.setSecretKey(new String(secretKeyField.getPassword()));
        
        // Save filter settings
        collectorController.setFilterEmptyResponse(filterEmptyResponseCheckbox.isSelected());
        collectorController.setFilterStaticResources(filterStaticResourcesCheckbox.isSelected());
        
        // Content type filtering has been removed
        
        // Parse status codes
        collectorController.setFilterStatusCodes(filterStatusCodesCheckbox.isSelected());
        String statusCodesText = allowedStatusCodesField.getText();
        Set<Integer> statusCodes = new HashSet<>();
        if (statusCodesText != null && !statusCodesText.trim().isEmpty()) {
            try {
                for (String codeStr : statusCodesText.split(",")) {
                    statusCodes.add(Integer.parseInt(codeStr.trim()));
                }
            } catch (NumberFormatException e) {
                logMessage("Warning: Invalid status code format. Using default values.");
                statusCodes.add(200); // Default to 200 if parsing fails
            }
        }
        collectorController.setAllowedStatusCodes(statusCodes);
        
        // Save to preferences
        collectorController.savePreferences();
        
        logMessage("Configuration saved successfully.");
    }

    private void testConnection() {
        String serverUrl = serverUrlField.getText();
        if (serverUrl == null || serverUrl.trim().isEmpty()) {
            logMessage("Error: Server URL cannot be empty.");
            return;
        }

        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() {
                try {
                    // Extract base URL from server URL (remove /recv part if present)
                    String baseUrl = serverUrl;
                    if (baseUrl.endsWith("/recv")) {
                        baseUrl = baseUrl.substring(0, baseUrl.length() - 5);
                    }
                    String testUrl = baseUrl + "/test";
                    
                    URL url = new URL(testUrl);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("POST");
                    connection.setRequestProperty("Content-Type", "application/json");
                    connection.setDoOutput(true);
                    connection.setConnectTimeout(5000);
                    connection.setReadTimeout(5000);
                    
                    // Send a simple test JSON data
                    String testJson = "{\"test\": true}";
                    try (java.io.OutputStream os = connection.getOutputStream()) {
                        byte[] input = testJson.getBytes("utf-8");
                        os.write(input, 0, input.length);
                    }
                    
                    int responseCode = connection.getResponseCode();
                    return responseCode >= 200 && responseCode < 400;
                } catch (IOException e) {
                    return false;
                }
            }

            @Override
            protected void done() {
                try {
                    boolean success = get();
                    if (success) {
                        logMessage("Connection successful!");
                    } else {
                        logMessage("Connection failed. Please check the server URL and ensure the server is running.");
                    }
                } catch (Exception e) {
                    logMessage("Error testing connection: " + e.getMessage());
                }
            }
        }.execute();
    }

    public void logMessage(String message) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(message + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }
}