package com.marinmiruna.vaultly.domain.model

data class PasswordListItem(
    val id: Long = 0,
    val service: String,
    val username: String,
    val url: String,
    val updatedAt: Long
)