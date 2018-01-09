package com.zacharee1.rctdremoverforlg.misc

import android.content.Context
import android.util.TypedValue
import java.io.*

class Utils {
    companion object {
        fun pxToDp(context: Context, dp: Int): Int {
            return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp.toFloat(), context.resources.displayMetrics).toInt()
        }

        fun getAikVersion(): Float {
            val file = File("/data/local/AIK-mobile/bin/", "module.prop")

            val text = StringBuilder()

            try {
                val br = BufferedReader(FileReader(file))
                var line: String?

                do {
                    line = br.readLine()

                    if (line == null) break

                    if (line.contains("version=")) {
                        text.append(line)
                    }
                } while (true)

                br.close()
            } catch (e: IOException) {}

            val version = text.toString().replace("version=", "")

            return java.lang.Float.valueOf(version)!!
        }
    }
}