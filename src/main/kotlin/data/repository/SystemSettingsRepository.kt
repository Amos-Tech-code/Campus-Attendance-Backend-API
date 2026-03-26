package com.amos_tech_code.data.repository

import com.amos_tech_code.api.dtos.admin.SystemSettingsResponse
import com.amos_tech_code.api.dtos.admin.UpdateSystemSettingsRequest
import com.amos_tech_code.data.database.utils.exposedTransaction
import java.util.concurrent.ConcurrentHashMap

class SystemSettingsRepository {

    // In-memory storage for settings (in production, this should be in a database table)
    private val settings = ConcurrentHashMap<String, Any>()

    init {
        // Initialize with default settings
        settings["defaultAttendanceDuration"] = 30
        settings["defaultLocationRadius"] = 50
        settings["allowedAttendanceMethods"] = listOf("QR_CODE", "MANUAL_CODE", "LECTURER_MANUAL")
        settings["requireLocationForAttendance"] = true
        settings["requireDeviceVerification"] = true
        settings["sessionCodeLength"] = 6
        settings["sessionCodeExpiryMinutes"] = 30
        settings["maxActiveSessionsPerLecturer"] = 3
        settings["maxLoginAttempts"] = 5
        settings["lockoutDurationMinutes"] = 30
        settings["passwordExpiryDays"] = 90
        settings["requireStrongPasswords"] = true
        settings["sessionTimeoutMinutes"] = 60
        settings["maxDevicesPerStudent"] = 2
        settings["deviceChangeRequiresApproval"] = true
        settings["autoApproveTrustedDevices"] = false
        settings["systemName"] = "Campus Attendance System"
        settings["systemEmail"] = "admin@campus.edu"
        settings["timezone"] = "Africa/Nairobi"
        settings["dateFormat"] = "dd/MM/yyyy"
        settings["timeFormat"] = "24h"
        settings["enablePushNotifications"] = true
        settings["enableEmailNotifications"] = false
        settings["notificationRetentionDays"] = 30
        settings["defaultReportFormat"] = "PDF"
        settings["autoGenerateReports"] = false
        settings["reportRetentionDays"] = 90
        settings["maintenanceMode"] = false
        settings["maintenanceMessage"] = ""
        settings["lastBackup"] = ""
    }

    suspend fun getSettings(): SystemSettingsResponse = exposedTransaction {
        SystemSettingsResponse(
            defaultAttendanceDuration = settings["defaultAttendanceDuration"] as? Int ?: 30,
            defaultLocationRadius = settings["defaultLocationRadius"] as? Int ?: 50,
            allowedAttendanceMethods = settings["allowedAttendanceMethods"] as? List<String> ?: listOf("QR_CODE", "MANUAL_CODE", "LECTURER_MANUAL"),
            requireLocationForAttendance = settings["requireLocationForAttendance"] as? Boolean ?: true,
            requireDeviceVerification = settings["requireDeviceVerification"] as? Boolean ?: true,
            sessionCodeLength = settings["sessionCodeLength"] as? Int ?: 6,
            sessionCodeExpiryMinutes = settings["sessionCodeExpiryMinutes"] as? Int ?: 30,
            maxActiveSessionsPerLecturer = settings["maxActiveSessionsPerLecturer"] as? Int ?: 3,
            maxLoginAttempts = settings["maxLoginAttempts"] as? Int ?: 5,
            lockoutDurationMinutes = settings["lockoutDurationMinutes"] as? Int ?: 30,
            passwordExpiryDays = settings["passwordExpiryDays"] as? Int ?: 90,
            requireStrongPasswords = settings["requireStrongPasswords"] as? Boolean ?: true,
            sessionTimeoutMinutes = settings["sessionTimeoutMinutes"] as? Int ?: 60,
            maxDevicesPerStudent = settings["maxDevicesPerStudent"] as? Int ?: 2,
            deviceChangeRequiresApproval = settings["deviceChangeRequiresApproval"] as? Boolean ?: true,
            autoApproveTrustedDevices = settings["autoApproveTrustedDevices"] as? Boolean ?: false,
            systemName = settings["systemName"] as? String ?: "Campus Attendance System",
            systemEmail = settings["systemEmail"] as? String ?: "admin@campus.edu",
            timezone = settings["timezone"] as? String ?: "Africa/Nairobi",
            dateFormat = settings["dateFormat"] as? String ?: "dd/MM/yyyy",
            timeFormat = settings["timeFormat"] as? String ?: "24h",
            enablePushNotifications = settings["enablePushNotifications"] as? Boolean ?: true,
            enableEmailNotifications = settings["enableEmailNotifications"] as? Boolean ?: false,
            notificationRetentionDays = settings["notificationRetentionDays"] as? Int ?: 30,
            defaultReportFormat = settings["defaultReportFormat"] as? String ?: "PDF",
            autoGenerateReports = settings["autoGenerateReports"] as? Boolean ?: false,
            reportRetentionDays = settings["reportRetentionDays"] as? Int ?: 90,
            maintenanceMode = settings["maintenanceMode"] as? Boolean ?: false,
            maintenanceMessage = settings["maintenanceMessage"] as? String ?: "",
            lastBackup = settings["lastBackup"] as? String
        )
    }

    suspend fun updateSettings(request: UpdateSystemSettingsRequest): Boolean = exposedTransaction {
        // Update Attendance Settings
        request.attendanceSettings?.let { att ->
            att.defaultAttendanceDuration?.let { settings["defaultAttendanceDuration"] = it }
            att.defaultLocationRadius?.let { settings["defaultLocationRadius"] = it }
            att.allowedAttendanceMethods?.let { settings["allowedAttendanceMethods"] = it }
            att.requireLocationForAttendance?.let { settings["requireLocationForAttendance"] = it }
            att.requireDeviceVerification?.let { settings["requireDeviceVerification"] = it }
        }

        // Update Security Settings
        request.securitySettings?.let { sec ->
            sec.maxLoginAttempts?.let { settings["maxLoginAttempts"] = it }
            sec.lockoutDurationMinutes?.let { settings["lockoutDurationMinutes"] = it }
            sec.passwordExpiryDays?.let { settings["passwordExpiryDays"] = it }
            sec.requireStrongPasswords?.let { settings["requireStrongPasswords"] = it }
            sec.sessionTimeoutMinutes?.let { settings["sessionTimeoutMinutes"] = it }
        }

        // Update Device Settings
        request.deviceSettings?.let { dev ->
            dev.maxDevicesPerStudent?.let { settings["maxDevicesPerStudent"] = it }
            dev.deviceChangeRequiresApproval?.let { settings["deviceChangeRequiresApproval"] = it }
            dev.autoApproveTrustedDevices?.let { settings["autoApproveTrustedDevices"] = it }
        }

        // Update System Preferences
        request.systemPreferences?.let { pref ->
            pref.systemName?.let { settings["systemName"] = it }
            pref.systemEmail?.let { settings["systemEmail"] = it }
            pref.timezone?.let { settings["timezone"] = it }
            pref.dateFormat?.let { settings["dateFormat"] = it }
            pref.timeFormat?.let { settings["timeFormat"] = it }
        }

        // Update Notification Settings
        request.notificationSettings?.let { notif ->
            notif.enablePushNotifications?.let { settings["enablePushNotifications"] = it }
            notif.enableEmailNotifications?.let { settings["enableEmailNotifications"] = it }
            notif.notificationRetentionDays?.let { settings["notificationRetentionDays"] = it }
        }

        // Update Report Settings
        request.reportSettings?.let { rep ->
            rep.defaultReportFormat?.let { settings["defaultReportFormat"] = it }
            rep.autoGenerateReports?.let { settings["autoGenerateReports"] = it }
            rep.reportRetentionDays?.let { settings["reportRetentionDays"] = it }
        }

        // Update Maintenance Settings
        request.maintenanceSettings?.let { maint ->
            maint.maintenanceMode?.let { settings["maintenanceMode"] = it }
            maint.maintenanceMessage?.let { settings["maintenanceMessage"] = it }
        }

        true
    }

    suspend fun getSetting(key: String): Any? = exposedTransaction {
        settings[key]
    }

    suspend fun isMaintenanceMode(): Boolean = exposedTransaction {
        settings["maintenanceMode"] as? Boolean ?: false
    }
}