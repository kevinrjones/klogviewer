package com.klogviewer.ui.viewmodel

import com.klogviewer.core.parser.HeuristicProbe
import com.klogviewer.core.parser.JsonLogParser
import com.klogviewer.core.parser.ProbeResult
import com.klogviewer.domain.repository.LocalFileSystem
import com.klogviewer.domain.repository.LogSource
import com.klogviewer.domain.repository.LogSourceFactory
import com.klogviewer.domain.repository.RemoteFileSystem
import com.klogviewer.ui.mvi.KLogViewerState
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

class LogLoadingCoordinatorColumnMergeTest {

    private val coordinator = LogLoadingCoordinator(
        localFileSystem = mockk<LocalFileSystem>(relaxed = true),
        remoteFileSystem = mockk<RemoteFileSystem>(relaxed = true),
        logSource = mockk<LogSource>(relaxed = true),
        heuristicProbe = mockk<HeuristicProbe>(relaxed = true),
        logSourceFactory = mockk<LogSourceFactory>(relaxed = true),
        scope = CoroutineScope(Dispatchers.Unconfined),
        state = MutableStateFlow(KLogViewerState()),
        onSavePreferences = {},
        onHandleLogUpdate = { _, _, _ -> },
        onShowError = {}
    )

    @Test
    fun `given discovered columns when merged then canonical defaults remain stable`() {
        val discovered = (1..12).map { "custom_$it" }
        val results = listOf(
            ProbeResult(
                parser = JsonLogParser(),
                parserName = "JSON",
                columns = listOf("Timestamp", "Level", "Content") + discovered
            )
        )

        val merged = coordinator.mergeColumnsWithDiscovered(
            persistedColumns = emptyList(),
            results = results
        )

        expectThat(merged).isEqualTo(
            listOf("Timestamp", "Level", "Content") + discovered.take(8)
        )
    }

    @Test
    fun `given persisted custom columns when merged then columns are preserved`() {
        val results = listOf(
            ProbeResult(
                parser = JsonLogParser(),
                parserName = "JSON",
                columns = listOf("Timestamp", "Level", "Content", "service", "tenant", "requestId")
            )
        )

        val merged = coordinator.mergeColumnsWithDiscovered(
            persistedColumns = listOf("Timestamp", "Level", "Content", "traceId", "service"),
            results = results
        )

        expectThat(merged).isEqualTo(
            listOf("Timestamp", "Level", "Content", "traceId", "service", "tenant", "requestId")
        )
    }

    @Test
    fun `given parser fallback when merged then canonical defaults are retained`() {
        val merged = coordinator.mergeColumnsWithDiscovered(
            persistedColumns = listOf("traceId"),
            results = listOf(null)
        )

        expectThat(merged).isEqualTo(
            listOf("Timestamp", "Level", "Content", "traceId")
        )
    }

    @Test
    fun `given parser reports message alias when merged then duplicate content column is avoided`() {
        val results = listOf(
            ProbeResult(
                parser = JsonLogParser(),
                parserName = "Simple",
                columns = listOf("Timestamp", "Level", "Message")
            )
        )

        val merged = coordinator.mergeColumnsWithDiscovered(
            persistedColumns = emptyList(),
            results = results
        )

        expectThat(merged).isEqualTo(
            listOf("Timestamp", "Level", "Content")
        )
    }
}
