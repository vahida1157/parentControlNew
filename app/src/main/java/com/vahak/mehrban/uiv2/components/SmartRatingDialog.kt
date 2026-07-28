package com.vahak.mehrban.uiv2.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.vahak.mehrban.uiv2.theme.LocalCustomColors

@Composable
fun SmartRatingDialog(
    step: Int,
    title: String,
    description: String,
    yesText: String,
    noText: String,
    rateText: String,
    feedbackText: String,
    dismissText: String,
    onSatisfied: () -> Unit,
    onDissatisfied: () -> Unit,
    onRateClicked: () -> Unit,
    onFeedbackClicked: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalCustomColors.current

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surface),
            elevation = CardDefaults.cardElevation(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Large Emoji Header
                Text(
                    text = when (step) {
                        1 -> "🤔"
                        2 -> "⭐"
                        else -> "🛠️"
                    },
                    fontSize = 48.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Title
                Text(
                    text = title,
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    color = colors.textPrimary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Description
                Text(
                    text = description,
                    fontSize = 13.sp,
                    color = colors.textSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Buttons
                when (step) {
                    1 -> {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            OutlinedButton(onClick = onDissatisfied, modifier = Modifier.weight(1f)) {
                                Text(noText, color = colors.red)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Button(
                                onClick = onSatisfied,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
                            ) {
                                Text(yesText, color = colors.textOnPrimaryVariant)
                            }
                        }
                    }
                    2 -> {
                        Button(
                            onClick = onRateClicked,
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
                        ) {
                            Text(rateText, color = colors.textOnPrimaryVariant, fontWeight = FontWeight.Bold)
                        }
                    }
                    3 -> {
                        Button(
                            onClick = onFeedbackClicked,
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
                        ) {
                            Text(feedbackText, color = colors.textOnPrimaryVariant, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Dismiss Button
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                ) {
                    Text(
                        text = dismissText,
                        color = colors.textHint
                    )
                }
            }
        }
    }
}