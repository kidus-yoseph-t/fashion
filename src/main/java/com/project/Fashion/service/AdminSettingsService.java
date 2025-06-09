package com.project.Fashion.service;

import com.project.Fashion.model.AdminSettings;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

@Service
public class AdminSettingsService {

    private final ConcurrentHashMap<String, Object> settings = new ConcurrentHashMap<>();

    public AdminSettingsService() {
        // Initialize with default values
        settings.put("siteMaintenanceMode", false);
        settings.put("supportEmail", "support@fashion.com");
    }

    public AdminSettings getSettings() {
        AdminSettings currentSettings = new AdminSettings();
        currentSettings.setSiteMaintenanceMode((Boolean) settings.get("siteMaintenanceMode"));
        currentSettings.setSupportEmail((String) settings.get("supportEmail"));
        return currentSettings;
    }

    public AdminSettings updateSettings(AdminSettings newSettings) {
        settings.put("siteMaintenanceMode", newSettings.isSiteMaintenanceMode());
        settings.put("supportEmail", newSettings.getSupportEmail());
        return getSettings();
    }
}