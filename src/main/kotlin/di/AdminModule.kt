package com.amos_tech_code.di

import com.amos_tech_code.data.repository.AdminManagementRepository
import com.amos_tech_code.data.repository.AdminRepository
import com.amos_tech_code.domain.services.impl.AdminAuthService
import com.amos_tech_code.domain.services.impl.AdminDashboardService
import com.amos_tech_code.domain.services.impl.AdminManagementService
import org.koin.dsl.module

val adminModule = module {

    /*-----------------------------------
      ADMIN SERVICES
    ------------------------------------*/

    single { AdminAuthService(get()) }

    single { AdminDashboardService(get()) }

    single { AdminManagementService(get()) }


    /*-----------------------------------
      ADMIN SERVICES
    ------------------------------------*/

    single { AdminRepository() }

    single { AdminManagementRepository() }
}