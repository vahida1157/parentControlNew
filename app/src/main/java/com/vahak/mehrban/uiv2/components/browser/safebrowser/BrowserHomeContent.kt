package com.vahak.mehrban.uiv2.components.browser.safebrowser

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vahak.mehrban.R
import com.vahak.mehrban.core.data.local.entity.BrowserAllowedSiteEntity
import com.vahak.mehrban.presentation.browser.BrowserEvent
import com.vahak.mehrban.uiv2.theme.AppIcons
import com.vahak.mehrban.uiv2.theme.LocalCustomColors
import com.vahak.mehrban.uiv2.theme.ParentControlTheme

@Composable
fun BrowserHomeContent(
    isCartoonWorldEnabled: Boolean,
    allowedSites: List<BrowserAllowedSiteEntity>,
    onEvent: (BrowserEvent) -> Unit
) {
    val colors = LocalCustomColors.current

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        if (isCartoonWorldEnabled) {
            Box(
                modifier = Modifier
                    .clickable {
                        onEvent(BrowserEvent.InputChanged("telewebion.ir/kids"))
                        onEvent(BrowserEvent.SubmitSearch)
                    }.padding(horizontal = 24.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🎬", fontSize = 48.sp)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            stringResource(R.string.browser_cartoon_world),
                            color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black
                        )
                        Text(
                            stringResource(R.string.browser_cartoon_world_desc),
                            color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.weight(1f).height(1.dp).background(colors.divider))
            Text(
                text = stringResource(R.string.browser_allowed_sites),
                fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colors.textSecondary,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
            Box(modifier = Modifier.weight(1f).height(1.dp).background(colors.divider))
        }
        Spacer(modifier = Modifier.height(16.dp))

        if (allowedSites.isEmpty()) {
            Text(stringResource(R.string.browser_empty_list_hint), color = colors.textHint, modifier = Modifier.padding(top = 16.dp))
        } else {
            LazyColumn(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(allowedSites) { site ->
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable {
                            onEvent(BrowserEvent.InputChanged(site.url))
                            onEvent(BrowserEvent.SubmitSearch)
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = colors.surface)
                    ) {
                        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(40.dp).background(colors.cardInnerBG, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) { Text("🌍", fontSize = 20.sp) }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(site.label, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                                Text(site.url, fontSize = 12.sp, color = colors.textSecondary)
                            }
                            Icon(AppIcons.ChevronLeft, contentDescription = null, tint = colors.textHint)
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, locale = "fa")
@Composable
fun PreviewBrowserHomeContent() {
    ParentControlTheme {
        BrowserHomeContent(
            isCartoonWorldEnabled = true,
            allowedSites = listOf(
                BrowserAllowedSiteEntity(childId = "1", url = "aparat.com", label = "آپارات"),
                BrowserAllowedSiteEntity(childId = "1", url = "telewebion.ir", label = "تلوبیون")
            ),
            onEvent = {}
        )
    }
}