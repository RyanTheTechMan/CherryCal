package com.ryanthetechman.cherrycal.ai

import kotlinx.serialization.json.*

object CalendarEventSchema {
    private val rawSchema = Json.parseToJsonElement(
        """
        {
          "type": "object",
          "properties": {
            "title": {
              "type": "string",
              "description": "The title of the event."
            },
            "location": {
              "type": "string",
              "description": "The location of the event."
            },
            "attendees": {
              "type": "array",
              "items": {
                "type": "object",
                "properties": {
                  "name": {
                    "type": "string",
                    "description": "The name of the attendee."
                  },
                  "email": {
                    "type": "string",
                    "description": "The email of the attendee."
                  }
                },
                "required": [
                  "name",
                  "email"
                ],
                "additionalProperties": false
              },
              "description": "List of attendees for the event."
            },
            "start_time": {
              "type": "string",
              "description": "The start time of the event."
            },
            "end_time": {
              "type": "string",
              "description": "The end time of the event."
            }
          },
          "required": [
            "title",
            "location",
            "attendees",
            "start_time",
            "end_time"
          ],
        "additionalProperties": false
        }
    """
    ).jsonObject

    val RESPONSE_FORMAT: JsonObject = buildJsonObject {
        put("type", "json_schema")
        put("json_schema", buildJsonObject {
            put("name", "calendar_event")
            put("schema", rawSchema)
            put("strict", true)
        })
    }
}