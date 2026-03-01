package di

import com.amos_tech_code.data.repository.AdminRepository
import com.amos_tech_code.data.repository.AttendanceExportRepository
import com.amos_tech_code.data.repository.AttendanceRecordRepository
import data.repository.AttendanceSessionRepository
import com.amos_tech_code.data.repository.LecturerAcademicRepository
import com.amos_tech_code.data.repository.LecturerRepository
import data.repository.StudentEnrollmentRepository
import com.amos_tech_code.data.repository.StudentRepository
import com.amos_tech_code.domain.services.AccountService
import com.amos_tech_code.domain.services.AttendanceEventBus
import com.amos_tech_code.domain.services.AttendanceExportService
import com.amos_tech_code.domain.services.AttendanceManagementService
import com.amos_tech_code.services.MarkAttendanceService
import domain.services.AttendanceSessionService
import com.amos_tech_code.services.AuthService
import com.amos_tech_code.domain.services.CloudStorageService
import com.amos_tech_code.domain.services.CsvGeneratorService
import com.amos_tech_code.services.GoogleAuthService
import com.amos_tech_code.services.LecturerAcademicService
import com.amos_tech_code.domain.services.LiveAttendanceService
import com.amos_tech_code.domain.services.PdfGeneratorService
import com.amos_tech_code.domain.services.impl.AccountServiceImpl
import com.amos_tech_code.domain.services.impl.AdminAuthService
import com.amos_tech_code.domain.services.impl.AdminDashboardService
import com.amos_tech_code.domain.services.impl.AttendanceExportServiceImpl
import com.amos_tech_code.domain.services.impl.AttendanceManagementServiceImpl
import com.amos_tech_code.domain.services.impl.CsvGeneratorServiceImpl
import com.amos_tech_code.domain.services.impl.PdfGeneratorServiceImpl
import domain.services.impl.AttendanceEventBusImpl
import com.amos_tech_code.services.QRCodeService
import com.amos_tech_code.services.SessionCodeGenerator
import com.amos_tech_code.services.StudentEnrollmentService
import domain.services.impl.MarkAttendanceServiceImpl
import domain.services.impl.AttendanceSessionServiceImpl
import domain.services.impl.AuthServiceImpl
import domain.services.impl.CloudStorageServiceImpl
import domain.services.impl.GoogleAuthServiceImpl
import domain.services.impl.LecturerAcademicServiceImpl
import domain.services.impl.LiveAttendanceServiceImpl
import domain.services.impl.QRCodeServiceImpl
import domain.services.impl.SessionCodeGeneratorImpl
import domain.services.impl.StudentEnrollmentServiceImpl
import com.amos_tech_code.utils.BackgroundTaskScope
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import org.koin.dsl.module

val appModule = module {

    single<HttpClient> {
        HttpClient(CIO)
    }

    single { BackgroundTaskScope() }

    /*-----------------------------------------
       SERVICES
    ------------------------------------------*/

    single<GoogleAuthService> { GoogleAuthServiceImpl(get()) }

    single<AuthService> {
        AuthServiceImpl(get(), get(), get())
    }

    single<AccountService> { AccountServiceImpl(get(), get()) }

    single<LecturerAcademicService> { LecturerAcademicServiceImpl(get()) }

    single<AttendanceSessionService> {
        AttendanceSessionServiceImpl(
            get(), get(), get(), get(), get()
        )
    }

    single<QRCodeService> { QRCodeServiceImpl() }

    single<SessionCodeGenerator> { SessionCodeGeneratorImpl() }

    single<CloudStorageService> { CloudStorageServiceImpl() }

    single<MarkAttendanceService> { MarkAttendanceServiceImpl(
        attendanceSessionRepository = get(),
        studentRepository = get(),
        studentEnrollmentRepository = get(),
        attendanceEventBus = get(),
        backgroundTaskScope = get()
    )}

    single<StudentEnrollmentService> { StudentEnrollmentServiceImpl(get()) }

    single<LiveAttendanceService> { LiveAttendanceServiceImpl(get(), get()) }

    single<AttendanceEventBus> { AttendanceEventBusImpl() }

    single<AttendanceManagementService> { AttendanceManagementServiceImpl(get(), get()) }

    single<AttendanceExportService> { AttendanceExportServiceImpl(get(), get(), get(), get()) }

    single<PdfGeneratorService> { PdfGeneratorServiceImpl() }

    single<CsvGeneratorService> { CsvGeneratorServiceImpl() }



    /*-----------------------------------------
          REPOSITORY
     ------------------------------------------*/

    single { StudentRepository() }

    single { LecturerRepository() }

    single { LecturerAcademicRepository() }

    single { AttendanceSessionRepository() }

    single { StudentEnrollmentRepository() }

    single { AttendanceRecordRepository() }

    single { AttendanceExportRepository() }


    /*-----------------------------------
    ADMIN SERVICES AND REPOSITORIES
    ------------------------------------*/

    single { AdminAuthService(get()) }

    single { AdminDashboardService(get()) }

    single { AdminRepository() }

}