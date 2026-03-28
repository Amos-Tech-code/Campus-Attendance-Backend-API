# Campus Attendance System - API Documentation

This document outlines the API endpoints available in the Campus Attendance System.

---

## 📊 API Summary
The system provides a total of **122** API endpoints:

- **General & System:** 4
- **Admin API (Management & Security):** 65
- **User Authentication (Lecturers/Students):** 4
- **Account & Profile Management:** 4
- **Lecturer Academic Setup:** 13
- **Attendance Sessions (Lifecycle):** 6
- **Attendance Marking (Real-time):** 3
- **Attendance Records & Exports:** 6
- **Student Enrollment:** 4
- **Device Management (Security):** 5
- **Student Lookup:** 1
- **Notifications Inbox:** 7

---

## 🛠️ General & System Endpoints

### 1. Web Application (SPA)
Serves the main frontend application.
- **Path:** `/`
- **Default Page:** `index.html`
- **Source:** `static` resources folder

### 2. Static Resources
Direct access to static assets used by the application and admin dashboard.
- **Path:** `/static/...` - Serves assets from the `static` package (CSS, JS, Images).
- **Path:** `/templates/...` - Serves HTML templates from the `templates` package (Admin dashboard layouts, email templates, etc.).

### 3. API Status
Check if the API is running and healthy.
- **Endpoint:** `GET /health/status`
- **Auth Required:** None
- **Response:**
  ```json
  {
    "statusCode": 200,
    "message": "Campus Attendance API is running"
  }
  ```

---

## 🔐 Admin API

The Admin API is prefixed with `/admin/api`. Most endpoints require `admin-jwt` authentication.

### 1. Authentication

#### Admin Login
Authenticates an administrator and returns access/refresh tokens.
- **Endpoint:** `POST /admin/api/login`
- **Request Body:** `AdminLoginRequest`
- **Response:** `AdminAuthResponse`

#### Refresh Token
Generates a new access token using a valid refresh token.
- **Endpoint:** `POST /admin/api/refresh`
- **Request Body:** `RefreshTokenRequest`

#### Logout
Invalidates the current session.
- **Endpoint:** `POST /admin/api/logout`
- **Auth:** Required (`admin-jwt`)

---

### 2. Dashboard & Statistics

#### Get Dashboard Stats
Retrieves an overview of system statistics for the logged-in admin.
- **Endpoint:** `GET /admin/api/dashboard`
- **Auth:** Required (`admin-jwt`)
- **Response:** `DashboardStatsDto`

---

### 3. Administrator Management

#### List & Create Admins
- `GET /admin/api/admins` - List all administrators.
- `POST /admin/api/admins` - Create a new administrator (`CreateAdminRequest`).

#### Admin Operations
- `GET /admin/api/admins/{id}` - Get details of a specific admin.
- `PUT /admin/api/admins/{id}` - Update admin details (`UpdateAdminRequest`).
- `DELETE /admin/api/admins/{id}` - Delete an administrator account (cannot delete self or the last admin).
- `POST /admin/api/admins/{id}/reset-password` - Reset password for an admin.

---

### 4. Lecturer & Student Management

#### Lecturer Operations
- `GET /admin/api/lecturers` - List lecturers (Query params: `page`, `pageSize`, `search`, `status`).
- `GET /admin/api/lecturers/{id}` - Detailed lecturer profile.
- `PUT /admin/api/lecturers/{id}` - Update lecturer details.
- `DELETE /admin/api/lecturers/{id}` - Deactivate lecturer account.
- `POST /admin/api/lecturers/{id}/activate` - Re-activate a lecturer.

#### Student Operations
- `GET /admin/api/students` - List students (Query params: `page`, `pageSize`, `search`, `status`).
- `GET /admin/api/students/{id}` - Detailed student profile.
- `PUT /api/v1/account/profile/student` - Update student details.
- `DELETE /admin/api/students/{id}` - Deactivate student account.
- `POST /admin/api/students/{id}/activate` - Re-activate a student.

---

### 5. University Structure

#### Universities
- `GET /admin/api/universities` - List universities.
- `GET /admin/api/universities/{id}` - Get university details.
- `POST /admin/api/universities` - Create university (`CreateUniversityRequest`).
- `PUT /admin/api/universities/{id}` - Update university.
- `DELETE /admin/api/universities/{id}` - Delete university.

#### Departments
- `GET /admin/api/departments` - List departments (filter by `universityId`).
- `POST /admin/api/departments` - Create department.
- `PUT /admin/api/departments/{id}` - Update department.
- `DELETE /admin/api/departments/{id}` - Delete department.

#### Programmes
- `GET /admin/api/programmes` - List programmes (filter by `universityId`, `departmentId`, `activeOnly`).
- `POST /admin/api/programmes` - Create programme.
- `PUT /admin/api/programmes/{id}` - Update programme.
- `DELETE /admin/api/programmes/{id}` - Delete programme.

#### Units
- `GET /admin/api/units` - List units (filter by `universityId`, `departmentId`, `activeOnly`).
- `POST /api/v1/lecturer/academic-setup` - Create unit.
- `PUT /admin/api/units/{id}` - Update unit.
- `DELETE /admin/api/units/{id}` - Delete unit.
- `POST /admin/api/units/{id}/link-programme` - Link a unit to a specific programme and year.
- `DELETE /admin/api/units/{unitId}/programmes/{programmeId}` - Unlink a unit from a programme.

#### Academic Terms
- `GET /admin/api/academic-terms` - List all academic terms.
- `GET /admin/api/academic-terms/{id}` - Get details of a specific term.
- `POST /admin/api/academic-terms` - Create a new term (Semester 1 or 2).
- `PUT /admin/api/academic-terms/{id}` - Update term details.
- `DELETE /admin/api/academic-terms/{id}` - Delete a term.
- `POST /admin/api/academic-terms/{id}/set-active` - Set a term as the current active semester for its university.
- `GET /admin/api/universities/{universityId}/active-term` - Get the currently active term for a university.

---

### 6. Security & Device Management

#### Device Change Requests (Admin View)
- `GET /admin/api/device-change-requests` - List requests (filter by `status`, `studentId`, `search`).
- `GET /admin/api/device-change-requests/{id}` - Get request details.
- `POST /admin/api/device-change-requests/{id}/review` - Approve or reject a device change request.

#### Suspicious Activity
- `GET /admin/api/suspicious-activity` - List flagged activities (filter by `studentId`, `sessionId`, `unitId`).
- `GET /admin/api/suspicious-activity/{id}` - Get details of a flagged activity.
- `GET /admin/api/suspicious-activity/stats` - Summary statistics of suspicious activities.
- `POST /admin/api/suspicious-activity/{id}/review` - Review and mark activity as confirmed suspicious or clear it.

---

### 7. System & Maintenance

#### Storage Management
- `GET /admin/api/storage/files` - List stored files (filter by `type`: `all`, `orphaned`, `expired`).
- `GET /admin/api/storage/stats` - View detailed storage usage statistics.
- `POST /admin/api/storage/cleanup` - Perform batch cleanup of files (`CleanupRequest`).
- `DELETE /admin/api/storage/files/{fileId}` - Manually delete a specific file.

#### Notification Management
- `GET /admin/api/notifications/history` - View logs of sent notifications.
- `GET /admin/api/notifications/templates` - List notification message templates.
- `PUT /admin/api/notifications/templates/{type}` - Update a notification template.
- `POST /admin/api/notifications/broadcast` - Send a global notification broadcast to specific user groups.
- `GET /admin/api/notifications/stats` - View notification delivery statistics.

#### System Settings
- `GET /admin/api/system-settings` - Retrieve all global system configurations.
- `PUT /admin/api/system-settings` - Batch update system settings (`UpdateSystemSettingsRequest`).
- `GET /admin/api/system-settings/{key}` - Retrieve a specific setting value by key.

---

## 👤 User Authentication API (Students & Lecturers)

The Authentication API is prefixed with `api/v1/auth`.

### 1. Lecturer Authentication

#### Google Sign-in
Authenticates a lecturer using a Google ID Token.
- **Endpoint:** `POST /api/v1/auth/lecturers/google`
- **Request Body:** `GoogleSignInRequest`
- **Response:** `LecturerAuthResponse`

#### Mock Google Sign-in (Testing Only)
Simulates Google authentication without calling external APIs.
- **Endpoint:** `POST /api/v1/auth/mock/lecturers/google`

---

### 2. Student Authentication

#### Student Registration
Registers a new student in the system.
- **Endpoint:** `POST /api/v1/auth/students/register`
- **Request Body:** `StudentRegistrationRequest`
- **Response:** `StudentAuthResponse` (Status 201 Created)

#### Student Login
Authenticates a student and binds their account to the provided device info.
- **Endpoint:** `POST /api/v1/auth/students/login`
- **Request Body:** `StudentLoginRequest`
- **Response:** `StudentAuthResponse`

---

## ⚙️ Account & Profile Management

Base path: `api/v1/account`. Requires `jwt-auth`.

### 1. Profile Updates
- `PATCH /api/v1/account/profile/lecturer` - Update lecturer profile details (Role: `LECTURER`).
- `PATCH /api/v1/account/profile/student` - Update student profile details (Role: `STUDENT`).

### 2. Notification Tokens (FCM)
Update Firebase Cloud Messaging tokens for push notifications.
- `PATCH /api/v1/account/fcm-token/lecturer` - Update token for lecturer.
- `PATCH /api/v1/account/fcm-token/student` - Update token for student.

---

## 🏫 Lecturer Academic Setup

Base path: `api/v1/lecturer/academic-setup`. Requires `jwt-auth` and `LECTURER` role.

### 1. Setup Configuration
- `POST /api/v1/lecturer/academic-setup` - Initialize academic setup (Universities, Departments, Units).
- `GET /api/v1/lecturer/academic-setup` - Retrieve current academic setup. Supports optional `universityId` query param.
- `DELETE /api/v1/lecturer/academic-setup/universities/{universityId}/deactivate` - Lecturer deactivates themselves from a university.

### 2. Academic Term & Programme Management
- `POST /api/v1/lecturer/academic-setup/universities/{universityId}/terms` - Add a new academic term to a university.
- `POST /api/v1/lecturer/academic-setup/universities/{universityId}/programmes` - Add a new programme with units.
- `PATCH /api/v1/lecturer/academic-setup/programmes/{programmeId}` - Update programme details (name, year, etc.).
- `DELETE /api/v1/lecturer/academic-setup/programmes/{programmeId}` - Deactivate a programme (soft delete).

### 3. Unit Management
- `POST /api/v1/lecturer/academic-setup/programmes/{programmeId}/units` - Add a new unit to a programme.
- `DELETE /api/v1/lecturer/academic-setup/programmes/{programmeId}/units/{unitId}` - Remove a unit from a programme.

### 4. Setup Suggestions
Helpers for finding entities during configuration.
- `GET /api/v1/lecturer/academic-setup/suggestions/universities` - Search for universities.
- `GET /api/v1/lecturer/academic-setup/suggestions/departments` - Search departments (requires `universityId`).
- `GET /api/v1/lecturer/academic-setup/suggestions/programmes` - Search programmes (requires `universityId`).
- `GET /api/v1/lecturer/academic-setup/suggestions/units` - Search units (requires `universityId`).

---

## 📅 Attendance Sessions

Base path: `api/v1/session`. Requires `jwt-auth`.

### 1. Lecturer Session Management (Role: `LECTURER`)
- `POST /api/v1/session/start` - Start a new attendance session for a unit (`StartSessionRequest`).
- `PATCH /api/v1/session/{sessionId}` - Update session details like duration or coordinates (`UpdateSessionRequest`).
- `POST /api/v1/session/end` - Manually end an active session (`EndSessionRequest`).
- `GET /api/v1/session/active` - Get the lecturer's currently running session.
- `GET /api/v1/session/history` - List previous sessions (Supports `page` and `size`).

### 2. Student Session Interaction (Role: `STUDENT`)
- `POST /api/v1/session/verify` - Verify if a student can mark attendance for a session based on OTP/Location (`VerifySessionRequest`).

---

## ✅ Attendance Marking

Base path: `api/v1/attendance`. Requires `jwt-auth`.

### 1. Marking Endpoints
- `POST /api/v1/attendance/mark` - Student marks their own attendance (`MarkAttendanceRequest`). Role: `STUDENT`.
- `POST /api/v1/attendance/lecturer-mark` - Lecturer manually marks attendance for a student (`LecturerMarkAttendanceRequest`). Role: `LECTURER`.

### 2. Live Monitoring (SSE)
- `GET /api/v1/attendance/{sessionId}/live` - Real-time stream of attendance events for a specific session.
- **Auth:** `LECTURER` only.
- **Protocol:** Server-Sent Events (SSE).
- **Events:** `INITIAL_STATE`, `ATTENDANCE_MARKED`.

---

## 📊 Attendance Management & Records

Base path: `api/v1/attendance-manage/record`. Requires `jwt-auth`.

### 1. Record Operations
- `DELETE /api/v1/attendance-manage/record` - Lecturer removes a student's attendance record (`RemoveAttendanceRequest`). Role: `LECTURER`.
- `GET /api/v1/attendance-manage/record` - Student retrieves their own attendance history (Supports `page`, `size`, `sort`). Role: `STUDENT`.
- `GET /api/v1/attendance-manage/record/students/stats` - Student retrieves their summary attendance statistics. Role: `STUDENT`.

### 2. Reports & Exports (Role: `LECTURER`)
Base path: `api/v1/attendance-manage/record/export`.
- `POST /api/v1/attendance-manage/record/export` - Generate a new attendance report (`AttendanceExportRequest`).
- `GET /api/v1/attendance-manage/record/export/{exportId}` - Retrieve details or download link for a specific export.
- `GET /api/v1/attendance-manage/record/export` - List all previous exports by the lecturer (Supports `page`, `size`).

---

## 📝 Student Enrollment

Base path: `api/v1/students/enrollment`. Requires `jwt-auth` and `STUDENT` role.

### 1. Enrollment Management
- `POST /api/v1/students/enrollment` - Enroll in a programme (`StudentEnrollmentRequest`).
- `GET /api/v1/students/enrollment` - List all active enrollments for the logged-in student.
- `DELETE /api/v1/students/enrollment/{enrollmentId}` - Deactivate a specific enrollment.
- `PATCH /api/v1/students/enrollment/{enrollmentId}/year` - Update the year of study for an enrollment (`UpdateYearRequest`).

---

## 📱 Device Management

Base path: `api/v1/device-change`. Requires `jwt-auth`.

### 1. Student Requests (Role: `STUDENT`)
- `POST /api/v1/device-change/student/change-request` - Submit a request to change the bound device (`StudentDeviceChangeRequest`).
- `GET /api/v1/device-change/student/history` - View personal device change request history.
- `PATCH /api/v1/device-change/student/cancel-request/{requestId}` - Cancel a pending device change request.

### 2. Lecturer Review (Role: `LECTURER`)
- `GET /api/v1/device-change/lecturer/pending` - List all pending device change requests from students in the lecturer's departments.
- `POST /api/v1/device-change/lecturer/review` - Approve or reject a student's request (`DeviceChangeApprovalRequest`).

---

## 🔍 Student Lookup

Base path: `api/v1/lecturer/student`. Requires `jwt-auth` and `LECTURER` role.

### 1. Search Operations
- `POST /api/v1/lecturer/student/lookup` - Search for a student by registration number to view their profile or enrollment status (`StudentLookupRequest`).

---

## 🔔 Notifications

Base path: `api/v1/notifications`. Requires `jwt-auth`.

### 1. Retrieval
- `GET /api/v1/notifications/unread` - Get unread notifications for the logged-in user (Supports `limit`).
- `GET /api/v1/notifications/history` - Get paginated notification history (Supports `page`, `pageSize`).
- `GET /api/v1/notifications/{id}` - Get a specific notification by ID.
- `GET /api/v1/notifications/count` - Get unread notification counts.

### 2. Actions
- `PATCH /api/v1/notifications/{id}/read` - Mark a specific notification as read.
- `PATCH /api/v1/notifications/read-all` - Mark all notifications as read.
- `DELETE /api/v1/notifications/{id}` - Delete a notification.

---
*Note: All protected endpoints require a valid JWT in the `Authorization: Bearer <token>` header.*
