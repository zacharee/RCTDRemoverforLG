package com.zacharee1.rctdremoverforlg

import android.Manifest
import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Color
import android.os.*
import android.text.Html
import android.text.method.LinkMovementMethod
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.topjohnwu.superuser.Shell
import com.topjohnwu.superuser.io.SuFile
import com.topjohnwu.superuser.io.SuFileOutputStream
import com.zacharee1.rctdremoverforlg.misc.ProgressFragment
import com.zacharee1.rctdremoverforlg.misc.SwitchViewWithText
import com.zacharee1.rctdremoverforlg.misc.Utils
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import java.io.*
import java.util.*

class InstallerActivity : AppCompatActivity(), SwipeRefreshLayout.OnRefreshListener {
    companion object {
        const val AIK_VERSION = 3.1F

        const val NO_AIK = -1
        const val OLD_AIK = 1
        const val AIK = 0
    }

    private val rctd by lazy { findViewById<SwitchViewWithText>(R.id.enable_patch_rctd) }
    private val triton by lazy { findViewById<SwitchViewWithText>(R.id.enable_patch_triton) }
    private val ccmd by lazy { findViewById<SwitchViewWithText>(R.id.enable_patch_ccmd) }
    private val refreshLayout by lazy { findViewById<SwipeRefreshLayout>(R.id.swipe_parent) }

    private val prefs: SharedPreferences by lazy { PreferenceManager.getDefaultSharedPreferences(this) }

    @SuppressLint("CheckResult")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_checkroot)

        Observable.fromCallable { Shell.rootAccess() }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { bool ->
                if (bool) {
                    setup()
                } else {
                    finish()
                    Toast.makeText(this, resources.getText(R.string.need_root), Toast.LENGTH_SHORT)
                        .show()
                }
            }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        for (i in 0..permissions.lastIndex) {
            val permission = permissions[i]
            val result = grantResults[i]

            if (permission == Manifest.permission.WRITE_EXTERNAL_STORAGE && result == PackageManager.PERMISSION_GRANTED) {
                checkAikStatus()
            } else {
                finish()
            }
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onRefresh() {
        checkStatus()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR
    }

    @SuppressLint("CheckResult")
    private fun checkStatus() {
        val rctd = findViewById<TextView>(R.id.status_report)
        val triton = findViewById<TextView>(R.id.triton_status_report)
        val ccmd = findViewById<TextView>(R.id.ccmd_status_report)

        rctd.setTextColor(Color.YELLOW)
        triton.setTextColor(Color.YELLOW)
        ccmd.setTextColor(Color.YELLOW)

        rctd.setText(R.string.checking)
        triton.setText(R.string.checking)
        ccmd.setText(R.string.checking)

        if (!(refreshLayout?.isRefreshing as Boolean)) refreshLayout?.isRefreshing = true

        Observable.fromCallable({ checkStatusAsync() })
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { bools ->
                rctd.setText(if (bools[0]) R.string.not_found else R.string.running)
                rctd.setTextColor(if (bools[0]) Color.GREEN else Color.RED)

                triton.setText(if (bools[1]) R.string.not_found else R.string.running)
                triton.setTextColor(if (bools[1]) Color.GREEN else Color.RED)

                ccmd.setText(if (bools[2]) R.string.not_found else R.string.running)
                ccmd.setTextColor(if (bools[2]) Color.GREEN else Color.RED)

                Handler(Looper.getMainLooper()).postDelayed({
                    refreshLayout?.isRefreshing = false
                }, 300)
            }
    }

    private fun checkStatusAsync(): BooleanArray {
        val rctResult = arrayListOf<String>()
        val tritonResult = arrayListOf<String>()
        val ccmdResult = arrayListOf<String>()

        Shell.su("ps | grep rctd").to(rctResult).exec()
        Shell.su("ps | grep triton").to(tritonResult).exec()
        Shell.su("ps | grep ccmd").to(ccmdResult).exec()

        val rctNotThere = rctResult.isEmpty() || !rctResult.contains("/sbin/rctd")
        val tritonNotThere = tritonResult.isEmpty() || !tritonResult.contains("/system/bin/triton")
        val ccmdNotThere = ccmdResult.isEmpty() || !ccmdResult.contains("/system/bin/ccmd")

        return booleanArrayOf(rctNotThere, tritonNotThere, ccmdNotThere)
    }

    private fun setup() {
        setContentView(R.layout.activity_installer)

        if (checkCallingOrSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 10)
        } else {
            checkAikStatus()
        }
    }

    private fun setupSwipeRefresh() {
        refreshLayout.setColorSchemeResources(
            R.color.color_1,
            R.color.color_2,
            R.color.color_3,
            R.color.color_4,
            R.color.color_5,
            R.color.color_6,
            R.color.color_7,
            R.color.color_8,
            R.color.color_9,
            R.color.color_a,
            R.color.color_b,
            R.color.color_c
        )
        refreshLayout.setOnRefreshListener(this)
    }

    private fun setSwitchChecksAndListeners() {
        val patchRctd = prefs.getBoolean("rctd", true)
        val patchCcmd = prefs.getBoolean("ccmd", false)
        val patchTriton = prefs.getBoolean("triton", true)

        rctd.isChecked = patchRctd
        ccmd.isChecked = patchCcmd
        triton.isChecked = patchTriton

        rctd.setOnCheckedChangeListener { _, b ->
            prefs.edit {
                putBoolean("rctd", b)
            }
        }

        ccmd?.setOnCheckedChangeListener { _, b ->
            prefs.edit {
                putBoolean("ccmd", b)
            }
        }

        triton?.setOnCheckedChangeListener { _, b ->
            prefs.edit {
                putBoolean("triton", b)
            }
        }
    }

    private fun setToMainScreen() {
        runOnUiThread {
            setSwitchChecksAndListeners()
            setupSwipeRefresh()

            checkStatus()

            val appBy = findViewById<TextView>(R.id.app_made_by)
            val aikBy = findViewById<TextView>(R.id.aik_by)

            appBy.movementMethod = LinkMovementMethod.getInstance()
            aikBy.movementMethod = LinkMovementMethod.getInstance()
        }
    }

    @SuppressLint("CheckResult")
    private fun checkAikStatus() {
        Observable.fromCallable { checkAikStatusAsync() }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { status ->
                when (status) {
                    NO_AIK -> {
                        (findViewById<View>(R.id.textView) as TextView).text =
                            resources.getString(R.string.installing_aik)
                        installAik()
                    }
                    AIK -> {
                        setToMainScreen()
                    }
                    OLD_AIK -> {
                        MaterialAlertDialogBuilder(this)
                            .setTitle(R.string.old_aik)
                            .setMessage(R.string.old_aik_msg)
                            .setPositiveButton(R.string.yes) { _, _ -> installAik() }
                            .setNegativeButton(R.string.no) { _, _ -> setToMainScreen() }
                            .setCancelable(false)
                            .show()
                    }
                }
            }
    }

    private fun checkAikStatusAsync(): Int {
        try {
            val aikProc = Runtime.getRuntime().exec("aik")
            val outputStream = DataOutputStream(aikProc.outputStream)

            outputStream.writeBytes("exit\n")
            outputStream.flush()

            aikProc.waitFor()
        } catch (e: Exception) {
            return NO_AIK
        }


        return if (Utils.getAikVersion() >= AIK_VERSION) AIK else OLD_AIK
    }

    @SuppressLint("CheckResult")
    private fun installAik() {
        Observable.fromCallable { installAikAsync() }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { bool ->
                if (bool) {
                    MaterialAlertDialogBuilder(this)
                        .setTitle(resources.getString(R.string.reboot))
                        .setMessage(resources.getString(R.string.install_aik))
                        .setPositiveButton(resources.getString(R.string.yes)) { _, _ ->
                            try {
                                Shell.su("reboot recovery").exec()
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                        .setNegativeButton(resources.getString(R.string.no)) { _, _ -> finish() }
                        .setCancelable(false)
                        .show()
                } else {
                    MaterialAlertDialogBuilder(this)
                        .setTitle(R.string.failed)
                        .setMessage(R.string.aik_install_fail)
                        .setPositiveButton(R.string.ok) { _, _ -> finish() }
                        .show()
                }
            }
    }

    private fun installAikAsync(): Boolean {
        val aik = "AIK.zip"

        val outDir = SuFile(Environment.getExternalStorageDirectory().absolutePath, "AndroidImageKitchen")
        createDir(outDir)
        val outFile = SuFile(outDir, aik)

        assets.open(aik).use { input ->
            SuFileOutputStream.open(outFile, false).use { output ->
                input.copyTo(output)
            }
        }

        val result = Shell.su("echo '--update_package=/sdcard/0/AndroidImageKitchen/AIK.zip' >> /cache/recovery/command").exec()

        return result.isSuccess
    }

    fun handlePatch(v: View) {
        val patchRctd = rctd?.isChecked
        val patchCcmd = ccmd?.isChecked
        val patchTriton = triton?.isChecked

        val executeMod = "executemod.sh"

        val outDir = SuFile(Environment.getExternalStorageDirectory().absolutePath, "AndroidImageKitchen")
        val outFile = SuFile(outDir, executeMod)

        assets.open(executeMod).use { input ->
            SuFileOutputStream.open(outFile, false).use { output ->
                input.copyTo(output)
            }
        }

        showExecuteDialog(
            R.string.flash,
            R.string.cancel,
            R.string.patching_image,
            { handleFlash(v) },
            { handlePatch(v) },
            "cp /sdcard/AndroidImageKitchen/$executeMod /data/local/AIK-mobile/. || exit 1",
            "cd /data/local/AIK-mobile/ || exit 1",
            "chmod 0755 $executeMod || exit 1",
            "./" + executeMod + " " + patchRctd + " " + patchCcmd + " " + patchTriton + " " + Build.DEVICE
        )
    }

    fun handleFlash(v: View) {
        showExecuteDialog(
            R.string.reboot,
            R.string.cancel,
            R.string.flashing_image,
            {
                try {
                    Shell.su("reboot").exec()
                } catch (e: Exception) {}
            },
            { handleFlash(v) },
            "dd if=/storage/emulated/0/AndroidImageKitchen/boot.img of=/dev/block/bootdevice/by-name/boot || exit 1",
            "echo Done!"
        )
    }

    fun handleBackup(v: View) {
        showExecuteDialog(
            R.string.done,
            R.string.cancel,
            R.string.backing_up,
            null,
            { handleBackup(v) },
            "mkdir /sdcard/AndroidImageKitchen/Backups/",
            "dd if=/dev/block/bootdevice/by-name/boot of=/sdcard/AndroidImageKitchen/Backups/" +
                    Date().toString().replace(" ", "_") +
                    ".img || exit 1",
            "echo Done!"
        )
    }

    private fun showExecuteDialog(
        positiveRes: Int,
        negativeRes: Int,
        titleRes: Int,
        positiveHandler: Runnable?,
        failHandler: Runnable?,
        vararg commands: String
    ) {
        val fragment = ProgressFragment()
        fragment.setPositiveRunnable(positiveHandler)
        fragment.setFailRunnable(failHandler)

        val args = Bundle()
        args.putInt("title", titleRes)
        args.putInt("positive", positiveRes)
        args.putInt("negative", negativeRes)

        fragment.arguments = args
        fragment.show(supportFragmentManager, "progress", *commands)
    }

    @Throws(IOException::class)
    private fun createDir(dir: File) {
        if (dir.exists()) {
            if (!dir.isDirectory) {
                throw IOException("Can't create directory, a file is in the way")
            }
        } else {
            dir.mkdirs()
            if (!dir.isDirectory) {
                throw IOException("Unable to create directory")
            }
        }
    }
}