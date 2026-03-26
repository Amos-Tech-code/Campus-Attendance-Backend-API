package com.amos_tech_code.di

import com.amos_tech_code.data.repository.AdminDashboardRepository
import com.amos_tech_code.data.repository.AdminRepository
import com.amos_tech_code.data.repository.NotificationManagementRepository
import com.amos_tech_code.data.repository.StorageManagementRepository
import com.amos_tech_code.data.repository.SystemSettingsRepository
import com.amos_tech_code.data.repository.UniversityStructureRepository
import com.amos_tech_code.domain.services.impl.AdminAuthService
import com.amos_tech_code.domain.services.impl.AdminDashboardService
import com.amos_tech_code.domain.services.impl.AdminDeviceChangeService
import com.amos_tech_code.domain.services.impl.AdminManagementService
import com.amos_tech_code.domain.services.impl.LecturerStudentManagementService
import com.amos_tech_code.domain.services.impl.NotificationManagementService
import com.amos_tech_code.domain.services.impl.StorageManagementService
import com.amos_tech_code.domain.services.impl.SuspiciousActivityService
import com.amos_tech_code.domain.services.impl.SystemSettingsService
import com.amos_tech_code.domain.services.impl.UniversityStructureService
import org.koin.core.scope.get
import org.koin.dsl.module

val adminModule = module {

    /*-----------------------------------
      ADMIN SERVICES
    ------------------------------------*/

    single { AdminAuthService(get()) }

    single {
        AdminDashboardService(
        get(),
        get()
        )
    }

    single { AdminManagementService(get()) }

    single { LecturerStudentManagementService(get(), get()) }

    single { UniversityStructureService(get(), get()) }

    single { AdminDeviceChangeService(
        get(),
        get(),
        get(),
        get()
    ) }

    single { SuspiciousActivityService(get()) }

    single { StorageManagementService(get(), get()) }

    single { NotificationManagementService(get(), get()) }

    single { SystemSettingsService(get()) }

    /*-----------------------------------
      REPOSITORIES
    ------------------------------------*/

    single { AdminRepository() }

    single { AdminDashboardRepository() }

    single { UniversityStructureRepository() }

    single { StorageManagementRepository() }

    single { NotificationManagementRepository() }

    single { SystemSettingsRepository() }
}