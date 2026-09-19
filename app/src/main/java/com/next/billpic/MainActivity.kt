package com.next.billpic

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BillPicTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = AppColor.bg,
                ) {
                    BillPicApp()
                }
            }
        }
    }
}
