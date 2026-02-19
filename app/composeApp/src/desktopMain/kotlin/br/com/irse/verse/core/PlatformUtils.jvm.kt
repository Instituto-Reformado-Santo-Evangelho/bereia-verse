package br.com.irse.verse.core

import java.util.UUID

actual fun generateUuid(): String = UUID.randomUUID().toString()
actual fun currentTimeMillis(): Long = System.currentTimeMillis()
