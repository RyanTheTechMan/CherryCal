package com.ryanthetechman.cherrycal.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.ryanthetechman.cherrycal.ai.AISecrets
import com.ryanthetechman.cherrycal.ai.CalendarEventSchema
import com.ryanthetechman.cherrycal.ai.PromptAI
import com.ryanthetechman.cherrycal.calendar_interaction.Contact
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.time.Duration.Companion.hours

/**
 * Simple “new-event” dialog: title + start/end time fields (HH:mm) and a Create button.
 * The parent decides what to do with the newly-created event.
 */
@Composable
fun EventEditorDialog(
    timeZone: TimeZone,
    initialDate: LocalDate,
    initialHour: Int,
    initialMinute: Int,
    onDismiss: () -> Unit,
    onCreateEvent: (title: String, location: String?, description: String?, attendees: List<Contact>, start: Instant, end: Instant) -> Unit
) {
    var title        by remember { mutableStateOf(TextFieldValue("")) }
    var startText    by remember { mutableStateOf(TextFieldValue("${initialHour.toString().padStart(2, '0')}:${initialMinute.toString().padStart(2, '0')}")) }
    var endText      by remember { mutableStateOf(TextFieldValue("${(initialHour + 1).toString().padStart(2, '0')}:${initialMinute.toString().padStart(2, '0')}")) }
    var location     by remember { mutableStateOf(TextFieldValue("")) }
    var description  by remember { mutableStateOf(TextFieldValue("")) }
    var attendees    by remember { mutableStateOf<List<Contact>>(emptyList()) }

    // AI integration
    var titleLocked       by remember { mutableStateOf(false) }
    var startLocked       by remember { mutableStateOf(true) }
    var endLocked         by remember { mutableStateOf(false) }
    var locationLocked    by remember { mutableStateOf(false) }
    var attendeesLocked   by remember { mutableStateOf(false) }

    val coroutineScope       = rememberCoroutineScope()
    var debounceJob: Job?    by remember { mutableStateOf(null) }

    fun runAI(desc: String) {
        debounceJob?.cancel()
        if (desc.isBlank()) return
        debounceJob = coroutineScope.launch {
            delay(500)

//            val now = Clock.System.now().toLocalDateTime(timeZone).date
//            val (prevYear, prevMonth) = getPreviousMonth(now.year, now.monthNumber)
//            val (nextYear, nextMonth) = getNextMonth(now.year, now.monthNumber)
//            val prevMonthEvents = CalendarDataManager.getCachedMonth(prevYear, prevMonth) ?: emptyList()
//            val currentMonthEvents = CalendarDataManager.getCachedMonth(now.year, now.monthNumber) ?: emptyList()
//            val nextMonthEvents = CalendarDataManager.getCachedMonth(nextYear, nextMonth) ?: emptyList()
//            val allMonthEvents = prevMonthEvents + nextMonthEvents + currentMonthEvents
//
//            val allEventsJson = allMonthEvents.map { event ->
//                if (event.time is EventTime.Timed) {
//                    buildString {
//                        append("    {\n")
//                        append("      \"title\": \"${event.title}\",\n")
//                        append("      \"description\": \"${event.description}\",\n")
//                        append("      \"location\": \"${event.location}\",\n")
//                        append("      \"start_time\": \"${event.time.start}\",\n")
//                        append("      \"end_time\": \"${event.time.end}\",\n")
//                        append("      \"attendees\": [\n")
//                        event.attendees?.forEach { attendee ->
//                            append("        {\n")
//                            append("          \"name\": \"${attendee.name}\",\n")
//                            append("          \"email\": \"${attendee.email}\"\n")
//                            append("        }")
//                        }
//                        append("      ]\n")
//                        append("    }")
//                    }
//                }
//            }


            val systemPrompt = """
                You are CherryCal’s calendar assistant.
                Given the user’s partial event data, fill in any missing fields (title, location, start_time, end_time, attendees) 
                to produce a complete event JSON that strictly matches the schema.
                Take a guess at any missing fields that you think the user may have intended to fill.
                Try to always come up with a creative title based on the user’s input.
                Do NOT modify any fields the user has locked.
                Only output the JSON object—no extra text.
                
                The user is: ${AISecrets.selfContact.name}
                Their email is: ${AISecrets.selfContact.email}
                
                The user has the following contacts in (email, name) format:
                ${AISecrets.contacts.joinToString("\n") { "email: {${it.email}}, name: ${it.name}" }}
                
                ${AISecrets.MISC_PROMPT}
            """.trimIndent()

            val userPartial = buildString {
                append("{\n")
                append("  \"description\": \"${description.text}\",\n")
                if (titleLocked)       append("  \"title\": \"${title.text}\",\n")
                if (locationLocked)    append("  \"location\": \"${location.text}\",\n")
                if (startLocked)       append("  \"start_time\": \"${startText.text}\",\n")
                if (endLocked)         append("  \"end_time\": \"${endText.text}\",\n")
                if (attendeesLocked)   append("  \"attendees\": [${attendees.joinToString(", ") { "\"${it.email}\"" }}]\n")
                append("}")
            }

            val aiJson = PromptAI.sendStructuredRequest(
                modelId        = AISecrets.MODEL_ID,
                userPrompt     = "This is what the user has filled in so far:\n```json\n$userPartial\n```",
                systemPrompt   = systemPrompt,
                responseFormat = CalendarEventSchema.RESPONSE_FORMAT
            )

            if ("error" in aiJson) return@launch

            if (!titleLocked) {
                val newTitle = aiJson["title"]?.jsonPrimitive?.contentOrNull.orEmpty()
                title = TextFieldValue(newTitle)
            }

            if (!locationLocked) {
                val newLocation = aiJson["location"]?.jsonPrimitive?.contentOrNull.orEmpty()
                location = TextFieldValue(newLocation)
            }

            if (!startLocked) {
                val newStart = aiJson["start_time"]?.jsonPrimitive?.contentOrNull.orEmpty()
                startText = TextFieldValue(newStart)
            }

            if (!endLocked) {
                val newEnd = aiJson["end_time"]?.jsonPrimitive?.contentOrNull.orEmpty()
                endText = TextFieldValue(newEnd)
            }

            if (!titleLocked && aiJson["attendees"] != null) {
                attendees = aiJson["attendees"]!!.jsonArray.mapNotNull { elem ->
                    runCatching {
                        val obj   = elem.jsonObject
                        val contact = Contact(
                            name  = obj["name"]!!.jsonPrimitive.content,
                            email = obj["email"]!!.jsonPrimitive.content
                        )
                        if (contact.email == AISecrets.selfContact.email) return@mapNotNull null
                        contact
                    }.getOrNull()
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create event") },
        text = {
            Column (Modifier.verticalScroll(rememberScrollState())) {
                // DESCRIPTION (always locked)
                OutlinedTextField(
                    value = description,
                    onValueChange = {
                        description = it
                        runAI(it.text)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Description") },
                    maxLines = 4,
                    trailingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) }
                )
                Spacer(Modifier.height(12.dp))

                // TITLE
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        titleLocked = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Title") },
                    trailingIcon = {
                        IconButton(onClick = { titleLocked = !titleLocked }) {
                            Icon(
                                if (titleLocked) Icons.Filled.Lock else Icons.Outlined.Lock,
                                contentDescription = null
                            )
                        }
                    }
                )
                Spacer(Modifier.height(8.dp))

                // START
                OutlinedTextField(
                    value = startText,
                    onValueChange = {
                        startText = it
                        startLocked = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Start (HH:mm)") },
                    trailingIcon = {
                        IconButton(onClick = { startLocked = !startLocked }) {
                            Icon(
                                if (startLocked) Icons.Filled.Lock else Icons.Outlined.Lock,
                                contentDescription = null
                            )
                        }
                    }
                )
                Spacer(Modifier.height(8.dp))

                // END
                OutlinedTextField(
                    value = endText,
                    onValueChange = {
                        endText = it
                        endLocked = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("End (HH:mm)") },
                    trailingIcon = {
                        IconButton(onClick = { endLocked = !endLocked }) {
                            Icon(
                                if (endLocked) Icons.Filled.Lock else Icons.Outlined.Lock,
                                contentDescription = null
                            )
                        }
                    }
                )
                Spacer(Modifier.height(8.dp))

                // LOCATION
                OutlinedTextField(
                    value = location,
                    onValueChange = {
                        location = it
                        locationLocked = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Location") },
                    trailingIcon = {
                        IconButton(onClick = { locationLocked = !locationLocked }) {
                            Icon(
                                if (locationLocked) Icons.Filled.Lock else Icons.Outlined.Lock,
                                contentDescription = null
                            )
                        }
                    }
                )
                Spacer(Modifier.height(12.dp))

                // ATTENDEES header with lock toggle
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Attendees", style = MaterialTheme.typography.subtitle1)
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = { attendeesLocked = !attendeesLocked }) {
                        Icon(
                            if (attendeesLocked) Icons.Filled.Lock else Icons.Outlined.Lock,
                            contentDescription = null
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))

                Column(
                    Modifier
                        .heightIn(max = 180.dp)        // ≈ 2½ rows
                        .verticalScroll(rememberScrollState())
                ) {
                    attendees.forEachIndexed { idx, attendee ->
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = TextFieldValue(attendee.name ?: ""),
                                onValueChange = { tfv ->
                                    val list = attendees.toMutableList()
                                    list[idx] = attendee.copy(name = tfv.text)
                                    attendees = list
                                    attendeesLocked = true
                                },
                                modifier = Modifier.weight(1f),
                                label = { Text("Name") }
                            )
                            Spacer(Modifier.width(8.dp))
                            OutlinedTextField(
                                value = TextFieldValue(attendee.email),
                                onValueChange = { tfv ->
                                    val list = attendees.toMutableList()
                                    list[idx] = attendee.copy(email = tfv.text)
                                    attendees = list
                                    attendeesLocked = true
                                },
                                modifier = Modifier.weight(1f),
                                label = { Text("Email") }
                            )
                            Spacer(Modifier.width(8.dp))
                            IconButton(onClick = {
                                val list = attendees.toMutableList()
                                list.removeAt(idx)
                                attendees = list
                            }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Remove")
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                    }
                }

                /* Add-attendee button */
                Button(
                    onClick = { attendees = attendees + Contact(name = "", email = "") },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Add attendee")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    fun parseTime(tfv: TextFieldValue): Pair<Int, Int> {
                        val parts = tfv.text.trim().split(":")
                        return (parts.getOrNull(0)?.toIntOrNull() ?: 0) to
                                (parts.getOrNull(1)?.toIntOrNull() ?: 0)
                    }
                    val (sh, sm) = parseTime(startText)
                    val (eh, em) = parseTime(endText)
                    val startLocal = LocalDateTime(initialDate, LocalTime(sh, sm))
                    val endLocal   = LocalDateTime(initialDate, LocalTime(eh, em))
                    val startInst  = startLocal.toInstant(timeZone)
                    val endInst    = endLocal.toInstant(timeZone)
                        .takeIf { it > startInst } ?: startInst.plus(1.hours)

                    onCreateEvent(
                        title.text.trim(),
                        location.text.trim().ifBlank { null },
                        description.text.trim().ifBlank { null },
                        attendees,
                        startInst,
                        endInst
                    )
                    onDismiss()
                }
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}