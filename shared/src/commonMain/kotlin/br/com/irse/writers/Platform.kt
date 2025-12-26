package br.com.irse.writers

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform