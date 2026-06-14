package com.vahak.mehrban.domain.usecase

import android.content.Context
import com.vahak.mehrban.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class VerifyOtpUseCase @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun execute(code: String): OtpValidationResult {
        if (code.length != 4) return OtpValidationResult.Error(context.getString(R.string.error_otp_length))
        if (code != "1234") return OtpValidationResult.Error(context.getString(R.string.error_otp_wrong))
        return OtpValidationResult.Success(120)
    }
}