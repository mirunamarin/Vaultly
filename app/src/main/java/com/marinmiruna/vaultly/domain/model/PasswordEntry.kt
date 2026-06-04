package com.marinmiruna.vaultly.domain.model

data class PasswordEntry(
    val id: Long = 0,
    val service: String,
    val username: String,
    val password: String,
    val url: String,
    val note: String,
    val createdAt: Long,
    val updatedAt: Long
)