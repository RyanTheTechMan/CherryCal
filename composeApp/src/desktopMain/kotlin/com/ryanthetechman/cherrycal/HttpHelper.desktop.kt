package com.ryanthetechman.cherrycal

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO

actual fun HttpClient(): HttpClient = HttpClient(CIO)