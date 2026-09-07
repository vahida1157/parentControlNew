package com.vahak.mehrban.uiv2.screens.timelimit

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.vahak.mehrban.R
import com.vahak.mehrban.presentation.timelimit.TimeLimitEffect
import com.vahak.mehrban.presentation.timelimit.TimeLimitEvent
import com.vahak.mehrban.presentation.timelimit.TimeLimitState
import com.vahak.mehrban.presentation.timelimit.TimeLimitViewModel
import com.vahak.mehrban.uiv2.components.DynamicTimePickerV2
import com.vahak.mehrban.uiv2.components.PickerPresentationMode
import com.vahak.mehrban.uiv2.components.header.HeaderAction
import com.vahak.mehrban.uiv2.components.header.MehrbanHeader
import com.vahak.mehrban.uiv2.theme.AppTheme
import com.vahak.mehrban.uiv2.theme.LocalCustomColors
import com.vahak.mehrban.uiv2.theme.ParentControlTheme

@Composable
fun TimeLimitScreen(
	viewModel: TimeLimitViewModel = hiltViewModel(), onBackClick: () -> Unit
) {
	val state by viewModel.state.collectAsState()
	val context = LocalContext.current
	val timeSettingsSavedMessage = stringResource(R.string.time_settings_saved)

	LaunchedEffect(Unit) {
		viewModel.effect.collect { effect ->
			when (effect) {
				is TimeLimitEffect.NavigateBack -> onBackClick()
				is TimeLimitEffect.ShowSavedToast -> Toast.makeText(
					context, timeSettingsSavedMessage, Toast.LENGTH_SHORT
				).show()
			}
		}
	}

	TimeLimitContent(state = state, onEvent = viewModel::onEvent)
}

@Composable
fun TimeLimitContent(
	state: TimeLimitState, onEvent: (TimeLimitEvent) -> Unit
) {
	val colors = LocalCustomColors.current

	Box(
		modifier = Modifier
			.fillMaxSize()
			.background(colors.background)
	) {
		Column(
			modifier = Modifier
				.fillMaxSize()
				.verticalScroll(rememberScrollState())
		) {
			MehrbanHeader(
				title = stringResource(R.string.timelimit_title),
				subtitle = stringResource(R.string.timelimit_subtitle),
				iconEmoji = "⏰",
				action = HeaderAction.Back { onEvent(TimeLimitEvent.BackClicked) },
			)

			// 🚀 WRAPPER FOR ALL CARDS
			Column(
				modifier = Modifier
					.fillMaxWidth()
					.padding(horizontal = 20.dp)
					.offset(y = (-30).dp),
				verticalArrangement = Arrangement.spacedBy(16.dp)
			) {

				// ==========================================
				// CARD 1: MAIN TIME LIMIT
				// ==========================================
				Card(
					modifier = Modifier.fillMaxWidth(),
					shape = RoundedCornerShape(24.dp),
					colors = CardDefaults.cardColors(containerColor = colors.surface),
					elevation = CardDefaults.cardElevation(8.dp)
				) {
					Column(
						modifier = Modifier
							.fillMaxWidth()
							.padding(24.dp)
					) {
						ToggleRowV2(
							title = stringResource(R.string.timelimit_activate_title),
							desc = stringResource(R.string.timelimit_activate_desc),
							iconEmoji = "⏳",
							iconBg = colors.orangeLight,
							isActive = state.isTimeLimitActive,
							onToggle = { onEvent(TimeLimitEvent.ToggleActive(it)) })

						if (state.isTimeLimitActive) {
							Spacer(modifier = Modifier.height(24.dp))
							Text(
								text = stringResource(R.string.timelimit_quick_select),
								fontSize = 12.sp,
								color = colors.textSecondary,
								fontWeight = FontWeight.Bold,
								modifier = Modifier.padding(bottom = 12.dp)
							)
							Row(
								modifier = Modifier.fillMaxWidth(),
								horizontalArrangement = Arrangement.spacedBy(8.dp)
							) {
								TimePresetButton(
									title = stringResource(R.string.timelimit_30min),
									isSelected = state.hours == 0 && state.minutes == 30,
									modifier = Modifier.weight(1f)
								) { onEvent(TimeLimitEvent.TimePresetSelected(0, 30)) }
								TimePresetButton(
									title = stringResource(R.string.timelimit_1hour),
									isSelected = state.hours == 1 && state.minutes == 0,
									modifier = Modifier.weight(1f)
								) { onEvent(TimeLimitEvent.TimePresetSelected(1, 0)) }
								TimePresetButton(
									title = stringResource(R.string.timelimit_2hours),
									isSelected = state.hours == 2 && state.minutes == 0,
									modifier = Modifier.weight(1f)
								) { onEvent(TimeLimitEvent.TimePresetSelected(2, 0)) }
							}

							Spacer(modifier = Modifier.height(8.dp))

							val isCustomMainTime =
								!(state.hours == 0 && state.minutes == 30) && !(state.hours == 1 && state.minutes == 0) && !(state.hours == 2 && state.minutes == 0) && !(state.hours == 3 && state.minutes == 0)

							val hourLabel = stringResource(R.string.hour)
							val minuteLabel = stringResource(R.string.minute)

							val customMainTimeFormatted = when {
								state.hours > 0 && state.minutes > 0 -> "${state.hours} $hourLabel و ${state.minutes} $minuteLabel"
								state.hours > 0 -> "${state.hours} $hourLabel"
								state.minutes > 0 -> "${state.minutes} $minuteLabel"
								else -> "۰ $minuteLabel"
							}

							Row(
								modifier = Modifier
									.fillMaxWidth()
									.clip(RoundedCornerShape(12.dp))
									.background(if (isCustomMainTime) colors.primary.copy(alpha = 0.08f) else colors.surface)
									.border(
										1.dp,
										if (isCustomMainTime) colors.primary else colors.divider,
										RoundedCornerShape(12.dp)
									)
									.clickable { onEvent(TimeLimitEvent.OpenPicker) }
									.padding(12.dp),
								horizontalArrangement = Arrangement.Center,
								verticalAlignment = Alignment.CenterVertically) {
								Text("⚙️", fontSize = 16.sp)
								Spacer(modifier = Modifier.width(8.dp))
								Text(
									text = if (isCustomMainTime) "${stringResource(R.string.timelimit_custom_prefix)}$customMainTimeFormatted"
									else stringResource(R.string.timelimit_custom_time),
									color = if (isCustomMainTime) colors.primary else colors.textPrimary,
									fontSize = 12.sp,
									fontWeight = FontWeight.Bold
								)
							}
						}
					}
				}

				// ==========================================
				// CARD 2: EXERCISE BONUS REWARD
				// ==========================================
				AnimatedVisibility(visible = state.isTimeLimitActive) {
					Card(
						modifier = Modifier.fillMaxWidth(),
						shape = RoundedCornerShape(24.dp),
						colors = CardDefaults.cardColors(containerColor = colors.surface),
						elevation = CardDefaults.cardElevation(8.dp)
					) {
						Column(
							modifier = Modifier
								.fillMaxWidth()
								.padding(24.dp)
						) {

							// 🚀 ADDED THE ❕ ICON RIGHT NEXT TO THE MAIN REWARD TOGGLE
							ToggleRowV2(
								title = stringResource(R.string.timelimit_exercise_reward_title),
								desc = stringResource(R.string.timelimit_exercise_reward_desc),
								iconEmoji = "🏃",
								iconBg = colors.greenLight,
								isActive = state.isExerciseRewardEnabled,
								onToggle = { onEvent(TimeLimitEvent.ToggleExerciseReward(it)) })

							if (state.isExerciseRewardEnabled) {

								// 🚀 2. NEW Permanent Info Box (Replacing the AnimatedVisibility hint)
								Card(
									modifier = Modifier
										.fillMaxWidth()
										.padding(top = 16.dp, bottom = 8.dp),
									colors = CardDefaults.cardColors(
										containerColor = colors.primary.copy(
											alpha = 0.08f
										)
									),
									border = androidx.compose.foundation.BorderStroke(
										1.dp, colors.primary.copy(alpha = 0.3f)
									),
									shape = RoundedCornerShape(12.dp)
								) {
									Row(
										modifier = Modifier.padding(12.dp),
										verticalAlignment = Alignment.CenterVertically
									) {
										Text(
											"💡", fontSize = 20.sp
										) // You can replace this with Icon(AppIcons.Info, ...) if you have one
										Spacer(modifier = Modifier.width(12.dp))
										Text(
											text = stringResource(R.string.timelimit_reward_value_hint),
											color = colors.primary,
											fontSize = 11.sp,
											lineHeight = 18.sp
										)
									}
								}

								Spacer(modifier = Modifier.height(16.dp))

								Text(
									text = stringResource(R.string.timelimit_max_reward_title),
									fontSize = 12.sp,
									color = colors.textSecondary,
									fontWeight = FontWeight.Bold,
									modifier = Modifier.padding(bottom = 12.dp)
								)

								Row(
									modifier = Modifier.fillMaxWidth(),
									horizontalArrangement = Arrangement.spacedBy(8.dp)
								) {
									TimePresetButton(
										title = stringResource(R.string.timelimit_20_min),
										isSelected = state.maxRewardMinutes == 20,
										modifier = Modifier.weight(1f)
									) { onEvent(TimeLimitEvent.MaxRewardSelected(0, 20)) }

									TimePresetButton(
										title = stringResource(R.string.timelimit_40_min),
										isSelected = state.maxRewardMinutes == 40,
										modifier = Modifier.weight(1f)
									) { onEvent(TimeLimitEvent.MaxRewardSelected(0, 40)) }

									TimePresetButton(
										title = stringResource(R.string.timelimit_60_min),
										isSelected = state.maxRewardMinutes == 60,
										modifier = Modifier.weight(1f)
									) { onEvent(TimeLimitEvent.MaxRewardSelected(1, 0)) }
								}

								Spacer(modifier = Modifier.height(8.dp))

								val isCustomReward = state.maxRewardMinutes !in listOf(20, 40, 60)
								val maxHours = state.maxRewardMinutes / 60
								val maxMins = state.maxRewardMinutes % 60
								val hourLabel = stringResource(R.string.hour)
								val minuteLabel = stringResource(R.string.minute)

								val customTimeFormatted = when {
									maxHours > 0 && maxMins > 0 -> "$maxHours $hourLabel و $maxMins $minuteLabel"
									maxHours > 0 -> "$maxHours $hourLabel"
									maxMins > 0 -> "$maxMins $minuteLabel"
									else -> "۰ $minuteLabel"
								}

								Row(
									modifier = Modifier
										.fillMaxWidth()
										.clip(RoundedCornerShape(12.dp))
										.background(if (isCustomReward) colors.primary.copy(alpha = 0.08f) else colors.surface)
										.border(
											1.dp,
											if (isCustomReward) colors.primary else colors.divider,
											RoundedCornerShape(12.dp)
										)
										.clickable { onEvent(TimeLimitEvent.OpenMaxRewardPicker) }
										.padding(12.dp),
									horizontalArrangement = Arrangement.Center,
									verticalAlignment = Alignment.CenterVertically) {
									Text("⚙️", fontSize = 16.sp)
									Spacer(modifier = Modifier.width(8.dp))
									Text(
										text = if (isCustomReward) "${stringResource(R.string.timelimit_custom_prefix)}$customTimeFormatted"
										else stringResource(R.string.timelimit_custom_reward_time),
										color = if (isCustomReward) colors.primary else colors.textPrimary,
										fontSize = 12.sp,
										fontWeight = FontWeight.Bold
									)
								}

								Spacer(modifier = Modifier.height(24.dp))

								// 🚀 SLIDER TITLE WITH HINT TOOLTIP
								Row(
									verticalAlignment = Alignment.CenterVertically,
									modifier = Modifier.padding(bottom = 8.dp)
								) {
									Text(
										text = stringResource(R.string.timelimit_reward_value_title),
										fontSize = 12.sp,
										color = colors.textSecondary,
										fontWeight = FontWeight.Bold
									)
								}

								val sliderColor = when {
									state.rewardSecondsPerPoint <= 3 -> colors.green
									state.rewardSecondsPerPoint <= 6 -> colors.yellow
									state.rewardSecondsPerPoint <= 8 -> colors.orange
									else -> colors.red
								}

								Column(
									modifier = Modifier
										.fillMaxWidth()
										.background(colors.cardInnerBG, RoundedCornerShape(12.dp))
										.border(1.dp, colors.divider, RoundedCornerShape(12.dp))
										.padding(16.dp)
								) {
									CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
										Column {
											Row(
												modifier = Modifier.fillMaxWidth(),
												horizontalArrangement = Arrangement.SpaceBetween
											) {
												Text(
													stringResource(R.string.timelimit_strict),
													fontSize = 10.sp,
													color = colors.textHint
												)
												Text(
													text = stringResource(
														R.string.timelimit_seconds,
														state.rewardSecondsPerPoint
													),
													fontWeight = FontWeight.Bold,
													color = sliderColor
												)
												Text(
													stringResource(R.string.timelimit_lenient),
													fontSize = 10.sp,
													color = colors.textHint
												)
											}

											Slider(
												value = state.rewardSecondsPerPoint.toFloat(),
												onValueChange = {
													onEvent(
														TimeLimitEvent.RewardSecondsChanged(
															it.toInt()
														)
													)
												},
												valueRange = 1f..10f, // 🚀 NEW RANGE (1 to 10)
												steps = 8,            // 🚀 8 steps creates exactly: 2, 3, 4, 5, 6, 7, 8, 9
												colors = androidx.compose.material3.SliderDefaults.colors(
													thumbColor = sliderColor,
													activeTrackColor = sliderColor,
													inactiveTrackColor = colors.divider
												)
											)
										}
									}

									val estimatedMinutes = (400 * state.rewardSecondsPerPoint) / 60

									Text(
										text = stringResource(
											R.string.timelimit_reward_helper_text, estimatedMinutes
										),
										fontSize = 11.sp,
										color = colors.textSecondary,
										modifier = Modifier.padding(top = 8.dp),
										lineHeight = 18.sp
									)
								}
							}
						}
					}
				}

				// ==========================================
				// SUMMARY SECTION & SAVE BUTTON
				// ==========================================
				Spacer(modifier = Modifier.height(8.dp))

				Row(
					modifier = Modifier
						.fillMaxWidth()
						.background(colors.primary.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
						.border(1.dp, colors.primary.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
						.padding(16.dp), verticalAlignment = Alignment.CenterVertically
				) {
					Text("⏰", fontSize = 24.sp)
					Spacer(modifier = Modifier.width(12.dp))
					Column {
						Text(
							stringResource(R.string.timelimit_daily_allowed),
							fontSize = 11.sp,
							color = colors.textSecondary,
							fontWeight = FontWeight.Bold
						)

						val hourLabel = stringResource(R.string.hour)
						val minuteLabel = stringResource(R.string.minute)
						val noLimit = stringResource(R.string.unlimited)

						// 🚀 UPDATED DYNAMIC PREVIEW LOGIC
						if (!state.isTimeLimitActive) {
							// 1. Time Limit is OFF -> Just show "Unlimited" (بدون محدودیت)
							Row(verticalAlignment = Alignment.Bottom) {
								Text(
									noLimit,
									fontSize = 16.sp,
									fontWeight = FontWeight.Black,
									color = colors.primary
								)
							}
						} else {
							// 2. Time Limit is ON -> Calculate Base + Bonus
							val baseTime = state.totalMinutes
							val bonusMinutes =
								if (state.isExerciseRewardEnabled) (state.earnedBonusSecondsToday / 60) else 0

							val totalAvailableMinutes = baseTime + bonusMinutes
							val finalHours = totalAvailableMinutes / 60
							val finalMinutes = totalAvailableMinutes % 60

							val previewText = when {
								finalHours > 0 && finalMinutes > 0 -> "${finalHours} $hourLabel و ${finalMinutes} $minuteLabel"
								finalHours > 0 -> "${finalHours} $hourLabel"
								finalMinutes > 0 -> "${finalMinutes} $minuteLabel"
								else -> "۰ $minuteLabel"
							}

							Row(verticalAlignment = Alignment.Bottom) {
								Text(
									previewText,
									fontSize = 16.sp,
									fontWeight = FontWeight.Black,
									color = colors.primary
								)
								if (bonusMinutes > 0) {
									Text(
										stringResource(
											R.string.timelimit_bonus_included, bonusMinutes
										),
										fontSize = 10.sp,
										fontWeight = FontWeight.Bold,
										color = Color(0xFF4CAF50),
										modifier = Modifier.padding(start = 6.dp, bottom = 2.dp)
									)
								}
							}
						}
					}
				}

				// TODO future: add these features in future when it's ready
//                        Spacer(modifier = Modifier.height(24.dp))
//
//                        ToggleRowV2(
//                            title = "هشدار پایان زمان",
//                            desc = "هشدار ۵ دقیقه قبل از پایان",
//                            iconEmoji = "🔔",
//                            iconBg = colors.orangeLight,
//                            isActive = state.isWarningEnabled,
//                            onToggle = { onEvent(TimeLimitEventV2.ToggleWarning(it)) }
//                        )
//                        Spacer(modifier = Modifier.height(12.dp))
//                        ToggleRowV2(
//                            title = "تفاوت روزهای آخر هفته",
//                            desc = "زمان مجاز جداگانه برای تعطیلات",
//                            iconEmoji = "📅",
//                            iconBg = colors.greenLight,
//                            isActive = state.isWeekendSeparate,
//                            onToggle = { onEvent(TimeLimitEventV2.ToggleWeekend(it)) }
//                        )

				Spacer(modifier = Modifier.height(16.dp))

				Button(
					onClick = { onEvent(TimeLimitEvent.SaveClicked) },
					enabled = !state.isSaving,
					modifier = Modifier
						.fillMaxWidth()
						.height(56.dp)
						.shadow(
							8.dp,
							RoundedCornerShape(14.dp),
							spotColor = colors.primary.copy(alpha = 0.4f)
						),
					shape = RoundedCornerShape(14.dp),
					colors = ButtonDefaults.buttonColors(
						containerColor = colors.primary,
						disabledContainerColor = colors.backgroundButtonDisable
					)
				) {
					if (state.isSaving) {
						CircularProgressIndicator(
							color = colors.textOnPrimaryVariant,
							modifier = Modifier.size(24.dp),
							strokeWidth = 2.dp
						)
					} else {
						Text(
							stringResource(R.string.button_save_settings),
							color = colors.textOnPrimaryVariant,
							style = MaterialTheme.typography.titleMedium,
							fontWeight = FontWeight.Bold
						)
					}
				}
			}
			Spacer(modifier = Modifier.height(30.dp))
		}

		if (state.isPickerVisible) {
			DynamicTimePickerV2(
				mode = PickerPresentationMode.BOTTOM_SHEET,
				title = stringResource(R.string.time_limit_title),
				initialHours = state.hours,
				initialMinutes = state.minutes,
				hoursRange = 0..23,
				minutesRange = 0..59,
				onDismiss = { onEvent(TimeLimitEvent.ClosePicker) },
				onConfirm = { h, m -> onEvent(TimeLimitEvent.ConfirmTime(h, m)) })
		}
		if (state.isMaxRewardPickerVisible) {
			DynamicTimePickerV2(
				mode = PickerPresentationMode.BOTTOM_SHEET,
				title = stringResource(R.string.timelimit_max_reward_title),
				initialHours = state.maxRewardMinutes / 60,
				initialMinutes = state.maxRewardMinutes % 60,
				hoursRange = 0..2,
				minutesRange = 0..59,
				onDismiss = { onEvent(TimeLimitEvent.CloseMaxRewardPicker) },
				onConfirm = { h, m ->
					onEvent(TimeLimitEvent.MaxRewardSelected(h, m))
					onEvent(TimeLimitEvent.CloseMaxRewardPicker)
				})
		}
	}
}

// --- SUB COMPONENTS ---

@Composable
fun TimePresetButton(
	title: String, isSelected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit
) {
	val colors = LocalCustomColors.current
	Box(
		modifier = modifier
			.clip(RoundedCornerShape(12.dp))
			.background(if (isSelected) colors.primary.copy(alpha = 0.08f) else colors.surface)
			.border(
				2.dp, if (isSelected) colors.primary else colors.divider, RoundedCornerShape(12.dp)
			)
			.clickable { onClick() }
			.padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
		Text(
			title,
			color = if (isSelected) colors.primary else colors.textPrimary,
			fontSize = 12.sp,
			fontWeight = FontWeight.Bold
		)
	}
}

@Composable
fun ToggleRowV2(
	title: String,
	desc: String,
	iconEmoji: String,
	iconBg: Color,
	isActive: Boolean,
	onToggle: (Boolean) -> Unit
) {
	val colors = LocalCustomColors.current
	Row(
		modifier = Modifier
			.fillMaxWidth()
			.background(colors.surface, RoundedCornerShape(12.dp))
			.border(1.dp, colors.divider, RoundedCornerShape(12.dp))
			.padding(16.dp),
		verticalAlignment = Alignment.CenterVertically
	) {
		Box(
			modifier = Modifier
				.size(44.dp)
				.background(iconBg, RoundedCornerShape(12.dp)),
			contentAlignment = Alignment.Center
		) {
			Text(iconEmoji, fontSize = 20.sp)
		}
		Spacer(modifier = Modifier.width(12.dp))
		Column(modifier = Modifier.weight(1f)) {
			Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = colors.textPrimary)
			Spacer(modifier = Modifier.height(2.dp))
			Text(desc, fontSize = 11.sp, color = colors.textSecondary)
		}
		Switch(
			checked = isActive, onCheckedChange = onToggle, colors = SwitchDefaults.colors(
				checkedThumbColor = Color.White,
				checkedTrackColor = colors.primary,
				uncheckedThumbColor = colors.textSecondary,
				uncheckedTrackColor = colors.divider
			)
		)
	}
}

// ==========================================
// PREVIEWS
// ==========================================

@Preview(showBackground = true, name = "1. Time Limit Light", locale = "fa")
@Composable
fun TimeLimitPreviewLight() {
	ParentControlTheme(themeMode = AppTheme.LIGHT) {
		TimeLimitContent(state = TimeLimitState(), onEvent = {})
	}
}

@Preview(showBackground = true, name = "2. Time Limit Dark", locale = "fa")
@Composable
fun TimeLimitPreviewDark() {
	ParentControlTheme(themeMode = AppTheme.DARK) {
		TimeLimitContent(state = TimeLimitState(isTimeLimitActive = false), onEvent = {})
	}
}