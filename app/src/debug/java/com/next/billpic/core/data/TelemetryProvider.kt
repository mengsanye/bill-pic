package com.next.billpic.core.data

import android.content.Context

/**
 * 走查包的采集实现注入点。
 *
 * 同一个类名在 `src/release` 里有另一份实现（返回空对象），
 * 由构建变体决定谁参与编译——这就是「上架包不含埋点」的保证方式。
 */
object TelemetryProvider {
    fun create(context: Context): Telemetry = DebugTelemetry(context)
}
