package com.zacharee1.rctdremoverforlg.misc

import android.content.Context
import android.util.TypedValue
import com.topjohnwu.superuser.io.SuFile
import com.topjohnwu.superuser.io.SuFileInputStream
import java.io.*

class Utils {
    companion object {
        fun pxToDp(context: Context, dp: Int): Int {
            return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp.toFloat(), context.resources.displayMetrics).toInt()
        }

        fun getAikVersion(): Float {
            val file = SuFile("/data/local/AIK-mobile/bin/", "module.prop")

            val text = StringBuilder()

            SuFileInputStream.open(file).bufferedReader().useLines { line ->
                if (line.contains("version=")) {
                    text.append(line)
                }
            }

            val version = text.toString().replace("version=", "")

            return version.toFloat()
        }
    }
}