package com.autoledgercn

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class UiCopyTest {
    @Test
    fun mainUiDoesNotUseAllowanceCopy() {
        val file = listOf(
            File("src/main/java/com/autoledgercn/MainActivity.kt"),
            File("app/src/main/java/com/autoledgercn/MainActivity.kt")
        ).first { it.exists() }
        val text = file.readText()
        listOf(
            "生活费",
            "当前生活费周期",
            "预计下次生活费",
            "预计还会消费",
            "到账前剩余",
            "本周期生活费",
            "周期预测",
            "一级分类",
            "二级分类"
        ).forEach {
            assertFalse("Main UI should not contain $it", text.contains(it))
        }
    }

    @Test
    fun transactionRowsShowNoteLabel() {
        val file = listOf(
            File("src/main/java/com/autoledgercn/MainActivity.kt"),
            File("app/src/main/java/com/autoledgercn/MainActivity.kt")
        ).first { it.exists() }
        val text = file.readText()

        assertTrue(text.contains("备注："))
    }

    @Test
    fun accountFieldUsesDropdownCopy() {
        val file = listOf(
            File("src/main/java/com/autoledgercn/MainActivity.kt"),
            File("app/src/main/java/com/autoledgercn/MainActivity.kt")
        ).first { it.exists() }
        val text = file.readText()

        assertFalse(text.contains("账户快捷选择"))
        assertTrue(text.contains("新增账户选项"))
    }
}
