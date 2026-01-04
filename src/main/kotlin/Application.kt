package com.amos_tech_code

import com.amos_tech_code.plugins.*
import io.ktor.server.application.*
import plugins.configureRouting
import plugins.configureSSE
import plugins.configureSockets

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
    configureSockets()
    configureRouting()
    configureSSE()
}
