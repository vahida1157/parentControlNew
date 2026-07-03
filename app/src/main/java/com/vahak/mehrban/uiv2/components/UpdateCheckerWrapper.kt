package com.vahak.mehrban.uiv2.components

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.vahak.mehrban.AppDownloadState
import com.vahak.mehrban.BuildConfig
import com.vahak.mehrban.MainViewModel
import com.vahak.mehrban.R
import com.vahak.mehrban.UpdateState
import com.vahak.mehrban.data.remote.DownloadError
import com.vahak.mehrban.uiv2.theme.LocalCustomColors

@Composable
fun UpdateCheckerWrapper(
    viewModel: MainViewModel, content: @Composable () -> Unit
) {
    val updateState by viewModel.updateState.collectAsState()
    val isUpdateIgnored by viewModel.isUpdateIgnored.collectAsState()
    val downloadState by viewModel.appDownloadState.collectAsState()

    val colors = LocalCustomColors.current
    val context = LocalContext.current
    val activity = context as? Activity

    Box(modifier = Modifier.fillMaxSize()) {
        content()
    }

    if (updateState is UpdateState.UpdateAvailable && !isUpdateIgnored) {
        val updateData = (updateState as UpdateState.UpdateAvailable)
        val info = updateData.info

        Dialog(
            onDismissRequest = {
                if (!updateData.isForced && downloadState !is AppDownloadState.Connecting && downloadState !is AppDownloadState.Downloading) {
                    viewModel.dismissOptionalUpdate()
                }
            }, properties = DialogProperties(
                dismissOnBackPress = !updateData.isForced,
                dismissOnClickOutside = !updateData.isForced
            )
        ) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                elevation = CardDefaults.cardElevation(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.update_new_version_available),
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        color = colors.textPrimary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(colors.cardInnerBG, RoundedCornerShape(12.dp))
                            .padding(14.dp)
                    ) {
                        Text(
                            text = info.releaseNotes,
                            fontSize = 12.sp,
                            color = colors.textSecondary,
                            lineHeight = 20.sp,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Start
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    when (downloadState) {
                        is AppDownloadState.Idle -> {
                            Button(
                                onClick = {
                                    viewModel.startDownload(
                                        info.downloadUrl, info.latestVersionName
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                @Suppress(
                                    "SimplifyBooleanWithConstants", "KotlinConstantConditions"
                                ) val buttonText = if (BuildConfig.FLAVOR != "website") {
                                    stringResource(R.string.update_download_from_store)
                                } else {
                                    stringResource(R.string.update_download_install)
                                }
                                Text(
                                    buttonText,
                                    color = colors.textOnPrimaryVariant,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        is AppDownloadState.Connecting -> {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(
                                    color = colors.primary, modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    stringResource(R.string.update_connecting),
                                    fontSize = 12.sp,
                                    color = colors.textSecondary
                                )
                            }
                        }

                        is AppDownloadState.Downloading -> {
                            val progress = (downloadState as AppDownloadState.Downloading).progress
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                LinearProgressIndicator(
                                    progress = { progress / 100f },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp),
                                    color = colors.primary,
                                    trackColor = colors.divider
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    stringResource(R.string.update_downloading, progress),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textPrimary
                                )
                            }
                        }

                        is AppDownloadState.Error -> {
                            // 🚀 THE FIX: Translate the Enum to a Compose localized string
                            val errorEnum = (downloadState as AppDownloadState.Error).error
                            val errMsg = when (errorEnum) {
                                DownloadError.CONNECTION_FAILED -> stringResource(R.string.error_download_connection_failed)
                                DownloadError.CONNECTION_LOST -> stringResource(R.string.error_download_connection_lost)
                                DownloadError.ALREADY_LATEST -> stringResource(R.string.update_cancelled_already_latest)
                                DownloadError.GENERIC_ERROR -> stringResource(R.string.download_error_generic)
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = errMsg,
                                    color = colors.red,
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Button(
                                    onClick = {
                                        viewModel.startDownload(
                                            info.downloadUrl, info.latestVersionName
                                        )
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = colors.red),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        stringResource(R.string.update_retry),
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        is AppDownloadState.Success -> {
                            Text(
                                stringResource(R.string.update_success_installing),
                                color = colors.green,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }

                    if (downloadState is AppDownloadState.Idle || downloadState is AppDownloadState.Error) {
                        if (updateData.isForced) {
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = { activity?.finish() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.red),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    stringResource(R.string.update_exit_app),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else {
                            Spacer(modifier = Modifier.height(4.dp))
                            TextButton(
                                onClick = { viewModel.dismissOptionalUpdate() },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    stringResource(R.string.update_remind_later),
                                    color = colors.textHint,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}