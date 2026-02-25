package com.vahak.parentcontroll.core.util

import android.provider.Settings

enum class PermissionType(
    val title: String,
    val description: String,
    val instruction: List<String>,
    val androidSettingsAction: String
) {
    USAGE_STATS(
        title = "آمار کارکرد برنامه‌ها",
        description = "برای مدیریت زمان استفاده از گوشی، این دسترسی حیاتی است.",
        instruction = listOf(
            "۱. روی اعطای دسترسی کلیک کن",
            "۲. نام برنامه را پیدا و فعال کن",
            "۳. تنظیمات تکمیل شد"
        ),
        androidSettingsAction = Settings.ACTION_USAGE_ACCESS_SETTINGS
    ),
    OVERLAY(
        title = "نمایش روی سایر برنامه‌ها",
        description = "برای مسدود کردن برنامه‌ها هنگام اتمام زمان، این دسترسی لازم است.",
        instruction = listOf(
            "۱. روی اعطای دسترسی کلیک کن",
            "۲. گزینه Allow display over other apps را فعال کن",
            "۳. به برنامه برگرد"
        ),
        androidSettingsAction = Settings.ACTION_MANAGE_OVERLAY_PERMISSION
    ),
    LOCATION(
        title = "موقعیت مکانی",
        description = "برای تامین امنیت و ردیابی دقیق، به این دسترسی نیاز داریم.",
        instruction = listOf(
            "۱. روی اعطای دسترسی کلیک کن",
            "۲. گزینه Allow all the time رو انتخاب کن",
            "۳. دسترسی تایید شد"
        ),
        androidSettingsAction = Settings.ACTION_LOCATION_SOURCE_SETTINGS
    )
}