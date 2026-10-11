package com.next.billpic.qs

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.next.billpic.MainActivity
import com.next.billpic.R

/**
 * 下拉快捷菜单图块「发票转图片」。
 *
 * 这是一个**触发型**图块（不是开关）：点击后收起面板并打开 App 的转换页。
 * 全程本机行为——不申请任何权限、不接触网络，因此不给「无 INTERNET 权限」
 * 的隐私承诺开口子。
 */
class BillPicTileService : TileService() {

    /**
     * 面板展开时刷新图块外观。
     *
     * 刻意保持轻量：这里在系统主线程被调用，不做任何 I/O 或渲染，
     * 否则会拖慢整个通知栏的下拉动画。
     */
    override fun onStartListening() {
        super.onStartListening()
        qsTile?.apply {
            state = Tile.STATE_INACTIVE
            label = getString(R.string.qs_tile_label)
            updateTile()
        }
    }

    override fun onClick() {
        super.onClick()
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra(MainActivity.EXTRA_OPEN, MainActivity.OPEN_CONVERT)
        }

        // API 34 起 startActivityAndCollapse(Intent) 被废弃，新增 PendingIntent 重载。
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val pending = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            startActivityAndCollapse(pending)
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }
}
