package com.vahak.mehrban.uiv2.screens.browser.pages

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.vahak.mehrban.R
import com.vahak.mehrban.presentation.browser.settings.BrowserSettingsEvent
import com.vahak.mehrban.presentation.browser.settings.BrowserSettingsState
import com.vahak.mehrban.uiv2.screens.browser.MockBrowserData
import com.vahak.mehrban.uiv2.theme.ParentControlTheme

@Composable
fun BlockedSitesPage(state: BrowserSettingsState, onEvent: (BrowserSettingsEvent) -> Unit) {
    GenericListEditorPage(
        items = state.draftBlockedSites,
        itemTitle = { it.url },
        itemInput1 = { it.url },
        onDelete = { onEvent(BrowserSettingsEvent.RemoveBlockedSite(it.url)) },
        onEdit = { oldItem, newUrl, _ -> onEvent(BrowserSettingsEvent.EditBlockedSite(oldItem.url, newUrl)) },
        dialogTitle = stringResource(R.string.browser_add_blocked_site_title),
        input1Hint = stringResource(R.string.browser_site_address_hint),
        input2Hint = null,
        onAdd = { url, _ -> onEvent(BrowserSettingsEvent.AddBlockedSite(url)) }
    )
}

@Preview(showBackground = true, locale = "fa")
@Composable
fun PreviewBlockedSitesPage() {
    ParentControlTheme {
        BlockedSitesPage(state = BrowserSettingsState(draftBlockedSites = MockBrowserData.mockProfile.blockedSites), onEvent = {})
    }
}