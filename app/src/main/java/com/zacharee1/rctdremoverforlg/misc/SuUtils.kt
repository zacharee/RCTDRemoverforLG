package com.zacharee1.rctdremoverforlg.misc

import android.app.Activity
import android.text.TextUtils
import android.util.Log
import android.widget.ScrollView
import org.zeroturnaround.exec.ProcessExecutor
import org.zeroturnaround.exec.ProcessResult
import org.zeroturnaround.exec.stream.LogOutputStream
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.ArrayList

class SuUtils {
    companion object {
        fun sudo(vararg strings: String): Process {
            val su = Runtime.getRuntime().exec("su")

            try {
                val outputStream = DataOutputStream(su.outputStream)

                for (s in strings) {
                    Log.e("Sudo Exec", s)
                    outputStream.writeBytes(s + "\n")
                    outputStream.flush()
                }

                outputStream.writeBytes("exit\n")
                outputStream.flush()
                try {
                    su.waitFor()
                    Log.e("Done", "Done")
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                    Log.e("No Root?", e.message)
                }

                outputStream.close()
            } catch (e: IOException) {
                e.printStackTrace()
                throw IOException(e)
            } catch (e: Exception) {
                e.printStackTrace()
            }

            return su
        }

        fun sudoForResult(vararg strings: String): String {
            var res = ""
            var outputStream: DataOutputStream? = null
            var response: InputStream? = null
            try {
                val su = Runtime.getRuntime().exec("su")
                outputStream = DataOutputStream(su.outputStream)
                response = su.inputStream

                for (s in strings) {
                    outputStream.writeBytes(s + "\n")
                    outputStream.flush()
                }

                outputStream.writeBytes("exit\n")
                outputStream.flush()

                res = readFully(response!!)

                outputStream.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }

            return res
        }

        fun readFully(inputStream: InputStream): String {
            val baos = ByteArrayOutputStream()
            val buffer = ByteArray(1024)
            var length: Int

            do {
                length = inputStream.read(buffer)

                if (length < 5) break
                baos.write(buffer, 0, length)
                if (length == 6) break
            } while (true)

            return baos.toString("UTF-8")
        }

        fun suAndPrintToView(processExecutor: ProcessExecutor, activity: Activity?, addTo: TerminalView, commands: ArrayList<String>): ProcessResult? {
            val origCmds = ArrayList(commands)

            for (i in origCmds.indices) {
                val cmd = origCmds[i]
                if (!cmd.startsWith("echo")) {
                    val echo = "echo '<font color=\"#ffff00\">" + cmd + "</font>'".replace("|| exit 1", "")
                    commands.add(i, echo)
                }
            }

            try {
                return processExecutor.command("su", "-c", TextUtils.join(" ; ", commands))
                        .readOutput(true)
                        .redirectOutput(object : LogOutputStream() {
                            override fun processLine(line: String) {
                                activity?.runOnUiThread {
                                    addTo.addText(line + "\n")
                                    val scroll = addTo.parent.parent

                                    (scroll as? ScrollView)?.post { scroll.fullScroll(ScrollView.FOCUS_DOWN) }
                                }
                            }
                        })
                        .execute()
            } catch (e: Exception) {
                e.printStackTrace()
            }

            return null
        }

        fun testSudo(): Boolean {
            val commandResult: String

            try {
                commandResult = ProcessExecutor("su")
                        .readOutput(true)
                        .execute()
                        .outputString()
            } catch (e: Exception) {
                return false
            }

            return commandResult.isEmpty()
        }
    }
}