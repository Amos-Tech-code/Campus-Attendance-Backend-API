// static/admin/js/modules/system-settings.js
let originalSettings = null;

window.initSystemSettings = async function() {
    console.log('=== initSystemSettings START ===');
    await loadSettings();
    console.log('=== initSystemSettings END ===');

    // Setup form submit handler
    document.getElementById('systemSettingsForm').addEventListener('submit', async (e) => {
        e.preventDefault();
        await saveSettings();
    });
};

async function loadSettings() {
    try {
        const response = await fetchWithAuth('/admin/api/system-settings');
        if (!response.ok) throw new Error('Failed to load settings');

        const settings = await response.json();
        originalSettings = settings;

        // Populate form with current settings
        populateForm(settings);

    } catch (err) {
        console.error('Failed to load settings:', err);
        showNotification('Failed to load system settings', 'error');
    }
}

function populateForm(settings) {
    // Attendance Settings
    document.getElementById('defaultAttendanceDuration').value = settings.defaultAttendanceDuration;
    document.getElementById('defaultLocationRadius').value = settings.defaultLocationRadius;
    document.getElementById('requireLocationForAttendance').checked = settings.requireLocationForAttendance;
    document.getElementById('requireDeviceVerification').checked = settings.requireDeviceVerification;

    // Security Settings
    document.getElementById('maxLoginAttempts').value = settings.maxLoginAttempts;
    document.getElementById('lockoutDurationMinutes').value = settings.lockoutDurationMinutes;
    document.getElementById('passwordExpiryDays').value = settings.passwordExpiryDays;
    document.getElementById('requireStrongPasswords').checked = settings.requireStrongPasswords;
    document.getElementById('sessionTimeoutMinutes').value = settings.sessionTimeoutMinutes;

    // Device Settings
    document.getElementById('maxDevicesPerStudent').value = settings.maxDevicesPerStudent;
    document.getElementById('deviceChangeRequiresApproval').checked = settings.deviceChangeRequiresApproval;
    document.getElementById('autoApproveTrustedDevices').checked = settings.autoApproveTrustedDevices;

    // System Preferences
    document.getElementById('systemName').value = settings.systemName;
    document.getElementById('systemEmail').value = settings.systemEmail;
    document.getElementById('timezone').value = settings.timezone;
    document.getElementById('dateFormat').value = settings.dateFormat;
    document.getElementById('timeFormat').value = settings.timeFormat;

    // Notification Settings
    document.getElementById('enablePushNotifications').checked = settings.enablePushNotifications;
    document.getElementById('enableEmailNotifications').checked = settings.enableEmailNotifications;
    document.getElementById('notificationRetentionDays').value = settings.notificationRetentionDays;

    // Report Settings
    document.getElementById('defaultReportFormat').value = settings.defaultReportFormat;
    document.getElementById('autoGenerateReports').checked = settings.autoGenerateReports;
    document.getElementById('reportRetentionDays').value = settings.reportRetentionDays;

    // Maintenance Settings
    document.getElementById('maintenanceMode').checked = settings.maintenanceMode;
    document.getElementById('maintenanceMessage').value = settings.maintenanceMessage || '';

    // Set allowed attendance methods checkboxes
    const allowedMethods = settings.allowedAttendanceMethods || [];
    document.querySelectorAll('#allowedAttendanceMethodsGroup input[type="checkbox"]').forEach(checkbox => {
        checkbox.checked = allowedMethods.includes(checkbox.value);
    });
}

async function saveSettings() {
    try {
        // Collect attendance settings
        const allowedMethods = [];
        document.querySelectorAll('#allowedAttendanceMethodsGroup input[type="checkbox"]:checked').forEach(cb => {
            allowedMethods.push(cb.value);
        });

        const updateRequest = {
            attendanceSettings: {
                defaultAttendanceDuration: parseInt(document.getElementById('defaultAttendanceDuration').value),
                defaultLocationRadius: parseInt(document.getElementById('defaultLocationRadius').value),
                allowedAttendanceMethods: allowedMethods,
                requireLocationForAttendance: document.getElementById('requireLocationForAttendance').checked,
                requireDeviceVerification: document.getElementById('requireDeviceVerification').checked
            },
            securitySettings: {
                maxLoginAttempts: parseInt(document.getElementById('maxLoginAttempts').value),
                lockoutDurationMinutes: parseInt(document.getElementById('lockoutDurationMinutes').value),
                passwordExpiryDays: parseInt(document.getElementById('passwordExpiryDays').value),
                requireStrongPasswords: document.getElementById('requireStrongPasswords').checked,
                sessionTimeoutMinutes: parseInt(document.getElementById('sessionTimeoutMinutes').value)
            },
            deviceSettings: {
                maxDevicesPerStudent: parseInt(document.getElementById('maxDevicesPerStudent').value),
                deviceChangeRequiresApproval: document.getElementById('deviceChangeRequiresApproval').checked,
                autoApproveTrustedDevices: document.getElementById('autoApproveTrustedDevices').checked
            },
            systemPreferences: {
                systemName: document.getElementById('systemName').value,
                systemEmail: document.getElementById('systemEmail').value,
                timezone: document.getElementById('timezone').value,
                dateFormat: document.getElementById('dateFormat').value,
                timeFormat: document.getElementById('timeFormat').value
            },
            notificationSettings: {
                enablePushNotifications: document.getElementById('enablePushNotifications').checked,
                enableEmailNotifications: document.getElementById('enableEmailNotifications').checked,
                notificationRetentionDays: parseInt(document.getElementById('notificationRetentionDays').value)
            },
            reportSettings: {
                defaultReportFormat: document.getElementById('defaultReportFormat').value,
                autoGenerateReports: document.getElementById('autoGenerateReports').checked,
                reportRetentionDays: parseInt(document.getElementById('reportRetentionDays').value)
            },
            maintenanceSettings: {
                maintenanceMode: document.getElementById('maintenanceMode').checked,
                maintenanceMessage: document.getElementById('maintenanceMessage').value
            }
        };

        const response = await fetchWithAuth('/admin/api/system-settings', {
            method: 'PUT',
            body: JSON.stringify(updateRequest)
        });

        if (response.ok) {
            showNotification('Settings saved successfully', 'success');
            await loadSettings(); // Reload to confirm changes
        } else {
            throw new Error('Failed to save settings');
        }
    } catch (err) {
        console.error('Failed to save settings:', err);
        showNotification('Failed to save settings: ' + err.message, 'error');
    }
}

async function resetSettings() {
    if (!confirm('Are you sure you want to reset all settings to default values? This cannot be undone.')) {
        return;
    }

    try {
        // Load default settings from server
        const response = await fetchWithAuth('/admin/api/system-settings');
        const defaultSettings = await response.json();

        // Populate form with default settings
        populateForm(defaultSettings);

        showNotification('Settings reset to defaults. Click Save to apply.', 'info');
    } catch (err) {
        console.error('Failed to reset settings:', err);
        showNotification('Failed to reset settings', 'error');
    }
}

// Add a container for allowed attendance methods checkboxes
document.addEventListener('DOMContentLoaded', () => {
    const allowedMethodsContainer = document.createElement('div');
    allowedMethodsContainer.id = 'allowedAttendanceMethodsGroup';
    allowedMethodsContainer.className = 'checkbox-group';
    allowedMethodsContainer.innerHTML = `
        <label><input type="checkbox" value="QR_CODE"> QR Code Scan</label>
        <label><input type="checkbox" value="MANUAL_CODE"> Manual Code Entry</label>
        <label><input type="checkbox" value="LECTURER_MANUAL"> Lecturer Manual</label>
    `;

    // Find the allowed attendance methods field
    const attendanceSettingsSection = document.querySelector('#defaultAttendanceDuration')?.closest('.settings-section');
    if (attendanceSettingsSection) {
        const methodLabel = attendanceSettingsSection.querySelector('.form-group:has(label:contains("Allowed Attendance Methods"))');
        if (methodLabel) {
            const container = methodLabel.querySelector('.checkbox-group');
            if (container) container.innerHTML = allowedMethodsContainer.innerHTML;
        }
    }
});

// Make functions globally accessible
window.resetSettings = resetSettings;
window.initSystemSettings = initSystemSettings;

console.log('system-settings.js loaded successfully');
console.log('window.initSystemSettings exists:', typeof window.initSystemSettings);