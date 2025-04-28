package com.ryanthetechman.cherrycal.calendar_interaction.provider

import io.ktor.http.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.coroutines.delay

/**
 * Waits for the OAuth redirect and returns the authorization code.
 */
actual suspend fun waitForRedirect(): String {
    var authCode: String? = null
    val port = 8080

    // Start the embedded HTTP server
    val server = embeddedServer(Netty, port = port) {
        routing {
            get("/callback/token") {
                // Expect the authorization code as a query parameter "code"
                authCode = call.request.queryParameters["code"]
                call.respondText(
                    "Authorization successful! You can close this window.",
                    ContentType.Text.Html
                )
            }
        }
    }.start(wait = false)

    // Wait until the code is received
    while (authCode == null) {
        delay(500) // Delay to avoid busy-waiting
    }

    server.stop(1000, 2000)
    return authCode!!
}