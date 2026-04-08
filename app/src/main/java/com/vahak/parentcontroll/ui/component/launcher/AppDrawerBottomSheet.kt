package com.vahak.parentcontroll.ui.component.launcher

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toDrawable
import com.vahak.parentcontroll.core.util.AppInfo
import com.vahak.parentcontroll.ui.theme.AppIcons
import com.vahak.parentcontroll.ui.theme.LocalCustomColors
import com.vahak.parentcontroll.ui.theme.ParentControlTheme
import android.graphics.Color as AndroidColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDrawerBottomSheet(
    apps: List<AppInfo>,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onAppClick: (String) -> Unit
) {
    val colors = LocalCustomColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.background,
        // Match the HTML prototype's top rounded corners
        shape = RoundedCornerShape(topStart = 35.dp, topEnd = 35.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize() // Takes up available height for the grid
                .padding(horizontal = 20.dp)
                .padding(bottom = 30.dp)
        ) {
            // --- HEADER ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "برنامه‌های مجاز شما",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = colors.textPrimary
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        painter = AppIcons.Close,
                        contentDescription = "Close",
                        tint = colors.textSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // --- CONTENT ---
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = colors.primary)
                }
            } else if (apps.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("هیچ برنامه‌ای یافت نشد.", color = colors.textSecondary)
                }
            } else {
                // --- HIGH PERFORMANCE GRID ---
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 80.dp), // Auto-fits items beautifully
                    horizontalArrangement = Arrangement.spacedBy(15.dp),
                    verticalArrangement = Arrangement.spacedBy(25.dp)
                ) {
                    items(
                        items = apps,
                        // PRO TIP: By providing a unique key (packageName), Compose knows 
                        // exactly which items changed. This stops lag when scrolling!
                        key = { appInfo -> appInfo.packageName }
                    ) { appInfo ->
                        LauncherRealAppItem(
                            appInfo = appInfo,
                            onClick = onAppClick
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, locale = "fa")
@Composable
fun AppDrawerBottomSheetPreview() {
    ParentControlTheme {
        val dummyDrawable = AndroidColor.BLUE.toDrawable()
        val dummyApps = listOf(
            AppInfo("بازی ماشین", "com.game1", dummyDrawable),
            AppInfo("نقاشی", "com.game2", dummyDrawable),
            AppInfo("شاد", "com.edu", dummyDrawable)
        )

        // Previews with BottomSheets can sometimes overlap, so we preview the content state
        AppDrawerBottomSheet(
            apps = dummyApps,
            isLoading = false,
            onDismiss = {},
            onAppClick = {}
        )
    }
}