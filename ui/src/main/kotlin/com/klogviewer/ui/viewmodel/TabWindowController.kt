package com.klogviewer.ui.viewmodel

import com.klogviewer.ui.mvi.*
import java.util.UUID

object TabWindowController {
    fun addTab(state: KLogViewerState): KLogViewerState {
        val newTabId = UUID.randomUUID().toString()
        val newWindowId = UUID.randomUUID().toString()
        val newTab = TabState(
            id = newTabId,
            title = "New Tab",
            windows = listOf(LogWindow(id = newWindowId)),
            activeWindowId = newWindowId
        )
        return state.copy(tabs = state.tabs + newTab, activeTabId = newTabId)
    }

    fun closeTab(state: KLogViewerState, id: String, onWindowClosed: (String) -> Unit): KLogViewerState {
        val tab = state.tabs.find { it.id == id }
        tab?.windows?.forEach { window ->
            onWindowClosed(window.id)
        }

        val remainingTabs = state.tabs.filter { it.id != id }
        val newTabs = if (remainingTabs.isEmpty()) {
            val newTabId = UUID.randomUUID().toString()
            val newWindowId = UUID.randomUUID().toString()
            listOf(TabState(
                id = newTabId,
                title = "Log View",
                windows = listOf(LogWindow(id = newWindowId)),
                activeWindowId = newWindowId
            ))
        } else {
            remainingTabs
        }
        val newActiveId = if (state.activeTabId == id) {
            newTabs.last().id
        } else {
            state.activeTabId
        }
        return state.copy(tabs = newTabs, activeTabId = newActiveId)
    }

    fun splitHorizontal(state: KLogViewerState): KLogViewerState {
        val newWindowId = UUID.randomUUID().toString()
        val newWindow = LogWindow(id = newWindowId)
        
        return state.updateActiveTab { tab ->
            tab.copy(
                windows = tab.windows + newWindow,
                activeWindowId = newWindowId
            )
        }
    }

    fun closeWindow(state: KLogViewerState, id: String, onWindowClosed: (String) -> Unit): KLogViewerState {
        onWindowClosed(id)
        
        return state.updateActiveTab { tab ->
            val remainingWindows = tab.windows.filter { it.id != id }
            if (remainingWindows.isEmpty()) tab // Cannot close the last window
            else {
                val newActiveId = if (tab.activeWindowId == id) remainingWindows.last().id else tab.activeWindowId
                tab.copy(windows = remainingWindows, activeWindowId = newActiveId)
            }
        }
    }

    fun switchWindow(state: KLogViewerState, id: String): KLogViewerState {
        return state.updateActiveTab { it.copy(activeWindowId = id) }
    }
}
