package com.amos_tech_code.di

import com.amos_tech_code.data.repository.AdminDashboardRepository
import com.amos_tech_code.data.repository.AdminRepository
import com.amos_tech_code.data.repository.StorageManagementRepository
import com.amos_tech_code.data.repository.UniversityStructureRepository
import com.amos_tech_code.domain.services.impl.AdminAuthService
import com.amos_tech_code.domain.services.impl.AdminDashboardService
import com.amos_tech_code.domain.services.impl.AdminDeviceChangeService
import com.amos_tech_code.domain.services.impl.AdminManagementService
import com.amos_tech_code.domain.services.impl.LecturerStudentManagementService
import com.amos_tech_code.domain.services.impl.StorageManagementService
import com.amos_tech_code.domain.services.impl.SuspiciousActivityService
import com.amos_tech_code.domain.services.impl.UniversityStructureService
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

    single { UniversityStructureService(get()) }

    single { AdminDeviceChangeService(
        get(),
        get(),
        get(),
        get()
    ) }

    single { SuspiciousActivityService(get()) }

    single { StorageManagementService(get(), get()) }

    /*-----------------------------------
      REPOSITORIES
    ------------------------------------*/

    single { AdminRepository() }

    single { AdminDashboardRepository() }

    single { UniversityStructureRepository() }

    single { StorageManagementRepository() }
}