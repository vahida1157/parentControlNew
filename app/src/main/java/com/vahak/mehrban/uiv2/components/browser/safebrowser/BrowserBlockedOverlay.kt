package com.vahak.mehrban.uiv2.components.browser.safebrowser

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vahak.mehrban.R
import com.vahak.mehrban.uiv2.theme.AppIcons
import com.vahak.mehrban.uiv2.theme.LocalCustomColors

@Composable
fun BrowserBlockedOverlay(
    blockedUrl: String,
    onGoBackClick: () -> Unit
) {
    val colors = LocalCustomColors.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = AppIcons.Warning, // Or a shield icon
            contentDescription = "Blocked",
            tint = colors.red,
            modifier = Modifier.size(80.dp)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = stringResource(R.string.browser_access_blocked),
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
            color = colors.textPrimary,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            text = "شما اجازه دسترسی به این آدرس را ندارید:", // Put in strings.xml
            fontSize = 14.sp,
            color = colors.textSecondary
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.surface, RoundedCornerShape(12.dp))
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = blockedUrl,
                fontSize = 12.sp,
                color = colors.red,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = onGoBackClick,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("بازگشت به صفحه امن", color = Color.White, fontWeight = FontWeight.Bold) // strings.xml
        }
    }
}