package com.ryanthetechman.cherrycal.calendar_interaction.provider.google

import io.ktor.http.ContentType
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.coroutines.CompletableDeferred
import java.awt.Desktop
import java.net.URI

actual fun openUrlInBrowser(url: String) {
    if (Desktop.isDesktopSupported()) {
        Desktop.getDesktop().browse(URI(url))
    } else {
        println("Desktop not supported. Please open the following URL manually: $url")
    }
}

actual suspend fun waitForGoogleOAuthRedirect(expectedState: String): Pair<String?, String?> {
    val tokenPairDeferred = CompletableDeferred<Pair<String?, String?>>()

    // JavaScript to extract tokens from the URL fragment and redirect to /callback/token.
    val jsCode = """
        var fragment = window.location.hash;
        if (fragment) {
            var params = new URLSearchParams(fragment.substring(1)); 
            var idToken = params.get('id_token');
            var accessToken = params.get('access_token');
            var receivedState = params.get('state');
            var expectedState = '$expectedState'; 
            if (receivedState === expectedState) {
                window.location.href = '/callback/token?' + 
                    (idToken ? 'id_token=' + idToken : '') + 
                    (idToken && accessToken ? '&' : '') + 
                    (accessToken ? 'access_token=' + accessToken : '');
            } else {
                console.error('State does not match! Possible CSRF attack.');
                window.location.href = '/callback/token?id_token=null';
            }
        }
    """.trimIndent()

    val server = embeddedServer(Netty, port = 8080) {
        routing {
            // Serves a simple HTML page that runs the JS to extract tokens.
            get("/callback") {
                call.respondText(
                    """
                    <html>
                    <head>
                        <script>
                            $jsCode
                        </script>
                    </head>
                    <body>
                        <p>Please close this window.</p>
                    </body>
                    </html>
                    """.trimIndent(),
                    contentType = ContentType.Text.Html
                )
            }

            get("/callback/token") {
                val idToken = call.request.queryParameters["id_token"]
                val accessToken = call.request.queryParameters["access_token"]
                if (!idToken.isNullOrEmpty() && !accessToken.isNullOrEmpty()) {
                    call.respondText("Authorization complete. You can close this window.", contentType = ContentType.Text.Plain)
                    tokenPairDeferred.complete(Pair(idToken, accessToken))
                } else {
                    call.respondText("Authorization failed.", contentType = ContentType.Text.Plain)
                    tokenPairDeferred.complete(Pair(null, null))
                }
            }
        }
    }.start(wait = false)

    val tokenPair = tokenPairDeferred.await()
    server.stop(1000, 1000)
    return tokenPair
}