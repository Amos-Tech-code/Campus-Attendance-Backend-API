package com.amos_tech_code.plugins

import com.amos_tech_code.di.adminModule
import di.appModule
import io.ktor.server.application.*
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger

fun Application.configureKoin() {

    install(Koin) {
        slf4jLogger()
        modules(
            listOf(
                appModule,
                adminModule,
            )
        )
    }

}
