package com.vahak.mehrban.domain.usecase

import androidx.annotation.StringRes
import com.vahak.mehrban.R
import javax.inject.Inject

sealed interface BrowserUrlValidationResult {
    object Success : BrowserUrlValidationResult
    data class Error(@StringRes val messageRes: Int) : BrowserUrlValidationResult
}

class ValidateBrowserUrlUseCase @Inject constructor() {

    fun execute(input: String): BrowserUrlValidationResult {
        val cleaned = clean(input)

        if (cleaned.isBlank()) {
            return BrowserUrlValidationResult.Error(R.string.error_invalid_url)
        }

        val domainRegex = "^([a-z0-9]+(-[a-z0-9]+)*\\.)+[a-z]{2,}(/.*)?$".toRegex()

        return if (domainRegex.matches(cleaned)) {
            BrowserUrlValidationResult.Success
        } else {
            BrowserUrlValidationResult.Error(R.string.error_invalid_url)
        }
    }

    fun clean(input: String): String {
        return input.trim().lowercase()
            .removePrefix("http://")
            .removePrefix("https://")
            .removePrefix("www.")
    }
}