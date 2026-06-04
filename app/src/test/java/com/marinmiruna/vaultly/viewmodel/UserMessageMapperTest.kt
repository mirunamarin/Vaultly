package com.marinmiruna.vaultly.viewmodel

import org.junit.Assert.assertEquals
import org.junit.Test

class UserMessageMapperTest {

    @Test
    fun fileOperationMessage_returnsSecurityExceptionMessageWhenAvailable() {
        val message = UserMessageMapper.fileOperationMessage(
            SecurityException("Acces refuzat.")
        )

        assertEquals("Acces refuzat.", message)
    }

    @Test
    fun fileOperationMessage_returnsIllegalArgumentExceptionMessageWhenAvailable() {
        val message = UserMessageMapper.fileOperationMessage(
            IllegalArgumentException("Fișier invalid.")
        )

        assertEquals("Fișier invalid.", message)
    }

    @Test
    fun fileOperationMessage_returnsDefaultMessageForUnknownException() {
        val message = UserMessageMapper.fileOperationMessage(
            RuntimeException()
        )

        assertEquals("Operația cu fișierul nu a putut fi finalizată.", message)
    }

    @Test
    fun photoOperationMessage_returnsDefaultMessageForUnknownException() {
        val message = UserMessageMapper.photoOperationMessage(
            RuntimeException()
        )

        assertEquals("Operația cu fotografia nu a putut fi finalizată.", message)
    }

    @Test
    fun noteOperationMessage_returnsDefaultMessageForUnknownException() {
        val message = UserMessageMapper.noteOperationMessage(
            RuntimeException()
        )

        assertEquals("Operația cu notița nu a putut fi finalizată.", message)
    }

    @Test
    fun passwordOperationMessage_returnsDefaultMessageForUnknownException() {
        val message = UserMessageMapper.passwordOperationMessage(
            RuntimeException()
        )

        assertEquals("Operația nu a putut fi finalizată.", message)
    }
}