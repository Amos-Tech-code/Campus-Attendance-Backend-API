package com.amos_tech_code.di

import com.amos_tech_code.data.repository.AdminDashboardRepository
import com.amos_tech_code.data.repository.AdminRepository
import com.amos_tech_code.domain.services.impl.AdminAuthService
import com.amos_tech_code.domain.services.impl.AdminDashboardService
import com.amos_tech_code.domain.services.impl.AdminManagementService
import com.amos_tech_code.domain.services.impl.LecturerStudentManagementService
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

    /*-----------------------------------
      ADMIN SERVICES
    ------------------------------------*/

    single { AdminRepository() }

    single { AdminDashboardRepository() }
}