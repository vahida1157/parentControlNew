package com.vahak.mehrban.domain.error

// 🚀 Pure Logical Network Errors
enum class AuthError {
    NETWORK_UNAVAILABLE,
    SERVER_REJECTION,
    WRONG_VERIFICATION_CODE,
    UNKNOWN
}

class AuthException(val error: AuthError, val serverMessage: String? = null) : Exception()