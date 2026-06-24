package com.vahak.mehrban.uiv2.components.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.vahak.mehrban.R
import com.vahak.mehrban.core.data.local.entity.ChildEntity
import com.vahak.mehrban.core.data.local.entity.Gender
import com.vahak.mehrban.uiv2.theme.AppIcons
import com.vahak.mehrban.uiv2.theme.AppTheme
import com.vahak.mehrban.uiv2.theme.LocalCustomColors
import com.vahak.mehrban.uiv2.theme.ParentControlTheme
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LauncherConfirmSheet(
    activeChild: ChildEntity,
    onDismiss: () -> Unit,
    onChangeChildClick: () -> Unit,
    onActivateClick: () -> Unit
) {
    val colors = LocalCustomColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // 🚀 THE FIX: Capture the localized environment from MainActivity before the Sheet opens
    val layoutDirection = LocalLayoutDirection.current
    val context = LocalContext.current
    val configuration = LocalConfiguration.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.background,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        // 🚀 THE FIX: Inject the localized environment back into the new BottomSheet Window!
        CompositionLocalProvider(
            LocalLayoutDirection provides layoutDirection,
            LocalContext provides context,
            LocalConfiguration provides configuration
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(
                            Brush.linearGradient(listOf(colors.red, Color(0xFFD44245))),
                            RoundedCornerShape(24.dp)
                        )
                        .shadow(
                            16.dp,
                            RoundedCornerShape(24.dp),
                            spotColor = colors.red.copy(alpha = 0.4f)
                        ), contentAlignment = Alignment.Center
                ) {
                    Text("🛡️", fontSize = 40.sp)
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    stringResource(R.string.launcher_safe_activation_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = colors.textPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    stringResource(R.string.launcher_safe_activation_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.surface, RoundedCornerShape(16.dp))
                        .border(1.dp, colors.primary.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                        .padding(12.dp), verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(colors.cardInnerBG, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(if (activeChild.gender == Gender.BOY) "👦" else "👧", fontSize = 24.sp)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            stringResource(R.string.launcher_activation_for_child),
                            fontSize = 11.sp,
                            color = colors.textSecondary
                        )
                        Text(
                            activeChild.name,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            color = colors.primary
                        )
                    }
                    TextButton(onClick = onChangeChildClick) {
                        Text(
                            stringResource(R.string.change),
                            color = colors.primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.orangeLight, RoundedCornerShape(12.dp))
                        .border(1.dp, colors.orange.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        .padding(12.dp), verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("⚠️", fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        stringResource(R.string.launcher_exit_warning),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE65100),
                        lineHeight = 18.sp
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onActivateClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .shadow(
                            8.dp,
                            RoundedCornerShape(14.dp),
                            spotColor = colors.red.copy(alpha = 0.3f)
                        ),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Brush.linearGradient(listOf(colors.red, Color(0xFFD44245)))),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            stringResource(R.string.launcher_activate_button),
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp
                        )
                    }
                }

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    Text(
                        stringResource(R.string.cancel),
                        color = colors.textSecondary,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun PinRequiredDialog(onDismiss: () -> Unit, onSetupPassword: () -> Unit) {
    val colors = LocalCustomColors.current

    // 🚀 THE FIX: Capture the localized environment from MainActivity before the Dialog opens
    val layoutDirection = LocalLayoutDirection.current
    val context = LocalContext.current
    val configuration = LocalConfiguration.current

    Dialog(onDismissRequest = onDismiss) {
        // 🚀 THE FIX: Inject the localized environment back into the new Dialog Window!
        CompositionLocalProvider(
            LocalLayoutDirection provides layoutDirection,
            LocalContext provides context,
            LocalConfiguration provides configuration
        ) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                elevation = CardDefaults.cardElevation(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(70.dp)
                            .background(colors.primary.copy(alpha = 0.1f), CircleShape)
                            .border(2.dp, colors.primary.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            AppIcons.LockBadge,
                            contentDescription = null,
                            tint = colors.primary,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = stringResource(R.string.pin_required_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = colors.textPrimary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.pin_required_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textSecondary,
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    Button(
                        onClick = onSetupPassword,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(
                            stringResource(R.string.set_password_button),
                            color = colors.textOnPrimaryVariant,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            stringResource(R.string.cancel),
                            color = colors.textHint,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// PREVIEWS
// ==========================================

private val mockChildDialog = ChildEntity(
    id = "1",
    name = "علی",
    dob = LocalDate.of(2012, 5, 20),
    gender = Gender.BOY
)

@Preview(showBackground = true, locale = "fa", name = "1. Launcher Confirm Sheet (Light)")
@Composable
fun LauncherConfirmSheetPreview() {
    ParentControlTheme(themeMode = AppTheme.LIGHT) {
        LauncherConfirmSheet(
            activeChild = mockChildDialog,
            onDismiss = {},
            onChangeChildClick = {},
            onActivateClick = {}
        )
    }
}

@Preview(showBackground = true, locale = "fa", name = "2. PIN Required Dialog (Dark)")
@Composable
fun PinRequiredDialogPreview() {
    ParentControlTheme(themeMode = AppTheme.DARK) {
        PinRequiredDialog(
            onDismiss = {},
            onSetupPassword = {}
        )
    }
}