package com.amos_tech_code

import com.amos_tech_code.plugins.*
import io.ktor.server.application.*
import plugins.configureRouting
import plugins.configureSSE

fun main(args: Array<String>) {

    io.ktor.server.netty.EngineMain.main(args)

}


fun Application.module() {
    configureAdministration()
    configureFrameworks()
    configureSerialization()
    configureDatabase()
    configureMonitoring()
    configureAuthentication()
    configureHTTP()
    configureExceptionHandler()
    configureRouting()
    configureSSE()
}
