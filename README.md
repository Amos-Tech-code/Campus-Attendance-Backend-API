# Campus Attendance System - Backend API

A robust, scalable backend built with **Ktor** to solve attendance management challenges in Kenyan universities. This system leverages mobile computing and real-time data to streamline attendance tracking for students and lecturers.

---

## 🚀 Overview
The Campus Attendance System is designed to automate the process of taking and managing attendance. It provides a secure, efficient, and transparent way to track student presence in lectures, reducing manual errors and "buddy signing" through device-specific tracking and secure authentication.

---

## 🛠️ Tech Stack
- **Framework:** [Ktor 3.3.0](https://ktor.io/)
- **Language:** Kotlin 2.2.20
- **Database:** PostgreSQL with [Exposed ORM](https://github.com/JetBrains/Exposed)
- **Dependency Injection:** [Koin](https://insert-koin.io/)
- **Authentication:** JWT (Standard & Admin) & Google OAuth2
- **Serialization:** Kotlinx Serialization (JSON)
- **Real-time:** Server-Sent Events (SSE) for live attendance updates
- **Storage:** Cloudinary SDK
- **Notifications:** Firebase Admin SDK
- **Document Generation:** iText7 (PDF) & OpenCSV (CSV)
- **Security:** BCrypt for password hashing & Rate Limiting

---

## ✨ Features & Architecture

The project is structured with a modular plugin-based architecture located in `src/main/kotlin/plugins`:

| Plugin | Responsibility |
| :--- | :--- |
| **Authentication** | Dual JWT strategy (User/Admin) and Google OAuth integration. |
| **Database** | Connection management (HikariCP), Migrations, Schema creation, and Seeding. |
| **Routing** | Comprehensive API surface covering Auth, Attendance, Enrollment, and Admin. |
| **ExceptionHandler**| Global error handling and standardized API responses via `StatusPages`. |
| **Administration** | Request Rate Limiting to prevent API abuse. |
| **Serialization** | Content negotiation configured for optimized JSON processing. |
| **SSE** | Real-time event streaming for live lecture attendance monitoring. |
| **Monitoring** | Structured `CallLogging` with custom formatting for request auditing. |
| **HTTP** | CORS configuration for secure cross-origin resource sharing. |

### Core Modules
- **Attendance Management:** Create sessions, mark attendance with QR codes/location, and monitor live updates.
- **Academic Setup:** Manage courses, departments, and university structures.
- **Device Management:** Strict device binding to prevent fraudulent attendance marking.
- **Reporting:** Export detailed attendance reports in PDF and CSV formats.
- **Admin Dashboard:** Centralized control for system settings, user management, and suspicious activity monitoring.

---

## 🛠️ Building & Running

### Prerequisites
- JDK 17
- PostgreSQL Instance

### Environment Setup
The application uses `dotenv-kotlin` to manage environment variables across different stages. Create environment-specific files in the root directory: `.env.dev` or `.env.prod`.

**Switching Environments:**
- Using JVM flag: `-Denv=prod`
- Using OS Environment Variable: `APP_ENV=prod`
- *Default is `dev` (loads `.env.dev`)*

#### Required Variables Template
```env
# Server
SERVER_HOST=
SERVER_PORT=

# Database
DB_HOST=
DB_PORT=
DB_NAME=
DB_USER=
DB_PASSWORD=

# JWT
JWT_SECRET=
JWT_ISSUER=
JWT_AUDIENCE=
JWT_REALM=
JWT_EXPIRATION=

# Google OAuth
GOOGLE_CLIENT_ID=your_google_id
GOOGLE_CLIENT_SECRET=your_google_secret

# Cloudinary
CLOUDINARY_NAME=your_name
CLOUDINARY_API_KEY=your_key
CLOUDINARY_API_SECRET=your_secret

# Firebase
FIREBASE_CONFIG=path/to/firebase-config.json
```

### Gradle Tasks
| Task | Description |
| :--- | :--- |
| `./gradlew run` | Start the development server |
| `./gradlew build` | Compile and build the project |
| `./gradlew test` | Run unit and integration tests |
| `./gradlew buildFatJar` | Build an executable standalone JAR |

---

## 📡 API Check
Once the server is running, you can verify its status at:
`GET http://localhost:8080/health/status`

---
*Developed as a practical solution for real-world educational problems.*


---
