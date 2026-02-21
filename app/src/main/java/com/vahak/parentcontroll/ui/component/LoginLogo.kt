package com.vahak.parentcontroll.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

@Composable
fun LoginLogo() {
    Box(
        modifier = Modifier
            .size(80.dp)
            // CSS: box-shadow: 0 8px 20px rgba(0, 176, 155, 0.3);
            .shadow(
                elevation = 20.dp,
                spotColor = LoginDesign.PrimaryShadow,
                ambientColor = LoginDesign.PrimaryShadow,
                shape = RoundedCornerShape(20.dp)
            )
            .background(
                brush = LoginDesign.PrimaryGradient,
                shape = RoundedCornerShape(20.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.Smartphone,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(40.dp) // Approx 2.5rem
        )
    }
}

@Preview(name = "Logo Component")
@Composable
fun LoginLogoPreview() {
    androidx.compose.runtime.CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        LoginLogo()
    }
}