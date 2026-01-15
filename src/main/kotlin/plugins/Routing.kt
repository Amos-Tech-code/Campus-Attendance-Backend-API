package plugins

import com.amos_tech_code.domain.dtos.response.GenericResponseDto
import api.routes.attendanceSessionRoutes
import api.routes.authRoutes
import com.amos_tech_code.domain.services.AttendanceManagementService
import com.amos_tech_code.routes.lecturerAcademicSetupRoutes
import com.amos_tech_code.routes.studentEnrollmentRoutes
import com.amos_tech_code.services.AttendanceSessionService
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

    routing {

        get("/health/status") {
            call.respond(
                HttpStatusCode.OK,
                GenericResponseDto(
                    HttpStatusCode.OK.value,
                    "✅ SmartAttend API is running"
                )
            )
        }

        authRoutes(authService)

        authenticate("jwt-auth") {

            lecturerAcademicSetupRoutes(lecturerAcademicService)

            attendanceSessionRoutes(
                attendanceSessionService,
                markAttendanceService,
                liveAttendanceService,
                attendanceManagementService
            )

            studentEnrollmentRoutes(studentEnrollmentService)

        }

    }
}
