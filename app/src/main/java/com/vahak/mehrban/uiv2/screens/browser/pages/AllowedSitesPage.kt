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
fun AllowedSitesPage(state: BrowserSettingsState, onEvent: (BrowserSettingsEvent) -> Unit) {
    GenericListEditorPage(
        items = state.draftAllowedSites,
        itemTitle = { it.label },
        itemSubtitle = { it.url },
        itemInput1 = { it.url },
        itemInput2 = { it.label },
        onDelete = { onEvent(BrowserSettingsEvent.RemoveAllowedSite(it.url)) },
        onEdit = { oldItem, newUrl, newLabel -> onEvent(BrowserSettingsEvent.EditAllowedSite(oldItem.url, newUrl, newLabel ?: "")) },
        dialogTitle = stringResource(R.string.browser_add_site_title),
        input1Hint = stringResource(R.string.browser_site_url_hint),
        input2Hint = stringResource(R.string.browser_site_name_hint),
        onAdd = { url, label -> onEvent(BrowserSettingsEvent.AddAllowedSite(url, label ?: "")) }
    )
}

@Preview(showBackground = true, locale = "fa")
@Composable
fun PreviewAllowedSitesPage() {
    ParentControlTheme {
        AllowedSitesPage(state = BrowserSettingsState(draftAllowedSites = MockBrowserData.mockProfile.allowedSites), onEvent = {})
    }
}