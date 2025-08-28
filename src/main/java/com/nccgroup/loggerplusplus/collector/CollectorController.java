package com.nccgroup.loggerplusplus.collector;

import com.coreyd97.BurpExtenderUtilities.Preferences;
import com.nccgroup.loggerplusplus.LoggerPlusPlus;
import com.nccgroup.loggerplusplus.logentry.LogEntry;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CollectorController {

    // Preference keys
    private static final String PREF_COLLECTOR_ENABLED = "collector.enabled";
    private static final String PREF_COLLECTOR_SERVER_URL = "collector.serverUrl";
    private static final String PREF_COLLECTOR_SUBSYSTEM_NAME = "collector.subsystemName";
    private static final String PREF_COLLECTOR_DOMAIN_WHITELIST = "collector.domainWhitelist";
    private static final String PREF_COLLECTOR_SECRET_KEY = "collector.secretKey";
    private static final String PREF_COLLECTOR_FILTER_EMPTY_RESPONSE = "collector.filterEmptyResponse";
    private static final String PREF_COLLECTOR_FILTER_STATIC_RESOURCES = "collector.filterStaticResources";
    private static final String PREF_COLLECTOR_FILTER_STATUS_CODES = "collector.filterStatusCodes";
    private static final String PREF_COLLECTOR_ALLOWED_STATUS_CODES = "collector.allowedStatusCodes";

    private final Preferences preferences;
    private CollectorTab collectorTab;
    private final ExecutorService executorService;

    private boolean enabled;
    private String serverUrl;
    private String subsystemName;
    private Set<String> domainWhitelist;
    private String secretKey;
    
    // Filter settings
    private boolean filterEmptyResponse;
    private boolean filterStaticResources;
    private boolean filterStatusCodes;
    private Set<Integer> allowedStatusCodes;

    public CollectorController(Preferences preferences) {
        this.preferences = preferences;
        this.executorService = Executors.newSingleThreadExecutor();
        initialize();
    }

    public void initialize() {
        // Register preferences
        preferences.registerSetting(PREF_COLLECTOR_ENABLED, Boolean.class, false);
        preferences.registerSetting(PREF_COLLECTOR_SERVER_URL, String.class, "http://127.0.0.1:5000/recv");
        preferences.registerSetting(PREF_COLLECTOR_SUBSYSTEM_NAME, String.class, "BurpSuite");
        preferences.registerSetting(PREF_COLLECTOR_DOMAIN_WHITELIST, String[].class, new String[]{"example.com"});
        preferences.registerSetting(PREF_COLLECTOR_SECRET_KEY, String.class, "");
        
        // Register filter settings
        preferences.registerSetting(PREF_COLLECTOR_FILTER_EMPTY_RESPONSE, Boolean.class, true);
        preferences.registerSetting(PREF_COLLECTOR_FILTER_STATIC_RESOURCES, Boolean.class, true);
        preferences.registerSetting(PREF_COLLECTOR_FILTER_STATUS_CODES, Boolean.class, true);
        preferences.registerSetting(PREF_COLLECTOR_ALLOWED_STATUS_CODES, Integer[].class, new Integer[]{200});

        loadPreferences();
    }

    private void loadPreferences() {
        this.enabled = preferences.getSetting(PREF_COLLECTOR_ENABLED);
        this.serverUrl = preferences.getSetting(PREF_COLLECTOR_SERVER_URL);
        this.subsystemName = preferences.getSetting(PREF_COLLECTOR_SUBSYSTEM_NAME);
        
        String[] whitelistArray = preferences.getSetting(PREF_COLLECTOR_DOMAIN_WHITELIST);
        this.domainWhitelist = new HashSet<>();
        if (whitelistArray != null) {
            for (String domain : whitelistArray) {
                this.domainWhitelist.add(domain);
            }
        }
        
        this.secretKey = preferences.getSetting(PREF_COLLECTOR_SECRET_KEY);
        
        // Load filter settings
        this.filterEmptyResponse = preferences.getSetting(PREF_COLLECTOR_FILTER_EMPTY_RESPONSE);
        this.filterStaticResources = preferences.getSetting(PREF_COLLECTOR_FILTER_STATIC_RESOURCES);
        this.filterStatusCodes = preferences.getSetting(PREF_COLLECTOR_FILTER_STATUS_CODES);
        
        Integer[] statusCodesArray = preferences.getSetting(PREF_COLLECTOR_ALLOWED_STATUS_CODES);
        this.allowedStatusCodes = new HashSet<>();
        if (statusCodesArray != null) {
            for (Integer statusCode : statusCodesArray) {
                this.allowedStatusCodes.add(statusCode);
            }
        }
    }

    public void savePreferences() {
        preferences.setSetting(PREF_COLLECTOR_ENABLED, this.enabled);
        preferences.setSetting(PREF_COLLECTOR_SERVER_URL, this.serverUrl);
        preferences.setSetting(PREF_COLLECTOR_SUBSYSTEM_NAME, this.subsystemName);
        preferences.setSetting(PREF_COLLECTOR_DOMAIN_WHITELIST, this.domainWhitelist.toArray(new String[0]));
        preferences.setSetting(PREF_COLLECTOR_SECRET_KEY, this.secretKey);
        
        // Save filter settings
        preferences.setSetting(PREF_COLLECTOR_FILTER_EMPTY_RESPONSE, this.filterEmptyResponse);
        preferences.setSetting(PREF_COLLECTOR_FILTER_STATIC_RESOURCES, this.filterStaticResources);
        preferences.setSetting(PREF_COLLECTOR_FILTER_STATUS_CODES, this.filterStatusCodes);
        preferences.setSetting(PREF_COLLECTOR_ALLOWED_STATUS_CODES, this.allowedStatusCodes.toArray(new Integer[0]));
    }

    public void setCollectorTab(CollectorTab collectorTab) {
        this.collectorTab = collectorTab;
    }

    public void sendToCollector(LogEntry logEntry) {
        if (!enabled || serverUrl == null || serverUrl.isEmpty()) {
            return;
        }

        // Check if host is in whitelist
        String host = logEntry.getHostname();
        if (host == null || !isHostInWhitelist(host)) {
            return;
        }
        
        // Apply filters
        if (!shouldSendLogEntry(logEntry)) {
            return;
        }

        // Send asynchronously to avoid blocking UI
        executorService.submit(() -> {
            try {
                // Create JSON payload
                String requestStr = new String(logEntry.getRequestBytes(), StandardCharsets.UTF_8);
                String responseStr = logEntry.getResponseBytes() != null ? 
                        new String(logEntry.getResponseBytes(), StandardCharsets.UTF_8) : "";
                
                String jsonPayload = String.format(
                        "{\"subsystem\": \"%s\", \"host\": \"%s\", \"request\": \"%s\", \"response\": \"%s\", \"secret\": \"%s\"}",
                        escapeJson(subsystemName),
                        escapeJson(host),
                        escapeJson(requestStr),
                        escapeJson(responseStr),
                        escapeJson(secretKey)
                );

                // Send HTTP POST request
                URL url = new URL(serverUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setDoOutput(true);

                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = jsonPayload.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                int responseCode = connection.getResponseCode();
                if (responseCode >= 200 && responseCode < 300) {
                    logMessage("Successfully sent data to collector for host: " + host);
                } else {
                    logMessage("Failed to send data to collector. Response code: " + responseCode);
                }
            } catch (Exception e) {
                logMessage("Error sending data to collector: " + e.getMessage());
            }
        });
    }

    private boolean isHostInWhitelist(String host) {
        if (domainWhitelist.isEmpty()) {
            return true; // If whitelist is empty, allow all hosts
        }
        
        for (String whitelistedDomain : domainWhitelist) {
            if (host.equals(whitelistedDomain) || host.endsWith("." + whitelistedDomain)) {
                return true;
            }
        }
        
        return false;
    }

    private String escapeJson(String input) {
        if (input == null) {
            return "";
        }
        return input.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }

    private void logMessage(String message) {
        if (collectorTab != null) {
            collectorTab.logMessage(message);
        }
    }

    // Getters and setters
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getServerUrl() {
        return serverUrl;
    }

    public void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    public String getSubsystemName() {
        return subsystemName;
    }

    public void setSubsystemName(String subsystemName) {
        this.subsystemName = subsystemName;
    }

    public Set<String> getDomainWhitelist() {
        return domainWhitelist;
    }

    public void setDomainWhitelist(Set<String> domainWhitelist) {
        this.domainWhitelist = domainWhitelist;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }
    
    // Filter getters and setters
    public boolean isFilterEmptyResponse() {
        return filterEmptyResponse;
    }

    public void setFilterEmptyResponse(boolean filterEmptyResponse) {
        this.filterEmptyResponse = filterEmptyResponse;
    }

    public boolean isFilterStaticResources() {
        return filterStaticResources;
    }

    public void setFilterStaticResources(boolean filterStaticResources) {
        this.filterStaticResources = filterStaticResources;
    }



    public boolean isFilterStatusCodes() {
        return filterStatusCodes;
    }

    public void setFilterStatusCodes(boolean filterStatusCodes) {
        this.filterStatusCodes = filterStatusCodes;
    }

    public Set<Integer> getAllowedStatusCodes() {
        return allowedStatusCodes;
    }

    public void setAllowedStatusCodes(Set<Integer> allowedStatusCodes) {
        this.allowedStatusCodes = allowedStatusCodes;
    }
    
    /**
     * Determines if a log entry should be sent to the collector based on filter settings
     */
    private boolean shouldSendLogEntry(LogEntry logEntry) {
        // Log the entry we're processing
        logMessage("Processing entry for host: " + logEntry.getHostname() + ", status: " + logEntry.getResponseStatus() + ", URL: " + logEntry.getUrl());
        
        // Filter empty responses
        if (filterEmptyResponse && (logEntry.getResponseBytes() == null || logEntry.getResponseBytes().length == 0)) {
            logMessage("Filtered: Empty response for host: " + logEntry.getHostname());
            return false;
        }
        logMessage("Passed empty response filter for host: " + logEntry.getHostname());
        
        // Filter by status code
        if (filterStatusCodes && logEntry.getResponseStatus() > 0) {
            if (!allowedStatusCodes.contains(logEntry.getResponseStatus())) {
                logMessage("Filtered: Status code " + logEntry.getResponseStatus() + " for host: " + logEntry.getHostname());
                return false;
            }
            logMessage("Passed status code filter (" + logEntry.getResponseStatus() + ") for host: " + logEntry.getHostname());
        }
        
        // Filter by URL extension for static resources
        if (filterStaticResources && logEntry.getUrlExtension() != null && !logEntry.getUrlExtension().isEmpty()) {
            String extension = logEntry.getUrlExtension().toLowerCase();
            if (extension.equals("css") || extension.equals("js") || extension.equals("png") || 
                extension.equals("jpg") || extension.equals("jpeg") || extension.equals("gif") || 
                extension.equals("svg") || extension.equals("woff") || extension.equals("woff2") || 
                extension.equals("ttf") || extension.equals("eot")) {
                logMessage("Filtered: Static resource extension " + extension + " for host: " + logEntry.getHostname());
                return false;
            }
            logMessage("Passed static resource filter (extension: " + extension + ") for host: " + logEntry.getHostname());
        }
        
        // Entry passed all filters
        logMessage("SUCCESS: Sending entry for host: " + logEntry.getHostname() + ", status: " + logEntry.getResponseStatus() + ", URL: " + logEntry.getUrl());
        return true;
    }

    public void shutdown() {
        executorService.shutdown();
    }
}