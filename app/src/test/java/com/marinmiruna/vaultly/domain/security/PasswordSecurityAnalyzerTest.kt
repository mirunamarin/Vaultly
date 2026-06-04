package com.marinmiruna.vaultly.domain.security

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PasswordSecurityAnalyzerTest {

    @Test
    fun analyzePassword_marksShortPasswordAsTooShort() {
        val issues = PasswordSecurityAnalyzer.analyzePassword(
            password = "Aa1!"
        )

        assertTrue(PasswordSecurityIssue.TOO_SHORT in issues)
    }

    @Test
    fun analyzePassword_marksPasswordWithoutUppercase() {
        val issues = PasswordSecurityAnalyzer.analyzePassword(
            password = "parola123456!"
        )

        assertTrue(PasswordSecurityIssue.NO_UPPERCASE in issues)
    }

    @Test
    fun analyzePassword_marksPasswordWithoutDigit() {
        val issues = PasswordSecurityAnalyzer.analyzePassword(
            password = "ParolaPuternica!"
        )

        assertTrue(PasswordSecurityIssue.NO_DIGIT in issues)
    }

    @Test
    fun analyzePassword_marksPasswordWithoutSymbol() {
        val issues = PasswordSecurityAnalyzer.analyzePassword(
            password = "ParolaPuternica123"
        )

        assertTrue(PasswordSecurityIssue.NO_SYMBOL in issues)
    }

    @Test
    fun analyzePassword_marksCommonPassword() {
        val issues = PasswordSecurityAnalyzer.analyzePassword(
            password = "password"
        )

        assertTrue(PasswordSecurityIssue.COMMON_PASSWORD in issues)
    }

    @Test
    fun analyzePassword_returnsNoIssuesForStrongPassword() {
        val issues = PasswordSecurityAnalyzer.analyzePassword(
            password = "ParolaPuternica123!"
        )

        assertTrue(issues.isEmpty())
    }

    @Test
    fun analyzePassword_marksReusedPasswordWhenRequested() {
        val issues = PasswordSecurityAnalyzer.analyzePassword(
            password = "ParolaPuternica123!",
            isReused = true
        )

        assertTrue(PasswordSecurityIssue.REUSED in issues)
    }

    @Test
    fun analyzePasswords_marksDuplicatePasswordsAsReused() {
        val reports = PasswordSecurityAnalyzer.analyzePasswords(
            listOf(
                PasswordSecurityInput(
                    id = 1L,
                    password = "ParolaDuplicata123!"
                ),
                PasswordSecurityInput(
                    id = 2L,
                    password = "ParolaDuplicata123!"
                ),
                PasswordSecurityInput(
                    id = 3L,
                    password = "AltaParolaPuternica123!"
                )
            )
        )

        assertTrue(reports.getValue(1L).isReused)
        assertTrue(reports.getValue(2L).isReused)
        assertFalse(reports.getValue(3L).isReused)
    }

    @Test
    fun passwordSecurityReport_isWeakIgnoresReusedOnlyIssue() {
        val report = PasswordSecurityReport(
            passwordId = 1L,
            issues = setOf(PasswordSecurityIssue.REUSED)
        )

        assertFalse(report.isWeak)
        assertTrue(report.isReused)
    }
}