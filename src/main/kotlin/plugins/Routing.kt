package plugins

import api.routes.attendanceRoutes
import com.amos_tech_code.domain.dtos.response.GenericResponseDto
import api.routes.authRoutes
import api.routes.sessionRoutes
import com.amos_tech_code.api.routes.accountRoutes
import com.amos_tech_code.api.routes.attendanceManagementRoutes
import com.amos_tech_code.domain.services.AccountService
import com.amos_tech_code.domain.services.AttendanceManagementService
import com.amos_tech_code.routes.lecturerAcademicSetupRoutes
import api.routes.studentEnrollmentRoutes
import com.amos_tech_code.domain.services.AttendanceExportService
import domain.services.AttendanceSessionService
import com.amos_tech_code.services.AuthService
import com.amos_tech_code.services.LecturerAcademicService
import com.amos_tech_code.domain.services.LiveAttendanceService
import com.amos_tech_code.services.MarkAttendanceService
import com.amos_tech_code.services.StudentEnrollmentService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.auth.authenticate
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

    routing {

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

        }

    }
}
