package com.vahak.mehrban.uiv2.screens.browser.pages

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.vahak.mehrban.R
import com.vahak.mehrban.uiv2.theme.AppIcons
import com.vahak.mehrban.uiv2.theme.LocalCustomColors

@Composable
fun <T> GenericListEditorPage(
    items: List<T>,
    itemTitle: (T) -> String,
    itemSubtitle: ((T) -> String)? = null,
    itemInput1: (T) -> String, // Extractor for pre-filling input 1
    itemInput2: ((T) -> String)? = null, // Extractor for pre-filling input 2
    onDelete: (T) -> Unit,
    onEdit: (oldItem: T, input1: String, input2: String?) -> Unit,
    dialogTitle: String,
    input1Hint: String,
    input2Hint: String? = null,
    onAdd: (input1: String, input2: String?) -> Unit
) {
    val colors = LocalCustomColors.current
    var showDialog by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<T?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        if (items.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(R.string.browser_settings_empty_list_hint),
                    color = colors.textHint
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)
            ) {
                items(items) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = colors.surface),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = itemTitle(item),
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textPrimary,
                                    maxLines = 1
                                )
                                if (itemSubtitle != null) {
                                    Text(
                                        text = itemSubtitle(item),
                                        fontSize = 12.sp,
                                        color = colors.textSecondary,
                                        maxLines = 1
                                    )
                                }
                            }
                            Row {
                                IconButton(onClick = { editingItem = item; showDialog = true }) {
                                    Icon(AppIcons.Edit, contentDescription = "Edit", tint = colors.textSecondary)
                                }
                                IconButton(onClick = { onDelete(item) }) {
                                    Icon(AppIcons.DeleteForever, contentDescription = "Delete", tint = colors.red)
                                }
                            }
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { editingItem = null; showDialog = true },
            containerColor = colors.primary,
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp).navigationBarsPadding()
        ) {
            Text("+", color = Color.White, fontSize = 24.sp)
        }
    }

    if (showDialog) {
        CustomAddDialog(
            title = dialogTitle,
            input1Hint = input1Hint,
            input2Hint = input2Hint,
            initialInput1 = editingItem?.let { itemInput1(it) } ?: "",
            initialInput2 = editingItem?.let { itemInput2?.invoke(it) } ?: "",
            onDismiss = { showDialog = false; editingItem = null },
            onSave = { val1, val2 ->
                if (editingItem != null) {
                    onEdit(editingItem!!, val1, val2)
                } else {
                    onAdd(val1, val2)
                }
                showDialog = false
                editingItem = null
            })
    }
}

@Composable
private fun CustomAddDialog(
    title: String,
    input1Hint: String,
    input2Hint: String?,
    initialInput1: String,
    initialInput2: String,
    onDismiss: () -> Unit,
    onSave: (String, String?) -> Unit
) {
    val colors = LocalCustomColors.current
    var input1 by remember { mutableStateOf(initialInput1) }
    var input2 by remember { mutableStateOf(initialInput2) }
    val cancelText = stringResource(R.string.cancel)
    val saveText = stringResource(R.string.save)

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
                Text(text = title, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = colors.textPrimary)
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = input1,
                    onValueChange = { input1 = it },
                    label = { Text(input1Hint, color = colors.textHint) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary,
                        focusedBorderColor = colors.primary,
                        unfocusedBorderColor = colors.divider,
                        cursorColor = colors.primary,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    )
                )

                if (input2Hint != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = input2,
                        onValueChange = { input2 = it },
                        label = { Text(input2Hint, color = colors.textHint) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = colors.textPrimary,
                            unfocusedTextColor = colors.textPrimary,
                            focusedBorderColor = colors.primary,
                            unfocusedBorderColor = colors.divider,
                            cursorColor = colors.primary,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        )
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onDismiss) { Text(cancelText, color = colors.textSecondary) }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { if (input1.isNotBlank() && (input2Hint == null || input2.isNotBlank())) onSave(input1, if (input2Hint != null) input2 else null) },
                        colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(saveText, color = Color.White)
                    }
                }
            }
        }
    }
}