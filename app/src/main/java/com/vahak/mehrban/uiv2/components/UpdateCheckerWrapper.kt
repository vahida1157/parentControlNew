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
import com.vahak.mehrban.MainEvent
import com.vahak.mehrban.MainViewModel
import com.vahak.mehrban.R
import com.vahak.mehrban.UpdateState
import com.vahak.mehrban.data.remote.DownloadError
import com.vahak.mehrban.uiv2.theme.LocalCustomColors

@Composable
fun UpdateCheckerWrapper(
    viewModel: MainViewModel, content: @Composable () -> Unit
) {
    // 🚀 THE FIX: Collect the single unified MVI state
    val state by viewModel.state.collectAsState()
    val updateState = state.updateState
    val isUpdateIgnored = state.isUpdateIgnored
    val downloadState = state.downloadState

    val colors = LocalCustomColors.current
    val context = LocalContext.current
    val activity = context as? Activity

    Box(modifier = Modifier.fillMaxSize()) {
        content()
    }

    if (updateState is UpdateState.UpdateAvailable && !isUpdateIgnored) {
        val info = updateState.info

        val updateNewVersionAvailableText = stringResource(R.string.update_new_version_available)
        val updateDownloadFromStoreText = stringResource(R.string.update_download_from_store)
        val updateDownloadInstallText = stringResource(R.string.update_download_install)
        val updateConnectingText = stringResource(R.string.update_connecting)
        val errorDownloadConnectionFailedText =
            stringResource(R.string.error_download_connection_failed)
        val errorDownloadConnectionLostText =
            stringResource(R.string.error_download_connection_lost)
        val updateCancelledAlreadyLatestText =
            stringResource(R.string.update_cancelled_already_latest)
        val downloadErrorGenericText = stringResource(R.string.download_error_generic)
        val updateRetryText = stringResource(R.string.update_retry)
        val updateSuccessInstallingText = stringResource(R.string.update_success_installing)
        val updateExitAppText = stringResource(R.string.update_exit_app)
        val updateRemindLaterText = stringResource(R.string.update_remind_later)
        val updateDownloadingText = stringResource(R.string.update_downloading)

        Dialog(
            onDismissRequest = {
                if (!updateState.isForced && downloadState !is AppDownloadState.Connecting && downloadState !is AppDownloadState.Downloading) {
                    viewModel.onEvent(MainEvent.DismissOptionalUpdate) // 🚀 Trigger event instead of direct function
                }
            }, properties = DialogProperties(
                dismissOnBackPress = !updateState.isForced,
                dismissOnClickOutside = !updateState.isForced
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
                        text = updateNewVersionAvailableText,
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
                                    // 🚀 Send MVI Intent
                                    viewModel.onEvent(
                                        MainEvent.StartDownload(
                                            info.downloadUrl, info.latestVersionName
                                        )
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
                                    updateDownloadFromStoreText
                                } else {
                                    updateDownloadInstallText
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
                                    updateConnectingText,
                                    fontSize = 12.sp,
                                    color = colors.textSecondary
                                )
                            }
                        }

                        is AppDownloadState.Downloading -> {
                            val progress = downloadState.progress
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
                                    text = updateDownloadingText.format(progress),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textPrimary
                                )
                            }
                        }

                        is AppDownloadState.Error -> {
                            val errorEnum = downloadState.error
                            val errMsg = when (errorEnum) {
                                DownloadError.CONNECTION_FAILED -> errorDownloadConnectionFailedText
                                DownloadError.CONNECTION_LOST -> errorDownloadConnectionLostText
                                DownloadError.ALREADY_LATEST -> updateCancelledAlreadyLatestText
                                DownloadError.GENERIC_ERROR -> downloadErrorGenericText
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
                                        // 🚀 Send MVI Intent
                                        viewModel.onEvent(
                                            MainEvent.StartDownload(
                                                info.downloadUrl, info.latestVersionName
                                            )
                                        )
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = colors.red),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        updateRetryText,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        is AppDownloadState.Success -> {
                            Text(
                                updateSuccessInstallingText,
                                color = colors.green,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }

                    if (downloadState is AppDownloadState.Idle || downloadState is AppDownloadState.Error) {
                        if (updateState.isForced) {
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
                                    updateExitAppText, fontWeight = FontWeight.Bold
                                )
                            }
                        } else {
                            Spacer(modifier = Modifier.height(4.dp))
                            TextButton(
                                onClick = {
                                    // 🚀 Send MVI Intent
                                    viewModel.onEvent(MainEvent.DismissOptionalUpdate)
                                }, modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    updateRemindLaterText,
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