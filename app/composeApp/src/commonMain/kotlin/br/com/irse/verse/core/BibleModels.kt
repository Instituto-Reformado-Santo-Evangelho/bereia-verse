package br.com.irse.verse.core

import kotlinx.serialization.Serializable

@Serializable
data class BookMetaData(
    val start: Int,
    val chapters: List<Int>
)

data class Book(
    val name: String,
    val metaData: BookMetaData
)

data class VerseRequest(
    val id: Int,
    val book: String,
    val chapter: Int,
    val verse: Int
)

data class VerseRange(
    val min: Int,
    val max: Int
)
