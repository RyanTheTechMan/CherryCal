package com.ryanthetechman.cherrycal

import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin

actual fun HttpClient(): HttpClient = HttpClient(Darwin)