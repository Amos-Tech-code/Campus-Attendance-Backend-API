package domain.models

enum class UserRole {
    STUDENT, LECTURER, ADMIN // Admin for future impl
}

enum class DeviceStatus {
    ACTIVE,           // Registered and approved device
    PENDING,          // Waiting for approval
    REJECTED         // Rejected device change
}

enum class AttendanceMethod {
    QR_CODE, // Qr Code scan
    MANUAL_CODE, // Manually entering session code if allowed
    LECTURER_MANUAL, // Lecturer manually signing for student
    ANY // All methods allowed
}

enum class AttendanceSessionStatus {
    SCHEDULED, ACTIVE, ENDED, CANCELLED
}

enum class FlagType {
    LOCATION_MISMATCH, DEVICE_MISMATCH,
    METHOD_NOT_ALLOWED, SUSPICIOUS_DEVICE, OUTSIDE_SCHEDULE_WINDOW
}

enum class SeverityLevel {
    LOW, MEDIUM, HIGH, CRITICAL
}

enum class StudentEnrollmentSource {
    ATTENDANCE, SELF, MANUAL
}

enum class AttendanceSessionType {
    REGULAR, MAKEUP, SPECIAL
}

enum class LiveAttendanceEventType {
    INITIAL_STATE, ATTENDANCE_MARKED
}

enum class ExportFormat {
    PDF, CSV
}

enum class NotificationType {
    ATTENDANCE_MARKED,
    ATTENDANCE_REVOKED,
    DEVICE_APPROVED,
    DEVICE_REJECTED,
    DEVICE_REQUEST,
    SESSION_STARTED,
    SESSION_ENDED,
    SUSPICIOUS_ACTIVITY,
    SUPPORT_RESPONSE,
    SYSTEM_ALERT,
    ADMIN_ALERT
}

