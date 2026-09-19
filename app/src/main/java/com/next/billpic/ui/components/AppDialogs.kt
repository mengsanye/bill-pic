package com.next.billpic.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.next.billpic.ui.theme.AppColor
import com.next.billpic.ui.theme.AppText

/**
 * 二次确认对话框。
 *
 * `destructive = true` 时确认按钮用红色 —— 删除记录、清除数据这类不可逆操作
 * 必须让用户在点之前就看出「这个按钮会删东西」。
 */
@Composable
fun AppConfirmDialog(
    title: String,
    message: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    dismissText: String = "取消",
    destructive: Boolean = false,
) {
    val palette = AppColor
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = palette.card,
        title = {
            Text(text = title, style = AppText.Title, color = palette.label)
        },
        text = {
            Text(text = message, style = AppText.Sub, color = palette.label2)
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = confirmText,
                    style = AppText.BodyStrong,
                    color = if (destructive) palette.red else palette.blue,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = dismissText, style = AppText.Body, color = palette.label2)
            }
        },
    )
}
