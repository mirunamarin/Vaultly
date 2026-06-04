package com.marinmiruna.vaultly.domain.security

object PasswordSecurityAnalyzer {

    const val MIN_RECOMMENDED_PASSWORD_LENGTH = 12

    fun analyzePassword(
        password: String,
        isReused: Boolean = false
    ): Set<PasswordSecurityIssue> {
        val issues = mutableSetOf<PasswordSecurityIssue>()

        if (password.length < MIN_RECOMMENDED_PASSWORD_LENGTH) {
            issues += PasswordSecurityIssue.TOO_SHORT
        }

        if (password.none { it.isLowerCase() }) {
            issues += PasswordSecurityIssue.NO_LOWERCASE
        }

        if (password.none { it.isUpperCase() }) {
            issues += PasswordSecurityIssue.NO_UPPERCASE
        }

        if (password.none { it.isDigit() }) {
            issues += PasswordSecurityIssue.NO_DIGIT
        }

        if (password.none { !it.isLetterOrDigit() }) {
            issues += PasswordSecurityIssue.NO_SYMBOL
        }

        if (password.lowercase() in COMMON_PASSWORDS) {
            issues += PasswordSecurityIssue.COMMON_PASSWORD
        }

        if (isReused) {
            issues += PasswordSecurityIssue.REUSED
        }

        return issues
    }

    fun analyzePasswords(
        passwords: List<PasswordSecurityInput>
    ): Map<Long, PasswordSecurityReport> {
        val reusedPasswords = passwords
            .groupBy { it.password }
            .filterValues { entries -> entries.size > 1 }
            .keys

        return passwords.associate { input ->
            val issues = analyzePassword(
                password = input.password,
                isReused = input.password in reusedPasswords
            )

            input.id to PasswordSecurityReport(
                passwordId = input.id,
                issues = issues
            )
        }
    }

    private val COMMON_PASSWORDS = setOf(
        "password",
        "password1",
        "123456",
        "1234567",
        "12345678",
        "123456789",
        "1234567890",
        "qwerty",
        "qwerty123",
        "admin",
        "admin123",
        "letmein",
        "welcome",
        "welcome123",
        "iloveyou",
        "111111",
        "000000"
    )
}

data class PasswordSecurityInput(
    val id: Long,
    val password: String
)

data class PasswordSecurityReport(
    val passwordId: Long,
    val issues: Set<PasswordSecurityIssue>
) {
    val isWeak: Boolean
        get() = issues.any { issue -> issue != PasswordSecurityIssue.REUSED }

    val isReused: Boolean
        get() = PasswordSecurityIssue.REUSED in issues

    val isSafe: Boolean
        get() = issues.isEmpty()
}

enum class PasswordSecurityIssue {
    TOO_SHORT,
    NO_LOWERCASE,
    NO_UPPERCASE,
    NO_DIGIT,
    NO_SYMBOL,
    COMMON_PASSWORD,
    REUSED
}