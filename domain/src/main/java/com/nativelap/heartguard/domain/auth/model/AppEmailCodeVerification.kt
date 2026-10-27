package com.nativelap.heartguard.domain.auth.model

data class AppEmailCodeVerification(
    val verificationToken: String,
    val expiresAt: String,
) {
    override fun toString(): String = "AppEmailCodeVerification(verificationToken=***)"
}
