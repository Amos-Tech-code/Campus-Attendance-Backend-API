package plugins

import api.routes.attendanceRoutes
import api.dtos.response.GenericResponseDto
import api.routes.authRoutes
import api.routes.deviceChangeRoutes
import api.routes.sessionRoutes
import com.amos_tech_code.api.routes.accountRoutes
import com.amos_tech_code.api.routes.attendanceManagementRoutes
import com.amos_tech_code.domain.services.AccountService
import domain.services.AttendanceManagementService
import com.amos_tech_code.routes.lecturerAcademicSetupRoutes
import api.routes.studentEnrollmentRoutes
import com.amos_tech_code.api.routes.adminRoutes
import com.amos_tech_code.api.routes.notificationRoutes
import com.amos_tech_code.api.routes.studentLookUpRoute
import com.amos_tech_code.domain.services.AttendanceExportService
import domain.services.AttendanceSessionService
import com.amos_tech_code.services.AuthService
import com.amos_tech_code.services.LecturerAcademicService
import com.amos_tech_code.domain.services.LiveAttendanceService
import com.amos_tech_code.domain.services.NotificationService
import com.amos_tech_code.domain.services.StudentLookUpService
import com.amos_tech_code.domain.services.impl.AdminAuthService
import com.amos_tech_code.domain.services.impl.AdminDashboardService
import com.amos_tech_code.domain.services.impl.AdminManagementService
import com.amos_tech_code.domain.services.impl.LecturerStudentManagementService
import com.amos_tech_code.services.MarkAttendanceService
import com.amos_tech_code.services.StudentEnrollmentService
import domain.services.impl.DeviceChangeService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.auth.authenticate
import io.ktor.server.http.content.singlePageApplication
import io.ktor.server.http.content.staticResources
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import org.koin.ktor.ext.inject

fun Application.configureRouting() {

    val authService by inject<AuthService>()
    val lecturerAcademicService by inject<LecturerAcademicService>()
    val attendanceSessionService by inject<AttendanceSessionService>()
    val markAttendanceService by inject<MarkAttendanceService>()
    val studentEnrollmentService by inject<StudentEnrollmentService>()
    val liveAttendanceService by inject<LiveAttendanceService>()
    val attendanceManagementService by inject<AttendanceManagementService>()
    val accountService by inject<AccountService>()
    val attendanceExportService by inject<AttendanceExportService>()
    val notificationService by inject<NotificationService>()
    val deviceChangeService by inject<DeviceChangeService>()
    val studentLookupService by inject<StudentLookUpService>()

    val adminAuthService by inject<AdminAuthService>()
    val adminDashboardService by inject<AdminDashboardService>()
    val adminManagementService by inject<AdminManagementService>()
    val lecturerStudentManagementService by inject<LecturerStudentManagementService>()

    routing {

        singlePageApplication {
            useResources = true
            filesPath = "static" // Folder in resources
            defaultPage = "index.html"
        }

        get("/health/status") {
            call.respond(
                HttpStatusCode.OK,
                GenericResponseDto(
                    HttpStatusCode.OK.value,
                    "Campus Attendance API is running"
                )
            )
        }

        authRoutes(authService)

        authenticate("jwt-auth") {

            accountRoutes(accountService)

            lecturerAcademicSetupRoutes(lecturerAcademicService)

            sessionRoutes(attendanceSessionService)

            attendanceRoutes(
                markAttendanceService,
                liveAttendanceService
            )

            attendanceManagementRoutes(
                attendanceManagementService,
                attendanceExportService
            )

            studentEnrollmentRoutes(studentEnrollmentService)

            deviceChangeRoutes(deviceChangeService)

            studentLookUpRoute(studentLookupService)

        }

        authenticate("jwt-auth", "admin-jwt") {
            notificationRoutes(notificationService)
        }

        adminRoutes(
            adminAuthService,
            adminDashboardService,
            adminManagementService,
            lecturerStudentManagementService
        )

        // Serve static files
        staticResources("/static", basePackage = "static")

        // Serve admin templates
        staticResources("/templates", basePackage = "templates")

    }
}
