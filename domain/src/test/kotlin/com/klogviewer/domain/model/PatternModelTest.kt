package com.klogviewer.domain.model

import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isTrue

class PatternModelTest {

    @Test
    fun `given draft history when push new draft then can undo`() {
        val draft1 = PatternDraft(name = "Initial")
        val draft2 = PatternDraft(name = "Edited")
        val history = PatternDraftHistory(current = draft1)

        val updatedHistory = history.push(draft2)

        expectThat(updatedHistory.canUndo).isTrue()
        expectThat(updatedHistory.canRedo).isFalse()
        expectThat(updatedHistory.current.name).isEqualTo("Edited")

        val undoneHistory = updatedHistory.undo()
        expectThat(undoneHistory.current.name).isEqualTo("Initial")
        expectThat(undoneHistory.canRedo).isTrue()

        val redoneHistory = undoneHistory.redo()
        expectThat(redoneHistory.current.name).isEqualTo("Edited")
    }

    @Test
    fun `given pattern token roles when effective name checked then resolves correctly`() {
        val tokenCustom = PatternToken(role = PatternTokenRole.CUSTOM_PROPERTY, customPropertyName = "transactionId")
        val tokenLevel = PatternToken(role = PatternTokenRole.LEVEL)

        expectThat(tokenCustom.effectiveName).isEqualTo("transactionId")
        expectThat(tokenLevel.effectiveName).isEqualTo("level")
    }
}
