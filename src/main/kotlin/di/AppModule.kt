package di

import com.amos_tech_code.data.repository.*
import com.amos_tech_code.domain.services.*
import com.amos_tech_code.domain.services.impl.*
import com.amos_tech_code.services.*
import com.amos_tech_code.utils.BackgroundTaskScope
import data.repository.AttendanceSessionRepository
import data.repository.DeviceChangeRequestRepository
import data.repository.LecturerAcademicRepository
import data.repository.StudentEnrollmentRepository
import data.repository.StudentRepository
import domain.services.AttendanceManagementService
import domain.services.AttendanceSessionService
import domain.services.impl.*
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
        AuthServiceImpl(
            get(),
            get(),
            get(),
            get(),
            get()
        )
    }

    single<AccountService> { AccountServiceImpl(get(), get()) }

    single<LecturerAcademicService> { LecturerAcademicServiceImpl(get()) }

    single<AttendanceSessionService> {
        AttendanceSessionServiceImpl(
            get(), get(), get(),
            get(), get(), get(), get()
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
        backgroundTaskScope = get(),
        notificationService = get()
    )}

    single<StudentEnrollmentService> { StudentEnrollmentServiceImpl(get()) }

    single<LiveAttendanceService> { LiveAttendanceServiceImpl(get(), get()) }

    single<AttendanceEventBus> { AttendanceEventBusImpl() }

    single<AttendanceManagementService> { AttendanceManagementServiceImpl(get(), get(), get(), get()) }

    single<AttendanceExportService> { AttendanceExportServiceImpl(get(), get(), get(), get()) }

    single<PdfGeneratorService> { PdfGeneratorServiceImpl() }

    single<CsvGeneratorService> { CsvGeneratorServiceImpl() }

    single { NotificationService(get(), get(), get()) }

    single {
        DeviceChangeService(
        get(),
        get(),
        get(),
        get(),
        get(),
        get()
        )
    }

    single<StudentLookUpService> {
        StudentLookUpServiceImpl(
        get(),
        get(),
        get(),
        get(),
            get(),
            get()
        )
    }

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

    single { NotificationRepository() }

    single { DeviceChangeRequestRepository() }

}