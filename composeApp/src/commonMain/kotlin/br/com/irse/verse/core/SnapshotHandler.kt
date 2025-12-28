package br.com.irse.verse.core

interface SnapshotHandler {
    suspend fun captureAndSave(
        verses: List<Pair<VerseRequest, String?>>,
        template: VerseViewModel.SnapshotTemplate
    )
}