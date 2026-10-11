package com.next.billpic

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import com.next.billpic.ui.BillPicApp
import com.next.billpic.ui.theme.AppColor
import com.next.billpic.ui.theme.BillPicTheme

/**
 * BillPic · 把 PDF 发票一键转成图片。
 *
 * 转换全程在本机用系统 PDF 引擎完成，应用未申请联网权限。
 */
class MainActivity : ComponentActivity() {

    /**
     * 由下拉快捷图块触发的「打开转换页」信号。
     *
     * 图块点击时 Activity 可能已经存在（热启动走 [onNewIntent]），
     * 所以用一个 Compose 状态把事件转交下去，由 ViewModel 消费后复位。
     */
    private val openConvertSignal = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        consumeLaunchIntent(intent)
        setContent {
            BillPicTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = AppColor.bg,
                ) {
                    BillPicApp(
                        openConvertFromTile = openConvertSignal.value,
                        onTileOpenHandled = { openConvertSignal.value = false },
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        consumeLaunchIntent(intent)
    }

    /** 识别来自下拉图块的启动意图（冷启动 [onCreate] 与热启动 [onNewIntent] 共用）。 */
    private fun consumeLaunchIntent(intent: Intent?) {
        if (intent?.getStringExtra(EXTRA_OPEN) == OPEN_CONVERT) {
            // 消费后清掉 extra：Activity 因配置变更重建时不应重复触发。
            intent.removeExtra(EXTRA_OPEN)
            openConvertSignal.value = true
        }
    }

    companion object {
        /** 启动意图的键。 */
        const val EXTRA_OPEN = "com.next.billpic.extra.OPEN"

        /** [EXTRA_OPEN] 的取值：打开转换页。 */
        const val OPEN_CONVERT = "convert"
    }
}
