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
fun KeywordsPage(state: BrowserSettingsState, onEvent: (BrowserSettingsEvent) -> Unit) {
    GenericListEditorPage(
        items = state.draftKeywords,
        itemTitle = { it.keyword },
        itemInput1 = { it.keyword },
        onDelete = { onEvent(BrowserSettingsEvent.RemoveBlockedKeyword(it.keyword)) },
        onEdit = { oldItem, newKw, _ ->
            onEvent(
                BrowserSettingsEvent.EditBlockedKeyword(
                    oldItem.keyword, newKw
                )
            )
        },
        dialogTitle = stringResource(R.string.browser_add_keyword_title),
        input1Hint = stringResource(R.string.browser_keyword_hint),
        input2Hint = null,
        onAdd = { keyword, _ -> onEvent(BrowserSettingsEvent.AddBlockedKeyword(keyword)) })
}

@Preview(showBackground = true, locale = "fa")
@Composable
fun PreviewKeywordsPage() {
    ParentControlTheme {
        KeywordsPage(
            state = BrowserSettingsState(draftKeywords = MockBrowserData.mockProfile.blockedKeywords),
            onEvent = {})
    }
}