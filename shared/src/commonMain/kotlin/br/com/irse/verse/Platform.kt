package br.com.irse.verse

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform