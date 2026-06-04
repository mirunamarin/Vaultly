package com.marinmiruna.vaultly.domain.security

import java.security.SecureRandom

object PasswordGenerator {

    fun generate(
        length: Int,
        includeUppercase: Boolean,
        includeDigits: Boolean,
        includeSymbols: Boolean
    ): String {
        val normalizedLength = length.coerceIn(MIN_PASSWORD_LENGTH, MAX_PASSWORD_LENGTH)
        val characterPool = buildString {
            append(LOWERCASE)
            if (includeUppercase) append(UPPERCASE)
            if (includeDigits) append(DIGITS)
            if (includeSymbols) append(SYMBOLS)
        }

        return buildString {
            repeat(normalizedLength) {
                append(characterPool[secureRandom.nextInt(characterPool.length)])
            }
        }
    }

    const val MIN_PASSWORD_LENGTH = 8
    const val MAX_PASSWORD_LENGTH = 32

    private const val LOWERCASE = "abcdefghijklmnopqrstuvwxyz"
    private const val UPPERCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    private const val DIGITS = "0123456789"
    private const val SYMBOLS = "!@#$%^&*()-_=+[]{};:,.?/"

    private val secureRandom = SecureRandom()
}