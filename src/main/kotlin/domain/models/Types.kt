package domain.models

enum class UserRole {
    STUDENT, LECTURER, ADMIN // Admin for future impl
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

