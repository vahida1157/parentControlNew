package com.vahak.mehrban.domain.usecase

import android.content.Context
import com.vahak.mehrban.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class ValidatePhoneUseCase @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun execute(phone: String): PhoneValidationResult {
        if (phone.isBlank()) return PhoneValidationResult.Error(context.getString(R.string.error_phone_empty))
        if (phone.length != 10 || !phone.startsWith("9")) {
            return PhoneValidationResult.Error(context.getString(R.string.error_phone_invalid_format))
        }
        return PhoneValidationResult.Success
    }
}