package com.vahak.mehrban.uiv2.screens.browser

import com.vahak.mehrban.core.data.local.entity.*

object MockBrowserData {
    val mockProfile = FullBrowserProfile(
        settings = BrowserSettingsEntity(
            childId = "1", searchEngine = "kiddle", filterMode = FilterMode.WHITELIST_ONLY
        ),
        allowedSites = listOf(
            BrowserAllowedSiteEntity(childId = "1", url = "aparat.com", label = "آپارات")
        ),
        blockedSites = listOf(BrowserBlockedSiteEntity(childId = "1", url = "badsite.com")),
        blockedKeywords = listOf(BrowserBlockedKeywordEntity(childId = "1", keyword = "ترسناک"))
    )

    val mockHistory = listOf(
        BrowserHistoryEntity(id = 1, childId = "1", url = "https://aparat.com", title = "آپارات", timestamp = System.currentTimeMillis()),
        BrowserHistoryEntity(id = 2, childId = "1", url = "https://kiddle.co", title = "Kiddle Search", timestamp = System.currentTimeMillis() - 3600000)
    )
}