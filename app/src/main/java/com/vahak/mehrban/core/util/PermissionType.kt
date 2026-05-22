package com.vahak.mehrban.core.util

import android.app.admin.DevicePolicyManager
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
    DEVICE_ADMIN(
        title = "محافظت از حذف برنامه",
        description = "برای جلوگیری از پاک شدن برنامه توسط کودک، این دسترسی الزامی است.",
        instruction = listOf(
            "۱. روی اعطای دسترسی کلیک کن",
            "۲. گزینه فعال‌سازی (Activate) را در صفحه بعد انتخاب کن",
            "۳. به برنامه برگرد"
        ),
        // This is the specific OS action for Device Admin
        androidSettingsAction = DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN
    ),
    ACCESSIBILITY(
        title = "دسترسی پذیری (سپر امنیتی)",
        description = "برای مسدود کردن تنظیمات گوشی و جلوگیری از دور زدن قوانین توسط کودک.",
        instruction = listOf(
            "۱. روی اعطای دسترسی کلیک کن",
            "۲. وارد بخش برنامه‌های نصب شده (Installed apps) شو",
            "۳. نام برنامه را پیدا کن و آن را روشن کن"
        ),
        androidSettingsAction = Settings.ACTION_ACCESSIBILITY_SETTINGS
    ),
    VPN(
        title = "فیلتر هوشمند وب (VPN)",
        description = "برای مسدود کردن سایت‌های نامناسب، نیاز به ایجاد یک تونل امن (VPN محلی) داریم. هیچ داده‌ای به خارج از گوشی ارسال نمی‌شود.",
        instruction = listOf(
            "۱. روی اعطای دسترسی کلیک کن",
            "۲. در پیام هشدار اندروید، تایید (OK) را انتخاب کن",
            "۳. تمام شد!"
        ),
        androidSettingsAction = "ACTION_REQUEST_VPN" // Custom flag
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