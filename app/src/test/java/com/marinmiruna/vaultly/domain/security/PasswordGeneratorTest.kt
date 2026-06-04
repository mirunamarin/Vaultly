package com.marinmiruna.vaultly.domain.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PasswordGeneratorTest {

    @Test
    fun generate_clampsLengthToMinimum() {
        val password = PasswordGenerator.generate(
            length = 3,
            includeUppercase = false,
            includeDigits = false,
            includeSymbols = false
        )

        assertEquals(PasswordGenerator.MIN_PASSWORD_LENGTH, password.length)
    }

    @Test
    fun generate_clampsLengthToMaximum() {
        val password = PasswordGenerator.generate(
            length = 100,
            includeUppercase = true,
            includeDigits = true,
            includeSymbols = true
        )

        assertEquals(PasswordGenerator.MAX_PASSWORD_LENGTH, password.length)
    }

    @Test
    fun generate_usesOnlyLowercaseWhenAllOptionsAreDisabled() {
        val password = PasswordGenerator.generate(
            length = 24,
            includeUppercase = false,
            includeDigits = false,
            includeSymbols = false
        )

        assertTrue(password.all { character ->
            character in 'a'..'z'
        })
    }

    @Test
    fun generate_usesOnlyAllowedCharacters() {
        val allowedCharacters =
            "abcdefghijklmnopqrstuvwxyz" +
                    "ABCDEFGHIJKLMNOPQRSTUVWXYZ" +
                    "0123456789" +
                    "!@#$%^&*()-_=+[]{};:,.?/"

        val password = PasswordGenerator.generate(
            length = 32,
            includeUppercase = true,
            includeDigits = true,
            includeSymbols = true
        )

        assertTrue(password.all { character ->
            character in allowedCharacters
        })
    }
}