package com.ryanthetechman.cherrycal.calendar_interaction.provider.google

import com.appstractive.jwt.JWT
import com.appstractive.jwt.from
import com.ryanthetechman.cherrycal.calendar_interaction.CalendarEvent
import com.ryanthetechman.cherrycal.calendar_interaction.DateRange
import com.ryanthetechman.cherrycal.calendar_interaction.Calendar
import com.ryanthetechman.cherrycal.calendar_interaction.provider.AccountProvider
import com.ryanthetechman.cherrycal.calendar_interaction.provider.AccountStorage
import com.ryanthetechman.cherrycal.calendar_interaction.provider.ProviderAccount
import com.ryanthetechman.cherrycal.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.encodeURLParameter
import io.ktor.serialization.JsonConvertException
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.kotlincrypto.random.CryptoRand
import org.kotlincrypto.random.RandomnessProcurementException
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

expect fun openUrlInBrowser(url: String)
expect suspend fun waitForGoogleOAuthRedirect(expectedState: String): Pair<String?, String?>

// TODO: Make provider also save the refresh token so we can refresh the access token
object GoogleAccountProvider : AccountProvider {
    override val providerName: String = "Google"

    // Constants for Calendar API
    private const val CALENDAR_BASE_URL = "https://www.googleapis.com/calendar/v3/calendars"
    private const val CALENDAR_LIST_URL = "https://www.googleapis.com/calendar/v3/users/me/calendarList"

    // Constants for OAuth flow
    private const val AUTH_URL = "https://accounts.google.com/o/oauth2/v2/auth"
    private const val CLIENT_ID = "" // Enter your client ID here
    private const val REDIRECT_URI = "http://localhost:8080/callback"
    private const val RESPONSE_TYPE = "id_token token"
    private const val SCOPE = "email https://www.googleapis.com/auth/calendar" // TODO: rather than email, use the account identifier

    private val client = HttpClient().config {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    override suspend fun configureAccount(): ProviderAccount? {
        return signIn()
    }

    override suspend fun readEvents(account: ProviderAccount, calendar: Calendar, dateRange: DateRange): Result<List<CalendarEvent>> {
        val accessToken = account.token ?: return Result.failure(Exception("No Access Token"))

        val response: HttpResponse = client.get("$CALENDAR_BASE_URL/${calendar.id}/events") {
            bearerAuth(accessToken)
            url {
                parameters.append("timeMin", dateRange.start.toString())
                parameters.append("timeMax", dateRange.end.toString())
                parameters.append("singleEvents", "true") // Expand recurring events
            }
        }

        return if (response.status == HttpStatusCode.OK) {
            try {
                val data = response.body<GoogleCalendarResponse>()
                val filtered = data.items.filter { it.status != "cancelled" }
                Result.success(filtered.map { it.asCalendarEvent() })
            }
            catch (e: JsonConvertException) {
                println("Failed to parse Google Calendar API response: ${response.bodyAsText()}")
                throw e
            }
        } else {
            Result.failure(Exception("Failed to fetch events: ${response.status}"))
        }
    }

    override suspend fun writeEvent(account: ProviderAccount, calendar: Calendar, event: CalendarEvent): Result<CalendarEvent> {
        val accessToken = account.token ?: return Result.failure(Exception("No Access Token"))

        val googleEvent = event.asGoogleCalendarEvent()
            .copy(sendUpdates = "none") // TODO: Temp, prevents email spam while testing

        val response: HttpResponse =
            if (googleEvent.id.isNullOrEmpty()) {
                client.post("$CALENDAR_BASE_URL/${calendar.id}/events"){
                    contentType(ContentType.Application.Json)
                    bearerAuth(accessToken)
                    setBody(googleEvent)
                }
            } else {
                client.put("$CALENDAR_BASE_URL/${calendar.id}/events/${googleEvent.id}") {
                    contentType(ContentType.Application.Json)
                    bearerAuth(accessToken)
                    setBody(googleEvent)
                }
            }


        return if (response.status == HttpStatusCode.OK) {
            return Result.success(response.body<GoogleCalendarEvent>().asCalendarEvent())
        } else {
            Result.failure(Exception("Failed to create event: ${response.status}"))
        }
    }

    override suspend fun deleteEvent(account: ProviderAccount, calendar: Calendar, eventId: String): Result<Boolean> {
        val accessToken = account.token ?: return Result.failure(Exception("No Access Token"))

        val response: HttpResponse = client.delete("$CALENDAR_BASE_URL/${calendar.id}/events/$eventId") {
            bearerAuth(accessToken)
        }

        return if (response.status == HttpStatusCode.NoContent) {
            Result.success(true)
        } else {
            Result.failure(Exception("Failed to delete event"))
        }
    }

    override suspend fun getCalendars(account: ProviderAccount): Result<List<Calendar>> {
        val accessToken = account.token ?: return Result.failure(Exception("No Access Token"))

        val response: HttpResponse = client.get(CALENDAR_LIST_URL) {
            bearerAuth(accessToken)
        }

        return if (response.status == HttpStatusCode.OK) {
            val data = response.body<GoogleCalendarListResponse>()
            Result.success(data.items.map { it.asCalendar() })
        } else {
            Result.failure(Exception("Failed to fetch calendar list: ${response.status}"))
        }
    }

    /**
     * Performs the OAuth flow to sign in and returns the account identifier (e.g. email) on success.
     */
    private suspend fun signIn(): ProviderAccount? {
        val state = generateRandomString(32)
        val nonce = generateRandomString(32)

        val encodedResponseType = RESPONSE_TYPE.encodeURLParameter()
        val encodedScope = SCOPE.encodeURLParameter()
        val encodedState = state.encodeURLParameter()
        val encodedNonce = nonce.encodeURLParameter()

        val fullAuthUrl = "$AUTH_URL?" +
                "client_id=$CLIENT_ID&" +
                "redirect_uri=$REDIRECT_URI&" +
                "response_type=$encodedResponseType&" +
                "scope=$encodedScope&" +
                "state=$encodedState&" +
                "nonce=$encodedNonce"

        openUrlInBrowser(fullAuthUrl)

        val (idToken, newAccessToken) = waitForGoogleOAuthRedirect(state)
        if (idToken == null || newAccessToken == null) {
            println("GoogleTokenProvider: Failed to obtain tokens")
            return null
        }

        // Validate nonce from the id token.
        val jwt = JWT.from(idToken)
        val tokenNonce = jwt.claims["nonce"].toString().replace("\"", "")
        if (tokenNonce != nonce) {
            println("GoogleTokenProvider: Nonce mismatch. Expected: $nonce, got: $tokenNonce")
            return null
        }

        // Extract an account identifier from the token (e.g., email).
        val email = jwt.claims["email"]?.toString()?.replace("\"", "")
        if (email == null) {
            println("GoogleTokenProvider: Email not found in token")
            return null
        }

        return AccountStorage.saveAccount(ProviderAccount(this, email, newAccessToken))
    }

    @OptIn(ExperimentalEncodingApi::class)
    @Throws(RandomnessProcurementException::class)
    private fun generateRandomString(length: Int): String {
        val randomBytes = ByteArray(length)
        CryptoRand.Default.nextBytes(randomBytes)
        return Base64.encode(randomBytes)
            .replace("+", "-")
            .replace("/", "_")
            .replace("=", "")
            .take(length)
    }
}