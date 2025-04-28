package com.ryanthetechman.cherrycal

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform