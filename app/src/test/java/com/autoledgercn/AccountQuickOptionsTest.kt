package com.autoledgercn

import com.autoledgercn.domain.model.AccountQuickOptions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AccountQuickOptionsTest {
    @Test
    fun defaultAccountOptionsContainExpectedValuesInOrder() {
        assertEquals(listOf("微信", "支付宝", "银行卡", "现金", "其他"), AccountQuickOptions.defaults)
    }

    @Test
    fun mergedOptionsContainAddedAccount() {
        val options = AccountQuickOptions.merged(listOf("校园卡"))

        assertTrue(options.contains("校园卡"))
    }

    @Test
    fun repeatedAccountOptionDoesNotDuplicate() {
        val options = AccountQuickOptions.addOption(listOf("校园卡"), "校园卡")

        assertEquals(1, options.count { it == "校园卡" })
    }

    @Test
    fun blankAccountOptionIsNotAdded() {
        val options = AccountQuickOptions.addOption(listOf("校园卡"), "   ")

        assertFalse(options.contains(""))
        assertEquals(listOf("校园卡"), options)
    }
}
