package com.klogviewer.ui.viewmodel

import com.klogviewer.domain.model.StructuredValue
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

class StructuredInspectorFilterFormatterTest {

    @Test
    fun `given path when creating field predicate then has syntax is emitted`() {
        val predicate = StructuredInspectorFilterFormatter.fieldPredicate("user.id")

        expectThat(predicate).isEqualTo("field:user.id exists")
    }

    @Test
    fun `given string value with quotes when creating value predicate then literal is escaped`() {
        val predicate = StructuredInspectorFilterFormatter.valuePredicate(
            path = "user.name",
            value = StructuredValue.StringValue("A\"B")
        )

        expectThat(predicate).isEqualTo("field:user.name=\"A\\\"B\"")
    }

    @Test
    fun `given scalar types when creating value predicate then canonical literals are emitted`() {
        expectThat(
            StructuredInspectorFilterFormatter.valuePredicate("user.active", StructuredValue.BooleanValue(true))
        ).isEqualTo("field:user.active=true")

        expectThat(
            StructuredInspectorFilterFormatter.valuePredicate("user.id", StructuredValue.NumberValue("42"))
        ).isEqualTo("field:user.id=42")

        expectThat(
            StructuredInspectorFilterFormatter.valuePredicate("user.deletedAt", StructuredValue.NullValue)
        ).isEqualTo("field:user.deletedAt=null")
    }

    @Test
    fun `given blank path when creating field predicate then empty query is emitted`() {
        val predicate = StructuredInspectorFilterFormatter.fieldPredicate("   ")

        expectThat(predicate).isEqualTo("")
    }

    @Test
    fun `given blank path when creating value predicate then empty query is emitted`() {
        val predicate = StructuredInspectorFilterFormatter.valuePredicate(
            path = "",
            value = StructuredValue.StringValue("value")
        )

        expectThat(predicate).isEqualTo("")
    }
}