package com.next.billpic.ui.sheets

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.next.billpic.R
import com.next.billpic.core.model.ConvertedPage
import com.next.billpic.core.util.Formatters
import com.next.billpic.core.util.Images
import com.next.billpic.ui.components.rememberTapHaptics
import com.next.billpic.ui.theme.AppColor
import com.next.billpic.ui.theme.AppText

/**
 * 全屏看图。
 *
 * 双指缩放 + 拖动查看细节，右上角直接保存到相册。
 * 发票字号小，放大核对是真实需求，所以这里没有用简单的 ImageView 静态展示。
 */
@Composable
fun ImageViewerOverlay(
    visible: Boolean,
    page: ConvertedPage?,
    onClose: () -> Unit,
    onSave: () -> Unit,
) {
    val palette = AppColor
    val haptics = rememberTapHaptics()

    var scale by remember { mutableFloatStateOf(1f) }
    var translation by remember { mutableStateOf(Offset.Zero) }

    LaunchedEffect(page) {
        scale = 1f
        translation = Offset.Zero
    }

    val transformState = rememberTransformableState { zoomChange, panChange, _ ->
        val next = (scale * zoomChange).coerceIn(1f, 8f)
        scale = next
        translation = if (next <= 1f) Offset.Zero else translation + panChange
    }

    val image = remember(page) {
        page?.let { Images.decodeSampled(it.bytes, maxDimension = 2048) }
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(durationMillis = 160)),
        exit = fadeOut(tween(durationMillis = 140)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(if (palette.isDark) Color.Black else Color(0xF2101010)),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.14f))
                        .clickable {
                            haptics()
                            onClose()
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_close),
                        contentDescription = "关闭",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    text = page?.let { "第 ${it.page} 页" } ?: "",
                    style = AppText.Body,
                    color = Color.White,
                    modifier = Modifier.weight(1f),
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White.copy(alpha = 0.18f))
                        .clickable {
                            haptics()
                            onSave()
                        }
                        .padding(horizontal = 16.dp, vertical = 9.dp),
                ) {
                    Text(
                        text = "保存",
                        style = AppText.Note,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .transformable(state = transformState),
                contentAlignment = Alignment.Center,
            ) {
                if (image != null) {
                    Image(
                        bitmap = image,
                        contentDescription = page?.let { "第 ${it.page} 页发票图片" },
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(10.dp)
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                                translationX = translation.x
                                translationY = translation.y
                            },
                    )
                } else {
                    Text(
                        text = "这张图解码失败",
                        style = AppText.Body,
                        color = Color.White.copy(alpha = 0.7f),
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(bottom = 26.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = "双指放大核对细节 · 点「保存」写入相册",
                    style = AppText.Caption,
                    color = Color.White.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                )
                if (page != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "${page.width} × ${page.height} px · " +
                            Formatters.bytes(page.bytes.size.toLong()),
                        style = AppText.Caption,
                        color = Color.White.copy(alpha = 0.45f),
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}
